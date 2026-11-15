package com.example.jap

import com.example.jap.data.Choice
import com.example.jap.data.Course
import com.example.jap.data.Exercises
import com.example.jap.data.LessonType
import com.example.jap.data.Memory
import com.example.jap.data.Review
import com.example.jap.data.Srs
import com.example.jap.ui.memoryChanges
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Grammar points (each practice set's note) have their own memory, reviewed alongside items. */
class GrammarReviewTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()

        private const val DAY = 86_400_000L
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

    /** Every practice set in the raw file, as (unitId, 1-based index, set). */
    private val sets = ContentTest.asset("course.json").getJSONArray("units").objects().flatMap { u ->
        u.optJSONArray("practices")?.objects().orEmpty().mapIndexed { i, p -> Triple(u.getString("id"), i + 1, p) }
    }

    @Test
    fun everyNoteHasAStableId() {
        val ids = sets.mapNotNull { (unit, n, p) ->
            p.optJSONObject("note")?.let { note ->
                assertEquals("$unit set $n", "g-$unit-$n", note.optString("id"))
                note.getString("id")
            }
        }
        assertEquals(ids.size, ids.toSet().size)
        // Ids come from the file, never computed: the course exposes exactly those ids.
        assertEquals(ids, Course.grammar.map { it.id })
    }

    @Test
    fun aPracticeSetWithoutANoteIsSkipped() {
        val noteless = sets.filter { it.third.optJSONObject("note") == null }
        assertTrue("content has no noteless set to check", noteless.isNotEmpty())
        noteless.forEach { (unit, n, _) ->
            assertTrue(Course.grammar.none { it.id == "g-$unit-$n" })
            val lesson = Course.lessons.first { it.id == "$unit-p$n" }
            assertTrue(Exercises.forLesson(lesson).isNotEmpty())
        }
    }

    @Test
    fun practiceLessonsTeachTheirGrammarPoint() {
        Course.lessons.filter { it.type == LessonType.PRACTICE && it.note != null && it.sentences.isNotEmpty() }.forEach { lesson ->
            val id = lesson.note!!.id
            assertTrue(lesson.id, Exercises.forLesson(lesson).any { it.itemId == id })
            assertNotNull(Course.grammarPoint(id))
        }
    }

    @Test
    fun forGrammarAsksEachPointOnce() {
        val points = Course.grammar.filter { it.sentences.isNotEmpty() }
        val ex = Exercises.forGrammar(points)
        assertEquals(points.size, ex.size)
        points.zip(ex).forEach { (point, e) ->
            assertTrue(e is Choice)
            assertEquals(point.id, e.itemId)
            if (point.sentences.any { it.blank >= 0 }) assertEquals(point.id, "Fill in the blank", (e as Choice).instruction)
        }
    }

    @Test
    fun dueGrammarSkipsItemsAndUnknownIds() {
        val now = 100 * DAY
        val (overdue, due, later) = Course.grammar.take(3)
        val srs = mapOf(
            "h-a" to Srs(box = 1, due = now - DAY),
            "g-nope-9" to Srs(box = 1, due = now - DAY),
            due.id to Srs(box = 1, due = now - DAY),
            overdue.id to Srs(box = 1, due = now - 3 * DAY),
            later.id to Srs(box = 1, due = now + DAY)
        )
        assertEquals(listOf(overdue.id, due.id), Review.dueGrammar(srs, now).map { it.id })
        assertFalse(Review.dueGrammar(srs, now).any { it.id == later.id })
    }

    @Test
    fun grammarDoesNotCountAsItems() {
        val point = Course.grammar.first()
        val results = mapOf("h-a" to Memory.Grade.GOOD, point.id to Memory.Grade.GOOD)
        // The daily cap, the learned badge and memory strength count only words and characters.
        assertEquals(setOf("h-a"), Review.itemsOnly(results).keys)
    }

    @Test
    fun revisionSummaryShowsGrammarLikeItems() {
        val point = Course.grammar.first()
        val grades = mapOf("h-a" to Memory.Grade.GOOD, point.id to Memory.Grade.AGAIN, "g-nope-9" to Memory.Grade.GOOD)
        val changes = memoryChanges(grades, emptyMap(), 100 * DAY)
        assertEquals(2, changes.size)
        val g = changes.single { it.jp == point.note.pattern }
        assertEquals(point.note.title, g.meaning)
        assertEquals(Memory.Grade.AGAIN, g.grade)
    }
}
