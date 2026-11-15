package com.example.jap

import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Build
import com.example.jap.data.Choice
import com.example.jap.data.Strokes
import com.example.jap.data.Exercises
import com.example.jap.data.Kind
import com.example.jap.data.LessonType
import com.example.jap.data.Placement
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Checks against the real content files in src/main/assets. */
class ContentTest {

    companion object {
        fun asset(name: String) = JSONObject(File("src/main/assets/$name").readText())

        @BeforeClass
        @JvmStatic
        fun load() {
            if (Config.placementLevels.isNotEmpty()) return
            Config.load(asset("config.json"))
            Course.load(asset("course.json"))
            Strokes.load(asset("strokes.json"))
        }
    }

    private fun play(answers: List<Boolean>): Placement.State =
        answers.fold(Placement.State()) { s, ok -> if (s.result != null) s else Placement.answer(s, ok) }

    @Test
    fun placementCapComesFromConfig() {
        // 8 levels × (2 to step up + 1 allowed miss)
        assertEquals(8, Placement.levels.size)
        assertEquals(24, Config.placementMaxQuestions)
    }

    @Test
    fun allCorrectClearsEveryLevel() {
        val end = play(List(30) { true })
        assertEquals(Placement.levels.size, end.result)
        assertEquals(16, end.asked)
    }

    @Test
    fun oneMissPerLevelStillClearsEveryLevel() {
        val perLevel = listOf(false, true, true)
        val end = play(List(Placement.levels.size) { perLevel }.flatten())
        assertEquals(Placement.levels.size, end.result)
        assertEquals(24, end.asked)
    }

    @Test
    fun twoMissesEndTheTestAtThatLevel() {
        val end = play(listOf(true, true, false, false))
        assertEquals(1, end.result)
        assertEquals(4, end.asked)
    }

    @Test
    fun placementCoversTheWholeMainCourse() {
        // Clearing every level skips all 59 units of the Basic course, so there is no next unit in it.
        assertEquals(59, Placement.unitsCleared(Placement.levels.size))
        assertEquals(Course.courses.first().unitIds, Course.units.take(59).map { it.id })
    }

    @Test
    fun unitNumbersSkipTheAlphabet() {
        val byId = Course.units.associateBy { it.id }
        assertEquals(0, Course.unitNumber(byId.getValue("hira")))
        assertEquals(1, Course.unitNumber(byId.getValue("b1")))
        assertEquals(20, Course.unitNumber(byId.getValue("colors")))
        // Themed courses number from 1.
        assertEquals(1, Course.unitNumber(byId.getValue("tr-air")))
        // After clearing Hiragana, Katakana and Basics (7 units), the next unit is Unit 6 on the Learn tab.
        assertEquals(6, Course.unitNumber(Course.units[Placement.unitsCleared(3)]))
    }

    @Test
    fun soundAlikeKanaAreNeverQuizzed() {
        listOf("h-dji", "h-dzu", "k-dji", "k-dzu").forEach { id ->
            val item = Course.item(id)!!
            assertFalse(Course.quizzable(item))
            assertNull(Course.allItems.firstOrNull { it.id == id })
        }
        assertTrue(Course.quizzable(Course.item("h-ji")!!))
    }

    @Test
    fun combinedTabQuizzesNeverShowSoundAlikeKana() {
        val banned = setOf("ぢ", "づ", "ヂ", "ヅ")
        listOf(Kind.HIRAGANA, Kind.KATAKANA).forEach { kind ->
            val chart = Course.kanaChart(kind).drop(1).flatMap { it.second }.flatten().filterNotNull()
            assertTrue(chart.any { it.jp in banned })   // shown in the chart
            val quiz = chart.filter(Course::quizzable)  // what the Alphabet tab quizzes
            repeat(50) {
                val text = Exercises.forReview(quiz.shuffled().take(12)).joinToString { it.toString() }
                banned.forEach { assertFalse("$it was quizzed", text.contains(it)) }
            }
        }
    }

