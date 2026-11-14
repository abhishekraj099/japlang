package com.example.jap

import com.example.jap.data.Course
import com.example.jap.data.Kind
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Checks every content file for the mistakes that break lessons quietly: one word stored under two ids (it then
 * has two separate memories), missing meanings or readings, broken sentences and impossible questions.
 * Add to [differentWords] only when two entries really are different words written the same way.
 */
class ContentCheckTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()

        /** Spelled the same but different words: かけます "make (a call)" / "can write", 一日 ついたち / いちにち. */
        private val differentWords = setOf("かけます", "一日")

        private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
    }

    private val course: JSONObject = ContentTest.asset("course.json")

    private fun allSentences(): List<JSONObject> = course.getJSONArray("units").objects().flatMap { u ->
        (u.optJSONArray("lessons") ?: u.optJSONArray("practices"))!!.objects().flatMap { l ->
            l.optJSONArray("sentences")?.objects().orEmpty()
        }
    }

    @Test
    fun eachWordHasOneId() {
        val words = Course.lessons.flatMap { it.items }.filter { it.kind == Kind.WORD }
        val idsBySpelling = words.groupBy { it.jp }.mapValues { (_, v) -> v.map { it.id }.toSet() }
        val split = idsBySpelling.filter { (jp, ids) -> ids.size > 1 && jp !in differentWords }
        assertTrue("Same word under several ids: $split", split.isEmpty())
    }

    @Test
    fun everyItemCanBeTaught() {
        Course.allItems.forEach { item ->
            assertTrue("No romaji: ${item.id}", item.romaji.isNotBlank())
            if (item.kind == Kind.WORD || item.kind == Kind.KANJI) assertTrue("No meaning: ${item.id}", item.meaning.isNotBlank())
            if (item.jp.any { it in '一'..'鿿' } && item.kind == Kind.WORD) {
                assertTrue("Kanji word without a reading: ${item.jp}", item.reading.isNotBlank())
            }
        }
    }

    @Test
    fun sentencesAreWellFormed() {
        allSentences().forEach { s ->
            val tokens = s.getJSONArray("tokens").let { a -> List(a.length()) { a.getString(it) } }
            val label = tokens.joinToString("")
            assertTrue("Empty sentence", tokens.isNotEmpty() && tokens.none { it.isBlank() })
            assertTrue("No English: $label", s.getString("en").isNotBlank())
            assertTrue("No romaji: $label", s.getString("romaji").isNotBlank())
            val blank = s.optInt("blank", -1)
            assertTrue("Blank out of range: $label", blank < tokens.size)
            if (blank >= 0) {
                val distract = s.optJSONArray("distract")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
                assertTrue("Fill-in has no wrong options: $label", distract.isNotEmpty())
                assertTrue("A wrong option is the answer: $label", tokens[blank] !in distract)
            }
        }
    }

    /** A particle gap offers only particles and a word gap offers no particles, so no answer is given away by its kind. */
    @Test
    fun gapOptionsAreTheSameKindAsTheAnswer() {
        val particles = setOf("は", "が", "を", "に", "で", "へ", "と", "も", "の", "か", "や", "から", "まで", "より", "ね", "よ")
        fun isParticle(token: String) = token.trimEnd('、') in particles
        allSentences().forEach { s ->
            val blank = s.optInt("blank", -1)
            if (blank < 0) return@forEach
            val tokens = s.getJSONArray("tokens").let { a -> List(a.length()) { a.getString(it) } }
            val distract = s.optJSONArray("distract")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
            val label = tokens.joinToString("")
            val answerIsParticle = isParticle(tokens[blank])
            assertTrue("Wrong options give away the gap: $label", distract.all { isParticle(it) == answerIsParticle })
        }
    }

    @Test
    fun mockTestQuestionsHaveOneAnswerAndDistinctOptions() {
        val exam = ContentTest.asset("exam.json")
        exam.getJSONArray("questions").objects().forEach { q ->
            val options = q.getJSONArray("options").let { a -> List(a.length()) { a.getString(it) } }
            assertTrue("Too few options: ${q.getString("prompt")}", options.size >= 3)
            assertTrue("Repeated option: ${q.getString("prompt")}", options.toSet().size == options.size)
        }
    }
}
