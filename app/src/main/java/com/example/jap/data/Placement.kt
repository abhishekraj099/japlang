package com.example.jap.data

import kotlin.random.Random

/**
 * Adaptive placement test. The levels and the rules for moving between them come from
 * assets/config.json; the test starts at the first level and moves up after a run of correct answers.
 */
object Placement {

    val levels: List<PlacementLevel> get() = Config.placementLevels

    private fun units(level: Int): List<CourseUnit> = Course.units.filter { it.id in levels[level].units }

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
