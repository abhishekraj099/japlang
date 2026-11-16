package com.example.jap.data

enum class Skill {
    READING, LISTENING, WRITING, SPEAKING, GRAMMAR;

    val label: String get() = name.lowercase().replaceFirstChar(Char::uppercase)
}

/**
 * The five skills every graded answer trains, scored over each skill's recent answers so Smart practice can train
 * the weakest one. A rolling window lets a score recover as the learner improves.
 */
object Skills {
    const val WINDOW = 50
    const val MIN_ANSWERS = 10

    fun of(exercise: Exercise): Skill? = when (exercise) {
        is Choice -> when {
            exercise.listen -> Skill.LISTENING
            exercise.instruction == "Fill in the blank" || exercise.itemId?.startsWith("g-") == true -> Skill.GRAMMAR
            else -> Skill.READING
        }
        is Build -> Skill.GRAMMAR
        is Match, is TypeIn -> Skill.READING
        is Write, is Dictate -> Skill.WRITING
        is Speak -> Skill.SPEAKING
        is Teach, is NoteCard, is DialogueCard -> null
    }

    /** Percent right, or null until there are enough answers to mean anything. */
    fun percent(log: List<Boolean>): Int? = if (log.size < MIN_ANSWERS) null else log.count { it } * 100 / log.size

    fun append(log: List<Boolean>, correct: Boolean): List<Boolean> = (log + correct).takeLast(WINDOW)

    /**
     * The skill to train next, never one in [unavailable]. While a skill still has too few answers to be scored, the
     * least-practised one goes first, so every skill gets a score instead of one known skill being trained forever;
     * once all are scored, the lowest percent wins.
     */
    fun pick(logs: Map<Skill, List<Boolean>>, unavailable: Set<Skill>): Skill {
        // Reading always has material once anything is learned, so it is the fallback if everything else is off.
        val open = Skill.entries.filter { it !in unavailable }.ifEmpty { listOf(Skill.READING) }
        val size = { s: Skill -> logs[s].orEmpty().size }
        return if (open.any { size(it) < MIN_ANSWERS }) open.minBy(size)
        else open.minBy { percent(logs.getValue(it))!! }
    }
}
