package com.example.jap.data

import org.json.JSONObject
import java.text.Normalizer

/** Something the learner can say back. [heard] lists spellings the recogniser returns (kanji); [next] null ends the talk. */
data class TalkAnswer(val jp: String, val romaji: String, val en: String, val heard: List<String>, val next: String?)

/** One thing the guide says, and the answers that move the talk on. */
data class TalkTurn(val id: String, val jp: String, val romaji: String, val en: String, val answers: List<TalkAnswer>)

/** A short spoken conversation with the guide, using only what the Basic course has taught by [unlockAfter]. */
data class GuideTalk(val id: String, val title: String, val unlockAfter: String, val start: String, val turns: List<TalkTurn>)

/** The talks with the guide, read from assets/talks.json. */
object Talks {

    var all: List<GuideTalk> = emptyList()
        private set

    fun load(json: JSONObject) {
        all = json.getJSONArray("talks").objects().map { t ->
            GuideTalk(
                t.getString("id"), t.getString("title"), t.getString("unlockAfter"), t.getString("start"),
                t.getJSONArray("turns").objects().map { turn ->
                    TalkTurn(
                        turn.getString("id"), turn.getString("jp"), turn.getString("romaji"), turn.getString("en"),
                        turn.getJSONArray("answers").objects().map {
                            TalkAnswer(
                                it.getString("jp"), it.getString("romaji"), it.getString("en"),
                                it.optJSONArray("heard")?.strings().orEmpty(), it.optString("next").ifEmpty { null }
                            )
                        }
                    )
                }
            )
        }
    }

    /** Testing out of a unit marks its review done too, so either way unlocks its talk. */
    fun unlocked(done: Set<String>): List<GuideTalk> = all.filter { "${it.unlockAfter}-check" in done }

    /** The first answer any recogniser alternative says, ignoring spacing, punctuation, digit width and kana script. */
    fun match(said: List<String>, answers: List<TalkAnswer>): TalkAnswer? {
        val heard = said.map(::normalise).filter { it.isNotEmpty() }.toSet()
        return answers.firstOrNull { a -> (a.heard + a.jp).any { normalise(it) in heard } }
    }

    // NFKC turns full-width digits into ASCII; katakana folds to hiragana since the recogniser picks either.
    private fun normalise(s: String) = Normalizer.normalize(s, Normalizer.Form.NFKC)
        .filter { it.isLetterOrDigit() }
        .map { if (it in 'ァ'..'ヶ') it - 0x60 else it }.joinToString("")
}
