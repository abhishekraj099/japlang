package com.example.jap

import com.example.jap.data.Build
import com.example.jap.data.Choice
import com.example.jap.data.Course
import com.example.jap.data.Dictate
import com.example.jap.data.DialogueCard
import com.example.jap.data.Exercises
import com.example.jap.data.Match
import com.example.jap.data.NoteCard
import com.example.jap.data.Skill
import com.example.jap.data.Skills
import com.example.jap.data.Speak
import com.example.jap.data.Teach
import com.example.jap.data.TypeIn
import com.example.jap.data.Write
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Smart practice: which skill each answer trains, how skills are scored, and which one a session trains next. */
class SkillsTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()
    }

    private fun log(right: Int, wrong: Int) = List(right) { true } + List(wrong) { false }

    @Test
    fun mappingCoversEveryExerciseType() {
        val item = Course.allItems.first { it.romaji != "wo" }
        val phrase = Course.phrases.first()
        val choice = Choice(instruction = "What does this mean?", prompt = "x", options = listOf("a", "b"), answer = 0, explain = "")
        assertEquals(Skill.READING, Skills.of(choice))
        assertEquals(Skill.LISTENING, Skills.of(choice.copy(listen = true)))
        assertEquals(Skill.GRAMMAR, Skills.of(choice.copy(instruction = "Fill in the blank")))
        assertEquals(Skill.GRAMMAR, Skills.of(choice.copy(itemId = "g-b1-1")))
        assertEquals(Skill.READING, Skills.of(Match(listOf("a" to "b"))))
        assertEquals(Skill.READING, Skills.of(TypeIn("x", listOf("x"), "x", item.id)))
        assertEquals(Skill.GRAMMAR, Skills.of(Build("en", listOf("a"), listOf("a"), "a")))
        assertEquals(Skill.WRITING, Skills.of(Write(item)))
        assertEquals(Skill.WRITING, Skills.of(Dictate(item)))
        assertEquals(Skill.SPEAKING, Skills.of(Speak(phrase)))
        assertNull(Skills.of(Teach(item)))
        assertNull(Skills.of(DialogueCard("t", emptyList())))
        val point = Course.grammarPoint("g-b1-1")!!
        assertNull(Skills.of(NoteCard(point.note, emptyList())))
    }

    @Test
    fun percentNeedsTenAnswers() {
        assertNull(Skills.percent(log(9, 0)))
        assertEquals(70, Skills.percent(log(7, 3)))
    }

    @Test
    fun logKeepsLastFifty() {
        var log = emptyList<Boolean>()
        repeat(60) { log = Skills.append(log, it < 10) }
        assertEquals(Skills.WINDOW, log.size)
        // The ten early right answers fell out of the window.
        assertTrue(log.none { it })
    }

    @Test
    fun pickChoosesLowestKnownSkill() {
        val logs = mapOf(
            Skill.READING to log(9, 1), Skill.LISTENING to log(4, 6), Skill.WRITING to log(8, 2),
            Skill.SPEAKING to log(7, 3), Skill.GRAMMAR to log(6, 4)
        )
        assertEquals(Skill.LISTENING, Skills.pick(logs, emptySet()))
    }

    @Test
    fun pickFallsBackToLeastPractised() {
        val skill = Skills.pick(emptyMap(), setOf(Skill.WRITING))
        assertFalse(skill == Skill.WRITING)
        // A skill not yet known is tried before the weakest known one, so every skill gets a score.
        val logs = mapOf(Skill.READING to log(2, 8), Skill.LISTENING to log(3, 0))
        assertEquals(Skill.WRITING, Skills.pick(logs, setOf(Skill.GRAMMAR, Skill.SPEAKING)))
    }

    @Test
    fun pickNeverReturnsUnavailable() {
        val off = setOf(Skill.WRITING, Skill.SPEAKING)
        val weakest = mapOf(Skill.WRITING to log(0, 20), Skill.SPEAKING to log(0, 20), Skill.READING to log(20, 0), Skill.LISTENING to log(15, 5), Skill.GRAMMAR to log(19, 1))
        listOf(emptyMap(), weakest, mapOf(Skill.READING to log(50, 0))).forEach { logs ->
            assertFalse(Skills.pick(logs, off) in off)
        }
    }

    @Test
    fun forSmartMatchesSkill() {
        val items = Course.lessons.flatMap { it.items }.filter(Course::quizzable).distinctBy { it.id }.take(20)
        val grammar = listOf("g-b1-1", "g-b2-1", "g-b2-2").mapNotNull(Course::grammarPoint)
        val phrases = Course.phrases.take(10)
        Skill.entries.forEach { skill ->
            val exercises = Exercises.forSmart(skill, items, grammar, phrases)
            assertTrue("$skill has exercises", exercises.isNotEmpty())
            exercises.mapNotNull(Skills::of).forEach { assertEquals(skill, it) }
        }
    }
}
