package com.example.jap

import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.Review
import com.example.jap.data.Srs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class QuickWinsTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()
    }

    private fun due(n: Int) = List(n) { i -> Item("w-$i", "x", "", "x$i", "m", kind = Kind.WORD) to Srs(box = i % 5, due = i.toLong()) }

    @Test
    fun everyKanaHasAPictureTrickAndAnExampleWord() {
        val kana = Course.lessons.flatMap { it.items }.filter { it.isKana }.distinctBy { it.id }
        assertEquals(92, kana.size)
        kana.forEach {
            assertTrue(it.jp, it.emoji.isNotEmpty() && it.note.isNotEmpty())
            val example = it.example!!
            assertTrue("${it.jp} not in ${example.jp}", it.jp in example.jp)
            assertTrue(example.romaji.isNotEmpty() && example.en.isNotEmpty())
        }
    }

    @Test
    fun smallBacklogIsAllOfferedToday() {
        val plan = Review.plan(due(12), doneToday = 0, daysAway = 1)
        assertEquals(12, plan.today.size)
        assertFalse(plan.catchUp)
        assertEquals(1, plan.daysToClear)
    }

    @Test
    fun hugeBacklogAfterABreakIsCappedAndSpread() {
        assertEquals(50, Config.reviewDailyCap)
        val plan = Review.plan(due(230), doneToday = 0, daysAway = 10)
        assertEquals(50, plan.today.size)
        assertEquals(230, plan.backlog)
        assertTrue(plan.catchUp && plan.welcomeBack)
        assertEquals(5, plan.daysToClear) // 50 today, then 50 + 50 + 50 + 30
        // Weakest first: every box-0 item comes before any box-1 item.
        assertTrue(plan.today.take(46).all { it.id.removePrefix("w-").toInt() % 5 == 0 })
    }

    @Test
    fun capCountsWhatWasAlreadyReviewedToday() {
        assertEquals(20, Review.plan(due(80), doneToday = 30, daysAway = 0).today.size)
        val done = Review.plan(due(80), doneToday = 50, daysAway = 0)
        assertTrue(done.today.isEmpty() && done.capReached)
    }

    @Test
    fun backlogWithoutABreakIsCatchUpButNotWelcomeBack() {
        val plan = Review.plan(due(60), doneToday = 0, daysAway = 1)
        assertTrue(plan.catchUp)
        assertFalse(plan.welcomeBack)
    }
}
