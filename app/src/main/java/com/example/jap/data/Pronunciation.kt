package com.example.jap.data

import java.text.Normalizer

/** What went wrong in one word, e.g. 「いらっしゃい」 with "An extra sound「っ」was added." */
data class SoundIssue(val word: String, val notes: List<String>)

/**
 * How closely a spoken attempt matched a line. [wordOk] has one flag per display word of the line,
 * so the line can be coloured word by word.
 */
data class Assessment(val score: Int, val heard: String, val wordOk: List<Boolean>, val issues: List<SoundIssue>)

/**
 * Scores speech-recogniser output against a subtitle line by aligning sounds (morae).
 * The recogniser returns kanji, so words the line contains are first turned back into their
 * readings; whatever is left is compared sound by sound.
 */
object Pronunciation {

    private const val SMALL = "ゃゅょぁぃぅぇぉゎ"

    fun assess(line: VideoLine, candidates: List<String>): Assessment =
        candidates.filter { it.isNotBlank() }.map { assessOne(line, it) }.maxByOrNull { it.score }
            ?: Assessment(0, "", line.words.map { false }, emptyList())

    private fun assessOne(line: VideoLine, heard: String): Assessment {
        // Expected sounds, remembering which word each belongs to.
        val expected = mutableListOf<String>()
        val owner = mutableListOf<Int>()
        line.words.forEachIndexed { w, word ->
            morae(kanaOf(word)).forEach { expected += it; owner += w }
        }
        val said = morae(toKana(heard, line))
        if (expected.isEmpty()) return Assessment(100, heard, line.words.map { true }, emptyList())

        val ops = align(expected, said)
        val matches = ops.count { it.kind == Op.MATCH }
        val score = (matches * 100f / maxOf(expected.size, said.size)).toInt().coerceIn(0, 100)

        // Gather what went wrong per word, joining runs of the same kind of mistake.
        val notes = sortedMapOf<Int, MutableList<Pair<Op, Pair<String, String>>>>()
        for (op in ops) {
            if (op.kind == Op.MATCH) continue
            val w = owner[op.at.coerceIn(0, owner.lastIndex)]
            val list = notes.getOrPut(w) { mutableListOf() }
            val last = list.lastOrNull()
            if (last != null && last.first == op.kind) {
                list[list.lastIndex] = op.kind to (last.second.first + op.expected to last.second.second + op.said)
            } else list += op.kind to (op.expected to op.said)
        }
        val issues = notes.map { (w, list) ->
            SoundIssue(line.words[w].joinToString("") { it.text }, list.map { (kind, sounds) ->
                when (kind) {
                    Op.EXTRA -> "An extra sound 「${sounds.second}」 was added."
                    Op.MISSING -> "The sound 「${sounds.first}」 was missing."
                    else -> "「${sounds.first}」 sounded like 「${sounds.second}」."
                }
            })
        }
        return Assessment(score, heard, line.words.indices.map { it !in notes }, issues)
    }

    private fun kanaOf(word: List<Ruby>) = hiragana(word.joinToString("") { it.reading.ifEmpty { it.text } }).filter(::isKana)

    /** Turns recogniser text into kana, using the line's own words to read any kanji. */
    private fun toKana(heard: String, line: VideoLine): String {
        var text = normalise(heard)
        val readings = buildList {
            line.words.forEach { word ->
                add(normalise(word.joinToString("") { it.text }) to kanaOf(word))
                word.filter { it.reading.isNotEmpty() }.forEach { add(normalise(it.text) to hiragana(it.reading)) }
            }
        }.filter { (surface, _) -> surface.any { !isKana(it) } }.sortedByDescending { it.first.length }
        for ((surface, kana) in readings) text = text.replace(surface, kana)
        return hiragana(text).filter { isKana(it) || Character.isIdeographic(it.code) }
    }

    private fun normalise(s: String) = Normalizer.normalize(s, Normalizer.Form.NFKC).filter { it.isLetterOrDigit() || it == 'ー' }

    private fun hiragana(s: String) = s.map { if (it in 'ァ'..'ヶ') it - 0x60 else it }.joinToString("")

    private fun isKana(c: Char) = c in 'ぁ'..'ゖ' || c == 'ー'

    /** Splits kana into sounds, keeping small ゃ/ゅ/ょ with the sound before them. */
    private fun morae(kana: String): List<String> {
        val out = mutableListOf<String>()
        for (c in kana) {
            if (c in SMALL && out.isNotEmpty()) out[out.lastIndex] = out.last() + c else out += c.toString()
        }
        return out
    }

    private enum class Op { MATCH, SWAP, MISSING, EXTRA }

    private class Step(val kind: Op, val at: Int, val expected: String, val said: String)

    /** Edit-distance alignment; [Step.at] is the expected sound each step belongs to. */
    private fun align(a: List<String>, b: List<String>): List<Step> {
        val d = Array(a.size + 1) { i -> IntArray(b.size + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
        for (i in 1..a.size) for (j in 1..b.size) {
            d[i][j] = minOf(d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1, d[i - 1][j] + 1, d[i][j - 1] + 1)
        }
        val steps = ArrayDeque<Step>()
        var i = a.size
        var j = b.size
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1 -> {
                    steps.addFirst(Step(if (a[i - 1] == b[j - 1]) Op.MATCH else Op.SWAP, i - 1, a[i - 1], b[j - 1])); i--; j--
                }
                i > 0 && d[i][j] == d[i - 1][j] + 1 -> { steps.addFirst(Step(Op.MISSING, i - 1, a[i - 1], "")); i-- }
                else -> { steps.addFirst(Step(Op.EXTRA, (i - 1).coerceAtLeast(0), "", b[j - 1])); j-- }
            }
        }
        return steps
    }
}
