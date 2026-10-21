package com.example.jap.data

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * How well the learner remembers each item, using the FSRS-4.5 memory model (the open-source scheduler Anki uses).
 * Every item keeps a stability (days until the chance of recall falls to 90%) and a difficulty (1 easy to 10 hard).
 * After each answer both are updated from how the learner did, and the item comes back just as recall is predicted
 * to drop to the target retention, so reviews land when they help most. Weights are FSRS-4.5's published defaults.
 */
object Memory {

    /** How an answer went: wrong, right but slow, right, or right and quick. */
    enum class Grade(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

    /** Mastery shown to the learner, from a memory's stability. */
    enum class Level(val label: String) { NEW("New"), LEARNING("Learning"), FAMILIAR("Familiar"), STRONG("Strong"), MASTERED("Mastered") }

    private val w = floatArrayOf(
        0.4872f, 1.4003f, 3.7145f, 13.8206f, 5.1618f, 1.2298f, 0.8975f, 0.031f, 1.6474f,
        0.1367f, 1.0461f, 2.1072f, 0.0793f, 0.3246f, 1.587f, 0.2272f, 2.8755f
    )
    private const val FACTOR = 19f / 81f
    private const val DECAY = -0.5f
    private const val DAY = 86_400_000L

    /** Stabilities (days) where mastery moves up a level: Learning below 7, then Familiar, Strong and Mastered. */
    private val levelAt = floatArrayOf(7f, 21f, 60f)

    /** Chance (0 to 1) the learner still recalls an item [elapsedDays] after the last review. */
    fun retrievability(stability: Float, elapsedDays: Float): Float =
        if (stability <= 0f) 0f else (1f + FACTOR * max(elapsedDays, 0f) / stability).pow(DECAY)

    fun retrievability(srs: Srs, now: Long): Float = retrievability(srs.stability, (now - srs.last).toFloat() / DAY)

    /** Days until recall falls to [retention], for a memory of [stability]. At 90% this equals the stability. */
    fun interval(stability: Float, retention: Float = Config.retention): Float =
        stability / FACTOR * (retention.pow(1f / DECAY) - 1f)

    fun level(srs: Srs?): Level = when {
        srs == null -> Level.NEW
        srs.hits == 0 -> Level.NEW
        srs.stability < levelAt[0] -> Level.LEARNING
        srs.stability < levelAt[1] -> Level.FAMILIAR
        srs.stability < levelAt[2] -> Level.STRONG
        else -> Level.MASTERED
    }

    /** The grade for an answer: right or wrong, and how long the learner took over it. */
    fun grade(correct: Boolean, millis: Long): Grade = when {
        !correct -> Grade.AGAIN
        millis >= Config.slowMillis -> Grade.HARD
        millis <= Config.fastMillis -> Grade.EASY
        else -> Grade.GOOD
    }

    /** The memory after one answer graded [grade] at [now]; [before] is null the first time the item is reviewed. */
    fun next(before: Srs?, grade: Grade, now: Long): Srs {
        val g = grade.value
        val (stability, difficulty) = if (before == null || before.stability <= 0f) {
            w[g - 1] to initialDifficulty(g)
        } else {
            val r = retrievability(before, now)
            val d = before.difficulty.coerceIn(1f, 10f)
            val s = before.stability
            val nextS = if (grade == Grade.AGAIN) {
                min(s, w[11] * d.pow(-w[12]) * ((s + 1f).pow(w[13]) - 1f) * exp(w[14] * (1f - r)))
            } else {
                val hard = if (grade == Grade.HARD) w[15] else 1f
                val easy = if (grade == Grade.EASY) w[16] else 1f
                s * (1f + exp(w[8]) * (11f - d) * s.pow(-w[9]) * (exp(w[10] * (1f - r)) - 1f) * hard * easy)
            }
            // Difficulty moves with the grade, then drifts slightly back towards the default so it cannot get stuck.
            val moved = d - w[6] * (g - 3)
            val nextD = (w[7] * initialDifficulty(4) + (1f - w[7]) * moved).coerceIn(1f, 10f)
            nextS to nextD
        }
        val s = stability.coerceIn(0.1f, 36_500f)
        val hits = (before?.hits ?: 0) + if (grade == Grade.AGAIN) 0 else 1
        val misses = (before?.misses ?: 0) + if (grade == Grade.AGAIN) 1 else 0
        val draft = Srs(0, now + (interval(s) * DAY).toLong().coerceAtLeast(MIN_GAP), s, difficulty, now, hits, misses)
        return draft.copy(box = level(draft).ordinal)
    }

    private fun initialDifficulty(g: Int) = (w[4] - (g - 3) * w[5]).coerceIn(1f, 10f)

    /** An item answered wrong comes back no sooner than this, so it is not repeated straight away. */
    private const val MIN_GAP = 10 * 60_000L

    /**
     * A memory from the old box schedule, kept when the app updates: the box's interval becomes the stability,
     * and the item is due when it was.
     */
    fun fromBox(box: Int, due: Long): Srs {
        val hours = Config.srsIntervals.getOrElse(box) { Config.srsIntervals.lastOrNull() ?: 0L } / 3_600_000f
        val stability = max(hours / 24f, 0.2f)
        val last = due - (interval(stability) * DAY).toLong()
        val draft = Srs(0, due, stability, 5f, last, hits = box, misses = 0)
        return draft.copy(box = level(draft).ordinal)
    }
}
