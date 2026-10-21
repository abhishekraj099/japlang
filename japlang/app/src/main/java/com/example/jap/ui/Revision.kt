package com.example.jap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Memory
import com.example.jap.data.Progress
import com.example.jap.data.Srs
import com.example.jap.ui.theme.Kin

/** The combo multiplier after [run] right answers in a row: ×1, then ×2, ×3, ×4 at each step in config.json. */
fun comboMultiplier(run: Int): Int = 1 + Config.comboSteps.count { run >= it }

/** One colour per mastery level, so strength reads the same everywhere. */
fun levelColour(level: Memory.Level): Color = when (level) {
    Memory.Level.NEW -> Color(0xFF9E9AAE)
    Memory.Level.LEARNING -> Color(0xFFE8833A)
    Memory.Level.FAMILIAR -> Color(0xFFE2B33C)
    Memory.Level.STRONG -> Color(0xFF4FA36B)
    Memory.Level.MASTERED -> Color(0xFF4B7BE5)
}

/**
 * The combo pill during a revision session: a flame, the multiplier and the run, popping each time it grows.
 * It appears from the second answer in a row.
 */
@Composable
fun ComboMeter(run: Int, bonus: Int, modifier: Modifier = Modifier) {
    val multiplier = comboMultiplier(run)
    val pop = remember { Animatable(1f) }
    LaunchedEffect(run) {
        if (run >= 2) {
            pop.snapTo(1.25f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    AnimatedVisibility(run >= 2, modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        val hot = if (multiplier >= 3) Color(0xFFE5482F) else Color(0xFFF08A24)
        Row(
            Modifier
                .scale(pop.value)
                .clip(CircleShape)
                .background(hot.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$run in a row, combo times $multiplier, $bonus bonus XP" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = hot, modifier = Modifier.size(20.dp))
            Text("×$multiplier", fontWeight = FontWeight.ExtraBold, color = hot, modifier = Modifier.padding(start = 4.dp))
            Text("  $run in a row", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The end of a revision session: XP with the combo bonus, accuracy and best combo, then what happened to each memory,
 * with the items that moved up a level first.
 */
@Composable
fun RevisionSummary(
    session: Session,
    progress: Progress,
    results: Map<String, Memory.Grade>,
    writeResults: Map<String, Memory.Grade>,
    gained: Int,
    comboBonus: Int,
    graded: Int,
    right: Int,
    bestRun: Int,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accuracy = if (graded == 0) 100 else right * 100 / graded
    val writing = session.review == "writing"
    val grades = if (writing) writeResults else results
    val memories = if (writing) progress.writeSrs else progress.srs
    // The change each answer makes, worked out before the progress is saved.
    val now = remember { System.currentTimeMillis() }
    val changes = remember(grades) {
        grades.mapNotNull { (id, grade) ->
            val item = Course.item(id) ?: return@mapNotNull null
            val before = memories[id]
            Change(item.jp, item.label, before, Memory.next(before, grade, now), grade)
        }.sortedByDescending { it.after.stability / (it.before?.stability ?: 0.1f) }
    }
    val stronger = changes.count { it.grew }
    val slipped = changes.count { it.grade == Memory.Grade.AGAIN }
    // Reviewing long before an item is due adds little: memory grows most when recall has started to fade.
    val early = changes.isNotEmpty() && changes.all { c -> c.before != null && Memory.retrievability(c.before, now) > 0.97f }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Seal("復習", 104.dp)
            Text(
                if (accuracy >= 90) "Brilliant revision!" else if (accuracy >= 70) "Revision complete!" else "Good effort!",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = scheme.onBackground,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                (if (stronger == 0 && early) "Your memories are still fresh"
                else "$stronger ${if (stronger == 1) "memory" else "memories"} got stronger") + if (slipped > 0) " · $slipped to practise again" else "",
                color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            if (early) {
                Text(
                    "Tip: these were still fresh. Memories grow most when you review them as they start to fade, so come back when they're due.",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(scheme.surfaceVariant)
                        .padding(12.dp).padding(bottom = 0.dp)
                )
                Spacer(Modifier.height(14.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Rounded.Star, Kin, "+$gained", if (comboBonus > 0) "XP · +$comboBonus combo" else "XP earned", Modifier.weight(1f))
                StatCard(Icons.Rounded.TrackChanges, scheme.tertiary, "$accuracy%", "Accuracy", Modifier.weight(1f))
                StatCard(Icons.Rounded.LocalFireDepartment, Color(0xFFE5482F), "$bestRun", "Best combo", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = scheme.tertiary)
                Text("Your memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground, modifier = Modifier.padding(start = 8.dp))
            }
            changes.take(10).forEach { c ->
                val before = Memory.level(c.before)
                val after = Memory.level(c.after)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(14.dp))
                        .background(scheme.surface).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.jp, fontSize = 20.sp, color = scheme.onSurface)
                        Text(c.meaning, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (after != before) {
                                LevelChip(before, faded = true)
                                Text("→", color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
                            }
                            LevelChip(after)
                        }
                        // How long the memory now lasts before recall falls to 90%: the number the review moved.
                        Text(
                            when {
                                c.grade == Memory.Grade.AGAIN -> "practise again soon"
                                c.grew -> "lasts ${days(c.before?.stability)} → ${days(c.after.stability)}"
                                else -> "still fresh · lasts ${days(c.after.stability)}"
                            },
                            fontSize = 12.sp,
                            color = if (c.grade == Memory.Grade.AGAIN) MaterialTheme.colorScheme.error else scheme.tertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            if (changes.size > 10) {
                Text("and ${changes.size - 10} more", color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        PushButton("Continue", onContinue, color = session.color)
    }
}

/** What one answer did to one memory. */
private class Change(val jp: String, val meaning: String, val before: Srs?, val after: Srs, val grade: Memory.Grade) {
    /** Grew by a meaningful amount: at least 5% longer-lasting, or newly learned. */
    val grew: Boolean get() = grade != Memory.Grade.AGAIN && after.stability >= (before?.stability ?: 0f) * 1.05f
}

/** A memory's lifetime in words: "new", "18 h", "4 days", "2 months". */
private fun days(stability: Float?): String = when {
    stability == null || stability <= 0f -> "new"
    stability < 1f -> "${(stability * 24).toInt().coerceAtLeast(1)} h"
    // One decimal under ten days, so small gains from a review still show.
    stability < 10f -> "${"%.1f".format(java.util.Locale.US, stability)} days"
    stability < 60f -> "${stability.toInt()} days"
    else -> "${(stability / 30).toInt()} months"
}

@Composable
fun LevelChip(level: Memory.Level, faded: Boolean = false) {
    val colour = levelColour(level)
    Text(
        level.label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (faded) colour.copy(alpha = 0.75f) else colour,
        modifier = Modifier.clip(CircleShape).background(colour.copy(alpha = if (faded) 0.08f else 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/**
 * How strong all of the learner's memories are: one bar split by mastery level, with a count for each below it.
 */
@Composable
fun MemoryStrength(memories: Map<String, Srs>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val counts = Memory.Level.entries.drop(1).associateWith { level -> memories.values.count { Memory.level(it) == level } } +
        (Memory.Level.NEW to memories.values.count { Memory.level(it) == Memory.Level.NEW })
    val total = memories.size.coerceAtLeast(1)
    val order = listOf(Memory.Level.MASTERED, Memory.Level.STRONG, Memory.Level.FAMILIAR, Memory.Level.LEARNING, Memory.Level.NEW)
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(scheme.surfaceVariant)
                .semantics { contentDescription = order.joinToString { "${counts[it]} ${it.label}" } }
        ) {
            order.forEach { level ->
                val n = counts[level] ?: 0
                if (n > 0) Box(Modifier.weight(n.toFloat() / total).height(12.dp).background(levelColour(level)))
            }
            val rest = total - counts.values.sum()
            if (rest > 0) Spacer(Modifier.weight(rest.toFloat() / total))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            order.filter { it != Memory.Level.NEW }.forEach { level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(levelColour(level)))
                        Text("${counts[level] ?: 0}", fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(start = 5.dp))
                    }
                    Text(level.label, fontSize = 12.sp, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** A gap of [width] between pieces, for rows built from the parts above. */
@Composable
fun Gap(width: androidx.compose.ui.unit.Dp) = Spacer(Modifier.width(width))
