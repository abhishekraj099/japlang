package com.example.jap.data

/**
 * Rules for checking handwriting. The learner writes one character per box; each box is read on its own
 * and the boxes are compared with every spelling the item accepts.
 */
object Writing {

    /** Printed in front of the boxes and never written, as in ～方. */
    private const val MARK = "～"

    /**
     * Pairs that look the same when written alone in a box. A box cannot show size, so small kana match
     * their full-size form, and some katakana are drawn exactly like a kanji.
     */
    private val sameShape: List<String> = listOf(
        "あぁ", "いぃ", "うぅ", "えぇ", "おぉ", "つっ", "やゃ", "ゆゅ", "よょ", "わゎ",
        "アァ", "イィ", "ウゥ", "エェ", "オォ", "ツッ", "ヤャ", "ユュ", "ヨョ", "ワヮ", "カヵ", "ケヶ",
        "へヘ", "べベ", "ぺペ", "りリ",
        "ー一｜|", "ロ口", "カ力", "エ工", "ニ二", "ハ八", "タ夕", "ト卜", "オ才"
    )

    /** What is printed before the boxes, e.g. "～". */
    fun prefix(item: Item): String = if (item.jp.startsWith(MARK)) MARK else ""

    /**
     * Every spelling the learner may write, main one first: the written form, then the kana reading for words
     * that have kanji. Kana and single kanji have one spelling.
     */
    fun spellings(item: Item): List<String> {
        val main = item.jp.removePrefix(MARK).replace(" ", "")
        if (item.kind != Kind.WORD) return listOf(main)
        val kana = item.reading.removePrefix(MARK).replace(" ", "")
        return listOf(main, kana).filter { it.isNotEmpty() }.distinct()
    }

    /** True when [item] has a kanji spelling and a kana one. */
    fun hasKanji(item: Item): Boolean = spellings(item).first().any(::isKanji)

    fun isKanji(c: Char): Boolean = c in '一'..'鿿' || c == '々'

    /** How many boxes to show: enough for the longest spelling, or exactly [spelling] when tracing it. */
    fun boxes(item: Item, spelling: String? = null): Int = spelling?.length ?: spellings(item).maxOf { it.length }

    fun same(a: Char, b: Char): Boolean = a == b || sameShape.any { a in it && b in it }

    /**
     * The spelling the written boxes match, or null. [read] holds each box's candidates, best first, or null for an
     * empty box; empty boxes are ignored. A box matches when the expected character is among its first [top] candidates.
     */
    fun match(item: Item, read: List<List<String>?>, top: Int): String? {
        val written = read.filterNotNull()
        return spellings(item).firstOrNull { spelling ->
            spelling.length == written.size && spelling.indices.all { i ->
                written[i].take(top).any { it.trim().length == 1 && same(it.trim()[0], spelling[i]) }
            }
        }
    }

    /** What the recogniser read, one best guess per written box, for showing next to the answer. */
    fun guess(read: List<List<String>?>): String =
        read.filterNotNull().joinToString("") { box -> box.firstOrNull { it.trim().length == 1 }?.trim() ?: "?" }
}
