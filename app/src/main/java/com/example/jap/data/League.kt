package com.example.jap.data

import kotlin.random.Random

data class Rival(val name: String, val xp: Int, val you: Boolean = false)

/**
 * Weekly points table. Until accounts are online there is nobody real to rank against,
 * so the other rows are the sample learners listed in assets/config.json, with scores
 * derived from the week number. Swap [table] for a server call when a backend exists.
 */
object League {

    /**
     * Rows sorted best first. Each sample learner studies a different amount each day,
     * so places change from one day to the next.
     */
    fun table(week: Long, weekDay: Int, you: String, yourXp: Int): List<Rival> {
        val others = Config.leagueSample.mapIndexed { i, name ->
            val pace = Random(week * 31 + i).nextInt(Config.leagueMinXp, Config.leagueMaxXp) / 7
            val earned = (0..weekDay).sumOf { day -> Random(week * 997 + i * 31 + day).nextInt(0, pace * 2 + 1) }
            Rival(name, earned)
        }
        return (others + Rival(you, yourXp, you = true)).sortedByDescending { it.xp }
    }
}
