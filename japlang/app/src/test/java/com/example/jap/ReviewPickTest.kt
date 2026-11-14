package com.example.jap

import com.example.jap.data.Course
import com.example.jap.data.Review
import com.example.jap.data.Srs
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

/** The items a practice session asks, shared by the Practice buttons and "Keep going". */
class ReviewPickTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()

        private const val DAY = 86_400_000L
    }

    private val now = 100 * DAY
    private fun srs(stability: Float) = Srs(box = 1, due = now - 5 * DAY, stability = stability, last = now - 5 * DAY)
    private val memory = mapOf("h-a" to srs(30f), "h-i" to srs(1f), "h-u" to srs(10f), "h-dji" to srs(0.1f))
    private val due = listOf(Course.item("h-u")!! to memory.getValue("h-u"), Course.item("h-i")!! to memory.getValue("h-i"))

    @Test
    fun dueItemsComeFirstWeakestFirst() {
        val plan = Review.plan(due, 0, 0, now)
        assertEquals(listOf("h-i", "h-u"), Review.pick(true, plan, memory, 12, now).map { it.id })
        assertEquals(listOf("h-i"), Review.pick(true, plan, memory, 1, now).map { it.id })
    }

    @Test
    fun weakestSkipsChartOnlyItemsAndRespectsTheSize() {
        val plan = Review.plan(due, 0, 0, now)
        assertEquals(listOf("h-i", "h-u"), Review.pick(false, plan, memory, 2, now).map { it.id })
    }

    @Test
    fun nothingDueGivesNoRevisionButStillWeakestWords() {
        val none = Review.plan(emptyList(), 0, 0, now)
        assertEquals(emptyList<String>(), Review.pick(true, none, memory, 12, now).map { it.id })
        assertEquals(listOf("h-i", "h-u", "h-a"), Review.pick(false, none, memory, 12, now).map { it.id })
    }
}
