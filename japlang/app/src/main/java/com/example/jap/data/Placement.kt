package com.example.jap.data

import kotlin.random.Random

/**
 * Adaptive placement test. The levels and the rules for moving between them come from
 * assets/config.json; the test starts at the first level and moves up after a run of correct answers.
 */
object Placement {

    val levels: List<PlacementLevel> get() = Config.placementLevels

    private fun units(level: Int): List<CourseUnit> = Course.units.filter { it.id in levels[level].units }

    /**
     * Where a test stands: the [level] being asked, the current [run] of right answers, [misses] at this level,
     * questions [asked] so far and how many were [right]. [result] is the number of levels cleared once it is over.
     */
    data class State(val level: Int = 0, val run: Int = 0, val misses: Int = 0, val asked: Int = 1, val right: Int = 0, val result: Int? = null)

    /** The state after one answer; "I don't know" counts as wrong. */
    fun answer(state: State, correct: Boolean): State {
        val top = levels.lastIndex
        var s = if (correct) state.copy(right = state.right + 1, run = state.run + 1) else state.copy(run = 0, misses = state.misses + 1)
        if (correct && s.run >= Config.placementStepUp) {
            if (s.level == top) return s.copy(result = top + 1)
            s = s.copy(level = s.level + 1, run = 0, misses = 0)
        }
        return if (s.misses >= Config.placementMaxMisses || s.asked >= Config.placementMaxQuestions) s.copy(result = s.level)
        else s.copy(asked = s.asked + 1)
    }

    /** How many course units a learner who cleared [passed] levels may skip. */
    fun unitsCleared(passed: Int): Int = levels.take(passed).sumOf { it.units.size }

    /** A question at [level] that is not in [used]; [used] is updated with the one returned. */
    fun question(level: Int, used: MutableSet<String>): Choice {
        val lessons = units(level).flatMap { it.lessons }.filter { it.type != LessonType.CHECKPOINT }
        val items = lessons.flatMap { it.items }.distinctBy { it.id }
        val blanks = lessons.flatMap { it.sentences }.filter { it.blank >= 0 }

        val sentence = blanks.filter { it.text !in used }.randomOrNull()
        if (sentence != null && Random.nextInt(3) == 0) {
            used += sentence.text
            return Exercises.blank(sentence)
        }
        val item = items.filter { it.id !in used }.randomOrNull() ?: items.random()
        used += item.id
        return if (Random.nextBoolean()) Exercises.quick(item, items) else Exercises.reverse(item, items)
    }
}
