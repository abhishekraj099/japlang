package com.example.jap.data

/**
 * The learner's tree. Studying fills a watering can once a day; watering makes the tree grow through its stages.
 * Days without study dry it out: leaves yellow, fall, and after a long break the tree withers. Studying again
 * brings the leaves back over a few days. Days saved by an omamori count as cared for.
 *
 * Only the rules live here, so the drawing can change freely; the numbers come from assets/config.json.
 */
object Tree {

    /** Waterings to reach each are in config.json; with one a day they arrive at days 1, 7, 30, 100 and 365. */
    enum class Stage { SPROUT, SAPLING, YOUNG, GROWN, BLOSSOM, GREAT }

    enum class Condition {
        /** Cared for today. */
        HEALTHY,
        /** Cared for yesterday; today is still open. */
        THIRSTY,
        /** One day missed: leaves turning and falling. */
        DRYING,
        /** Several days missed: no leaves left. */
        BARE,
        /** A long break: grey and dry, but it comes back. */
        WITHERED
    }

    /**
     * [leaves] is the share of the crown in leaf, 0 to 1. [run] is days in a row of care up to today or yesterday.
     * [waterings] counts every watering; [toNextStage] is how many more it takes to grow, or 0 at the last stage.
     */
    class State(
        val stage: Stage,
        val condition: Condition,
        val leaves: Float,
        val run: Int,
        val daysMissed: Int,
        val canWater: Boolean,
        val wateredToday: Boolean,
        val waterings: Int,
        val toNextStage: Int
    )

    fun stageFor(waterings: Int): Stage {
        val reached = Config.treeStages.indexOfLast { waterings >= it }.coerceAtLeast(0)
        return Stage.entries[reached.coerceAtMost(Stage.entries.lastIndex)]
    }

    /**
     * The tree on [today], from the days studied, the days an omamori saved and the days the tree was watered.
     * A brand-new learner has a healthy sprout that is waiting for its first watering.
     */
    fun state(today: Long, studied: Set<Long>, saved: Set<Long>, watered: Set<Long>): State {
        val cared = studied + saved
        val waterings = watered.size
        val stage = stageFor(waterings)
        val next = Config.treeStages.getOrNull(stage.ordinal + 1)
        val last = cared.filter { it <= today }.maxOrNull()
        val gap = if (last == null) -1 else (today - last).toInt()
        // Days in a row of care ending at the last cared day.
        var run = 0
        if (last != null) while ((last - run) in cared) run++
        val missed = if (gap <= 1) 0 else gap - 1
        val condition = when {
            last == null -> Condition.THIRSTY
            gap == 0 -> Condition.HEALTHY
            gap == 1 -> Condition.THIRSTY
            missed < Config.treeBareAfter -> Condition.DRYING
            missed < Config.treeWitherAfter -> Condition.BARE
            else -> Condition.WITHERED
        }
        // A dry spell is a gap before the current run of care; a learner who never missed a day has a full crown.
        val afterDrySpell = last != null && cared.any { it < last - run + 1 }
        val leaves = when (condition) {
            // Leaves come back over the first days of care after a dry spell.
            Condition.HEALTHY, Condition.THIRSTY ->
                if (!afterDrySpell) 1f
                else (Config.treeRegrowStart + (1f - Config.treeRegrowStart) * (run - 1) / (Config.treeRegrowDays - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
            Condition.DRYING -> (1f - missed.toFloat() / Config.treeBareAfter).coerceIn(0.15f, 0.7f)
            Condition.BARE, Condition.WITHERED -> 0f
        }
        val studiedToday = today in studied
        val wateredToday = today in watered
        return State(
            stage = stage,
            condition = condition,
            leaves = leaves,
            run = if (gap in 0..1) run else 0,
            daysMissed = missed,
            canWater = studiedToday && !wateredToday,
            wateredToday = wateredToday,
            waterings = waterings,
            toNextStage = next?.let { it - waterings } ?: 0
        )
    }
}