    @Test
    fun aRevisionFollowsEveryTwoLessons() {
        val revisions = Course.lessons.filter { it.id.contains("-rev") }
        assertEquals(102, revisions.size)
        Course.units.forEach { unit ->
            val lessons = unit.lessons
            lessons.forEachIndexed { i, lesson ->
                if (!lesson.id.contains("-rev")) return@forEachIndexed
                // It mixes exactly the two teaching lessons before it, and comes before the unit's final review.
                val taught = lessons.take(i).filter { it.type in setOf(LessonType.KANA, LessonType.PRACTICE, LessonType.KANJI) }.takeLast(2)
                assertEquals(lesson.title, taught.flatMap { it.items }.map { it.id }.distinct().sorted(), lesson.items.map { it.id }.sorted())
                assertEquals(LessonType.CHECKPOINT, lesson.type)
                assertTrue(i < lessons.lastIndex)
            }
        }
        val hira = Course.units.first { it.id == "hira" }.lessons.map { it.title }
        assertEquals("Review あ + か", hira[hira.indexOf("か row") + 1])
    }

    @Test
    fun everyUnitSaysWhatItTeaches() {
        Course.units.forEach { unit -> assertTrue("No goals: ${unit.id}", Course.goals(unit).isNotEmpty()) }
        val numbers = Course.goals(Course.units.first { it.id == "kj1" })
        assertEquals("Read and write 10 kanji: 一 二 三 四 五 六 七 八 九 十", numbers.first())
        assertTrue(Course.goals(Course.units.first { it.id == "hira" }).first().startsWith("Read and write 46 hiragana"))
    }

    /** Every Basic course unit except the alphabet ones has full depth: six sentences per set and a conversation. */
    private val contentReadyUnits = Course.courses.first().unitIds.filterNot { id -> Course.units.first { it.id == id }.alphabet }

    @Test
    fun basicUnitsHaveSixSentencesPerSet() {
        val byId = Course.units.associateBy { it.id }
        contentReadyUnits.forEach { id ->
            val practices = byId.getValue(id).lessons.filter { it.type == LessonType.PRACTICE }
            assertTrue("$id has no practice sets", practices.isNotEmpty())
            practices.forEach { lesson ->
                assertTrue("${lesson.id} has ${lesson.sentences.size} sentences", lesson.sentences.size >= 6)
                assertTrue("${lesson.id} has too few gap sentences", lesson.sentences.count { it.blank >= 0 } >= 2)
            }
        }
    }

    @Test
    fun speakingLessonsSayEachPhraseOnce() {
        Course.lessons.filter { it.type == LessonType.SPEAKING }.forEach { lesson ->
            val keys = lesson.phrases.map { p -> p.jp.filter(Char::isLetterOrDigit) }
            assertEquals("${lesson.id} repeats a phrase", keys.size, keys.toSet().size)
        }
    }

    @Test
    fun basicUnitsHaveAConversation() {
        val talks = Course.conversations.associateBy { it.id }
        contentReadyUnits.forEach { id ->
            val lines = talks["$id-talk"]?.dialogue.orEmpty()
            assertTrue("$id has ${lines.size} conversation lines", lines.size >= 6)
        }
    }

    @Test
    fun practiceSentencesAreUnderstoodBeforeTheyAreBuilt() {
        Course.lessons.filter { it.type == LessonType.PRACTICE && it.sentences.isNotEmpty() }.forEach { lesson ->
            val ex = Exercises.forLesson(lesson)
            val asked = ex.filterIsInstance<Choice>().filter { c -> lesson.sentences.any { it.en == c.options.getOrNull(c.answer) } }
            assertEquals(lesson.id, lesson.sentences.size, asked.size)
            assertTrue(lesson.id, ex.indexOf(asked.last()) < ex.indexOfFirst { it is Build })
            asked.forEach { assertTrue("Too few options in ${lesson.id}", it.options.size >= 2) }
        }
    }
}
