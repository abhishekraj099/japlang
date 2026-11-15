package com.example.jap.data

/**
 * How much review to offer today. A learner never faces more than [Config.reviewDailyCap] reviews a day for each
 * skill; when more than that is due, catch-up mode takes the weakest and longest-overdue items first and spreads
 * the rest over the next days.
 */
object Review {

    /**
     * [today] is what to review now, best first. [backlog] is everything due. [daysToClear] counts today.
     * [welcomeBack] is set when the backlog built up over a break of [Config.reviewBreakDays] days or more.
     */
    class Plan(
        val today: List<Item>,
        val backlog: Int,
        val catchUp: Boolean,
        val welcomeBack: Boolean,
        val daysToClear: Int,
        val capReached: Boolean
    )

    /** [due] pairs each due item with its schedule; [doneToday] were already reviewed today; [daysAway] since the last study day. */
    fun plan(due: List<Pair<Item, Srs>>, doneToday: Int, daysAway: Int, now: Long = System.currentTimeMillis()): Plan {
        val cap = Config.reviewDailyCap
        val left = (cap - doneToday).coerceAtLeast(0)
        // Most likely to be forgotten first: lowest predicted recall, then weakest level, then longest overdue.
        val ordered = due.sortedWith(compareBy({ Memory.retrievability(it.second, now) }, { it.second.box }, { it.second.due })).map { it.first }
        val catchUp = due.size > cap
        val after = (due.size - left).coerceAtLeast(0)
        return Plan(
            today = ordered.take(left),
            backlog = due.size,
            catchUp = catchUp,
            welcomeBack = catchUp && daysAway >= Config.reviewBreakDays,
            daysToClear = if (after == 0) 1 else 1 + (after + cap - 1) / cap,
            capReached = left == 0 && due.isNotEmpty()
        )
    }

    /**
     * What one practice session asks. [due] picks today's due items (weakest first, per [plan]); otherwise the learned
     * items the learner is least likely to recall. Shared by the Practice buttons and "Keep going" so both choose alike.
     */
    fun pick(due: Boolean, plan: Plan, srs: Map<String, Srs>, size: Int, now: Long = System.currentTimeMillis()): List<Item> =
        if (due) plan.today.take(size)
        else srs.entries.sortedBy { Memory.retrievability(it.value, now) }.mapNotNull { Course.item(it.key) }.filter(Course::quizzable).take(size)

    /** [memories] without grammar points: what counts as words and characters, for the daily cap, badges and memory strength. */
    fun <V> itemsOnly(memories: Map<String, V>): Map<String, V> = memories.filterKeys { Course.grammarPoint(it) == null }

    /** Grammar points due by [now], most overdue first; item and unknown ids are left out. */
    fun dueGrammar(srs: Map<String, Srs>, now: Long = System.currentTimeMillis()): List<GrammarPoint> =
        srs.entries.filter { it.value.due <= now }.sortedBy { it.value.due }.mapNotNull { Course.grammarPoint(it.key) }
}
