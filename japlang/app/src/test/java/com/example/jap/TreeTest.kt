package com.example.jap

import com.example.jap.data.Tree
import com.example.jap.data.Tree.Condition
import com.example.jap.data.Tree.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class TreeTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()
    }

    private val today = 1000L
    private fun days(vararg back: Int) = back.map { today - it }.toSet()
    private fun state(studied: Set<Long>, watered: Set<Long> = emptySet(), saved: Set<Long> = emptySet()) =
        Tree.state(today, studied, saved, watered)

    @Test
    fun newLearnerHasAFullSproutThatCannotBeWateredYet() {
        val s = state(emptySet())
        assertEquals(Stage.SPROUT, s.stage)
        assertEquals(Condition.THIRSTY, s.condition)
        assertEquals(1f, s.leaves)
        assertFalse(s.canWater)
    }

    @Test
    fun studyingFillsTheCanOncePerDay() {
        assertTrue(state(days(0)).canWater)
        val watered = state(days(0), watered = days(0))
        assertFalse(watered.canWater)
        assertTrue(watered.wateredToday)
        assertEquals(Stage.SAPLING, watered.stage)
    }

    @Test
    fun wateringGrowsTheTreeThroughAYear() {
        // One watering a day: a sapling on day 1, then day 7, 30, 100 and a great sakura on day 365.
        assertEquals(Stage.SAPLING, Tree.stageFor(1))
        assertEquals(Stage.SAPLING, Tree.stageFor(6))
        assertEquals(Stage.YOUNG, Tree.stageFor(7))
        assertEquals(Stage.GROWN, Tree.stageFor(30))
        assertEquals(Stage.BLOSSOM, Tree.stageFor(100))
        assertEquals(Stage.BLOSSOM, Tree.stageFor(364))
        assertEquals(Stage.GREAT, Tree.stageFor(365))
        assertEquals(Stage.GREAT, Tree.stageFor(2000))
        assertEquals(5, state(days(0, 1), watered = days(1, 2)).toNextStage)
        assertEquals(0, state(days(0), watered = (1..365).map { today - it }.toSet()).toNextStage)
    }

    @Test
    fun missedDaysDryTheTreeThenStripItThenWitherIt() {
        val thirsty = state(days(1, 2))
        assertEquals(Condition.THIRSTY, thirsty.condition)
        assertEquals(1f, thirsty.leaves)

        val drying = state(days(2, 3))
        assertEquals(Condition.DRYING, drying.condition)
        assertEquals(1, drying.daysMissed)
        assertTrue(drying.leaves in 0.15f..0.7f)

        val bare = state(days(4))
        assertEquals(Condition.BARE, bare.condition)
        assertEquals(0f, bare.leaves)

        assertEquals(Condition.WITHERED, state(days(8)).condition)
        assertEquals(0f, state(days(8)).leaves)
    }

    @Test
    fun leavesGrowBackOverThreeDaysAfterADrySpell() {
        // Studied long ago, then a gap, then back.
        assertEquals(0.4f, state(days(0, 20)).leaves, 0.001f)
        assertEquals(0.7f, state(days(0, 1, 20)).leaves, 0.001f)
        assertEquals(1f, state(days(0, 1, 2, 20)).leaves, 0.001f)
    }

    @Test
    fun omamoriDaysCountAsCare() {
        val s = state(days(0, 2), saved = days(1))
        assertEquals(Condition.HEALTHY, s.condition)
        assertEquals(3, s.run)
        assertEquals(1f, s.leaves)
    }
}
