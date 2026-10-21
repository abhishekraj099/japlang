package com.example.jap

import com.example.jap.data.Memory
import com.example.jap.data.Memory.Grade
import com.example.jap.data.Memory.Level
import com.example.jap.data.Srs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** The FSRS-4.5 memory model and the revision game around it. */
class MemoryTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()

        private const val DAY = 86_400_000L
    }

    @Test
    fun recallFallsToNinetyPercentAfterOneStability() {
        assertEquals(1f, Memory.retrievability(5f, 0f), 0.0001f)
        assertEquals(0.9f, Memory.retrievability(5f, 5f), 0.001f)
        assertTrue(Memory.retrievability(5f, 20f) < 0.8f)
        // At 90% target retention the review comes back after one stability.
        assertEquals(10f, Memory.interval(10f, 0.9f), 0.01f)
    }

    @Test
    fun firstAnswerSetsTheStartingMemory() {
        assertEquals(0.4872f, Memory.next(null, Grade.AGAIN, 0L).stability, 0.001f)
        assertEquals(3.7145f, Memory.next(null, Grade.GOOD, 0L).stability, 0.001f)
        assertEquals(13.8206f, Memory.next(null, Grade.EASY, 0L).stability, 0.001f)
    }

    @Test
    fun reviewingOnTimeMakesMemoriesLastLongerAndLonger() {
        var m = Memory.next(null, Grade.GOOD, 0L)
        var now = 0L
        val levels = mutableListOf(Memory.level(m))
        repeat(4) {
            now = m.due
            val next = Memory.next(m, Grade.GOOD, now)
            assertTrue("stability grows: ${m.stability} -> ${next.stability}", next.stability > m.stability * 1.5f)
            m = next
            levels += Memory.level(m)
        }
        // Learning after the lesson, then Familiar, Strong and Mastered with each on-time review.
        assertEquals(Level.LEARNING, levels.first())
        assertEquals(Level.MASTERED, levels.last())
        assertTrue(levels.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun forgettingShrinksTheMemoryButNotToNothing() {
        val strong = Srs(3, 30 * DAY, stability = 30f, difficulty = 5f, last = 0L, hits = 4)
        val lapsed = Memory.next(strong, Grade.AGAIN, 30 * DAY)
        assertTrue(lapsed.stability < strong.stability)
        assertTrue(lapsed.stability >= 0.1f)
        assertEquals(1, lapsed.misses)
        // Hard and Easy answers move difficulty in opposite directions.
        val base = Srs(2, 10 * DAY, stability = 10f, difficulty = 5f, last = 0L, hits = 2)
        assertTrue(Memory.next(base, Grade.HARD, 10 * DAY).difficulty > Memory.next(base, Grade.EASY, 10 * DAY).difficulty)
        assertTrue(Memory.next(base, Grade.EASY, 10 * DAY).stability > Memory.next(base, Grade.HARD, 10 * DAY).stability)
    }

    @Test
    fun answersAreGradedByCorrectnessAndSpeed() {
        assertEquals(Grade.AGAIN, Memory.grade(false, 1_000))
        assertEquals(Grade.EASY, Memory.grade(true, 3_000))
        assertEquals(Grade.GOOD, Memory.grade(true, 10_000))
        assertEquals(Grade.HARD, Memory.grade(true, 25_000))
    }

    @Test
    fun oldBoxSchedulesBecomeMemories() {
        val old = Memory.fromBox(3, due = 100 * DAY)
        assertEquals(3f, old.stability, 0.01f)   // box 3 was a 72-hour interval
        assertEquals(100 * DAY, old.due)
        assertTrue(old.hits > 0)
        assertEquals(Level.LEARNING, Memory.level(old))
    }

    @Test
    fun comboMultiplierStepsUpAtThreeSixAndTen() {
        val multipliers = (0..12).map { com.example.jap.ui.comboMultiplier(it) }
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4), multipliers)
    }
}
