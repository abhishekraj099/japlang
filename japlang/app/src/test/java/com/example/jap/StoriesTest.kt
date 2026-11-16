package com.example.jap

import com.example.jap.data.Choice
import com.example.jap.data.Course
import com.example.jap.data.Exercises
import com.example.jap.data.Stories
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Graded reading stories: one per three Basic units, written only with words the learner has met by then. */
class StoriesTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() {
            ContentTest.load()
            Stories.load(ContentTest.asset("stories.json"))
        }

        /**
         * Conjugated forms of known words whose ending has also been taught by the story's unit,
         * but which no course sentence happens to spell out.
         */
        private val allowedForms = setOf(
            "いきましたか", // いきます + ～ましたか (time)
            "たべました", // たべます + ～ました (time)
            "たべましたか", // たべます + ～ましたか (time)
            "かかりました", // かかります + ～ました (time)
            "ありましたか", // あります + ～ましたか (time)
            "ありませんでした", // あります + ～ませんでした (time)
            "なくしました", // なくします + ～ました (time)
            "のぼりました", // のぼります (登ります) + ～ました (time)
            "のぼる", // のぼります (登ります), dictionary form (cap1)
            "やすんで", // やすみます, て-form (teform)
            "しましょう", // します + ～ましょう (rel1)
            "たべられません", // たべられます, can-do form, + ～ません (cap2)
            "こわかった", // こわい + ～かった (pastadj)
            "からすぎます", // からい + ～すぎます (ease)
        )
    }

    @Test
    fun storiesLoad() {
        val every3rd = KnownWords.basicUnits.filterIndexed { i, _ -> (i + 1) % 3 == 0 }
        assertEquals(23, every3rd.size)
        assertTrue(Stories.all.size >= 30)
        val unlocks = Stories.all.map { it.unlockAfter }
        // Every third unit has exactly one story; extra ones give more reading in the later units.
        assertEquals(every3rd, unlocks.filter { it in every3rd })
        // At most one story per unit, listed in course order.
        assertEquals(KnownWords.basicUnits.filter { it in unlocks }, unlocks)
        Stories.all.forEach { assertEquals("st-${it.unlockAfter}", it.id) }
    }

    @Test
    fun everyStoryIsReadable() {
        Stories.all.forEach { s ->
            assertTrue("${s.id}: ${s.lines.size} lines", s.lines.size in 8..12)
            assertTrue("${s.id}: ${s.questions.size} questions", s.questions.size in 3..4)
            s.lines.forEach { assertTrue("${s.id}: empty line", it.tokens.isNotEmpty() && it.romaji.isNotBlank() && it.en.isNotBlank()) }
            s.questions.forEach { q ->
                assertTrue("${s.id}: ${q.prompt}", q.options.size in 3..4 && q.options.toSet().size == q.options.size && q.explain.isNotBlank())
            }
        }
    }

    @Test
    fun storiesUseOnlyKnownWords() {
        val unknown = Stories.all.flatMap { s ->
            val known = KnownWords.known(s.unlockAfter)
            s.lines.flatMap { l -> l.tokens.flatMap(KnownWords::words) }.filter { it !in known && it !in allowedForms }.map { "${s.id}: $it" }
        }
        assertTrue("Not taught yet: $unknown", unknown.isEmpty())
    }

    @Test
    fun quizKeepsTheRightAnswer() {
        Stories.all.forEach { s ->
            Exercises.forStory(s).zip(s.questions).forEach { (c, q) ->
                c as Choice
                assertEquals(q.options.first(), c.options[c.answer])
                assertEquals(q.options.toSet(), c.options.toSet())
            }
        }
    }

    @Test
    fun unlockCountsTestOut() {
        val real = Stories.all
        try {
            Stories.load(JSONObject("""{"stories": [{"id": "st-b2", "title": "T", "unlockAfter": "b2", "lines": [], "questions": []}]}"""))
            assertEquals(listOf("st-b2"), Stories.unlocked(setOf("b2-check")).map { it.id })
            assertTrue(Stories.unlocked(setOf("b2-p1")).isEmpty())
        } finally {
            Stories.load(ContentTest.asset("stories.json"))
        }
        assertEquals(real.map { it.id }, Stories.all.map { it.id })
        // Testing out of a unit marks all its lessons done, its review among them.
        val testedOut = Course.units.first { it.id == "fam" }.lessons.map { it.id }.toSet()
        assertEquals(listOf("st-fam"), Stories.unlocked(testedOut).map { it.id })
    }
}
