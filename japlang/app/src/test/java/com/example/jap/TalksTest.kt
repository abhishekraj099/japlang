package com.example.jap

import com.example.jap.data.TalkAnswer
import com.example.jap.data.Talks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Spoken mini-conversations with the guide: well-formed, matched forgivingly, and written with known words only. */
class TalksTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() {
            ContentTest.load()
            Talks.load(ContentTest.asset("talks.json"))
        }

        /** Conjugated forms of known words whose ending is taught by the talk's unit, but no course line spells out. */
        private val allowedForms = setOf(
            "いきましたか", // いきます + ～ましたか (time)
            "しましたか", // します + ～ましたか (time)
        )
    }

    @Test
    fun talksAreWellFormed() {
        val basic = KnownWords.basicUnits
        assertTrue("${Talks.all.size} talks", Talks.all.size >= 8)
        val unlocks = Talks.all.map { basic.indexOf(it.unlockAfter) }
        assertTrue("unlockAfter must be Basic units: $unlocks", unlocks.all { it >= 0 })
        assertEquals("one talk per unit", unlocks.size, unlocks.toSet().size)
        // Spread across the course: one in the first three units and one in the second half.
        assertTrue(unlocks.min() < 3 && unlocks.max() >= basic.size / 2)
        Talks.all.forEach { t ->
            val ids = t.turns.map { it.id }
            assertEquals("${t.id}: start", t.start, ids.first())
            assertTrue("${t.id}: ${ids.size} turns", ids.size in 4..8)
            t.turns.forEachIndexed { i, turn ->
                assertTrue("${t.id}/${turn.id}: answers", turn.answers.size >= 2)
                // Answers become tap choices, so none may repeat another.
                assertEquals("${t.id}/${turn.id}: repeated answer", turn.answers.size, turn.answers.map { it.en }.toSet().size)
                turn.answers.forEach { a ->
                    assertTrue("${t.id}/${turn.id}: empty answer", a.jp.isNotBlank() && a.romaji.isNotBlank() && a.en.isNotBlank())
                    // Pointing only forward means every talk ends.
                    assertTrue("${t.id}/${turn.id} → ${a.next}", a.next == null || ids.indexOf(a.next) > i)
                }
            }
            val reached = mutableSetOf(t.start)
            t.turns.forEach { turn -> if (turn.id in reached) turn.answers.mapNotNullTo(reached) { it.next } }
            assertEquals("${t.id}: unreachable turns", ids.toSet(), reached)
        }
    }

    @Test
    fun matchAcceptsVariants() {
        val answers = Talks.all.first().turns.first().answers
        val first = answers.first()
        assertEquals(first, Talks.match(listOf(" ${first.jp.replace(" ", "  ")} "), answers))
        assertEquals(first, Talks.match(listOf(first.jp.replace("、", "").replace("。", "!?")), answers))

        val time = listOf(
            TalkAnswer("6じ に おきます。", "rokuji ni okimasu", "At six.", listOf("6時に起きます"), null),
            TalkAnswer("7じ に おきます。", "shichiji ni okimasu", "At seven.", listOf("7時に起きます"), "t2")
        )
        // Kanji, full-width digits and spaces from the recogniser still match; the second alternative can win.
        assertEquals(time[1], Talks.match(listOf("なな時に起きます", "７時に 起きます。"), time))
        assertEquals(time[0], Talks.match(listOf("6時に起きます"), time))
        assertNull(Talks.match(listOf("今日は暑いですね", ""), time))
        assertNull(Talks.match(emptyList(), time))
    }

    @Test
    fun talksUseOnlyKnownWords() {
        val unknown = Talks.all.flatMap { t ->
            val known = KnownWords.known(t.unlockAfter)
            val lines = t.turns.flatMap { turn -> listOf(turn.jp) + turn.answers.map { it.jp } }
            lines.flatMap(KnownWords::words).filter { it !in known && it !in allowedForms }.map { "${t.id}: $it" }
        }
        assertTrue("Not taught yet: $unknown", unknown.isEmpty())
    }

    @Test
    fun unlockCountsTestOut() {
        assertTrue(Talks.unlocked(emptySet()).isEmpty())
        val t = Talks.all.first()
        assertEquals(listOf(t), Talks.unlocked(setOf("${t.unlockAfter}-check")))
    }

    @Test
    fun titlesAreUniqueAcrossTalksConversationsAndStories() {
        // Talks, unit conversations and stories sit side by side on the Practice tab; two with one title confuse learners.
        val titles = Talks.all.map { it.title } + com.example.jap.data.Course.conversations.map { it.title } +
            com.example.jap.data.Stories.all.map { it.title }
        assertEquals(titles.groupBy { it }.filterValues { it.size > 1 }.keys, emptySet<String>())
    }
}
