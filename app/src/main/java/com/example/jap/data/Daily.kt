package com.example.jap.data

import kotlin.random.Random

data class Quest(val title: String, val current: Int, val target: Int) {
    val done get() = current >= target
}

/** Everything that renews at midnight: the day's quests and one challenge, chosen from the date. */
object Daily {

    fun quests(progress: Progress): List<Quest> {
        val pool = Config.quests
        // Seeding with the day keeps the same quests all day and changes them tomorrow.
        return pool.indices.shuffled(Random(progress.day)).take(Config.questsPerDay).sorted().map { i ->
            val rule = pool[i]
            val target = rule.target ?: progress.goal
            val current = when (rule.metric) {
                QuestMetric.XP -> progress.todayXp
                QuestMetric.LESSONS -> progress.lessonsToday
                QuestMetric.PERFECT -> progress.perfectToday
                QuestMetric.PRACTICE -> progress.practiceToday
                QuestMetric.CHALLENGE -> if (progress.challengeDoneToday) 1 else 0
            }
            Quest(rule.title.replace("{target}", "$target"), current, target)
        }
    }

    /**
     * Today's challenge, pitched at the learner: what they have studied once there is enough of it,
     * otherwise the unit they are working on.
     */
    fun challenge(progress: Progress): List<Exercise> {
        val learned = progress.srs.keys.mapNotNull(Course::item)
        val pool = if (learned.size >= Config.challengeMinLearned) learned else {
            val next = Course.lessons.firstOrNull { it.id !in progress.done } ?: Course.lessons.last()
            Course.unitOf(next).lessons.flatMap { it.items }.distinctBy { it.id }
        }
        return Exercises.forReview(pool.shuffled(Random(progress.day)).take(Config.challengeSize))
    }
}
