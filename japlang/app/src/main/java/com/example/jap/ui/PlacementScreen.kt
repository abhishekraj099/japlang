package com.example.jap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Analytics
import com.example.jap.data.Course
import com.example.jap.data.Placement
import com.example.jap.data.Progress
import kotlinx.coroutines.delay

private const val NO_ANSWER = -2
private const val FEEDBACK_MILLIS = 700L

/**
 * Placement test. Questions start at the first level and step up after [Config.placementStepUp] correct
 * answers in a row; [Config.placementMaxMisses] misses at a level end the test there.
 */
@Composable
fun PlacementScreen(progress: Progress, onLeave: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val top = Placement.levels.lastIndex
    val used = remember { mutableSetOf<String>() }
    var state by remember { mutableStateOf(Placement.State()) }

    /** Leaves the test, logging its result with what the learner chose to do with it. */
    fun onDone(choice: String) {
        state.result?.let { cleared ->
            Analytics.log(
                Analytics.PLACEMENT_RESULT,
                "levels_cleared" to cleared, "levels" to Placement.levels.size, "asked" to state.asked, "right" to state.right, "choice" to choice
            )
        }
        onLeave()
    }

    val level = state.level
    var question by remember { mutableStateOf(Placement.question(0, used)) }
    // -1 while waiting for an answer, NO_ANSWER for "I don't know", else the chosen option.
    var picked by remember { mutableIntStateOf(-1) }

    LaunchedEffect(picked) {
        if (picked == -1) return@LaunchedEffect
        delay(FEEDBACK_MILLIS)
        state = Placement.answer(state, picked == question.answer)
        if (state.result == null) {
            question = Placement.question(state.level, used)
            picked = -1
        }
    }

    // Levels cleared, once the test is over.
    val cleared = state.result
    SubScreen("Find your level", onBack = { onDone("left") }) {
        if (cleared == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Level ${level + 1} of ${top + 1} · ${Placement.levels[level].name}",
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Question ${state.asked}", color = scheme.onSurfaceVariant)
                }
                Meter((level + state.run / Config.placementStepUp.toFloat()) / (top + 1), scheme.primary, height = 8.dp)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        question.instruction,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (question.sub.isNotEmpty()) {
                        Text(question.sub, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                    }
                    Text(
                        question.prompt,
                        fontSize = if (question.prompt.length <= 2) 80.sp else if (question.prompt.length <= 8) 40.sp else 26.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    question.options.forEachIndexed { i, option ->
                        val state = when {
                            picked == -1 -> OptionState.IDLE
                            i == question.answer -> OptionState.CORRECT
                            i == picked -> OptionState.WRONG
                            else -> OptionState.DIM
                        }
                        OptionCard(
                            option, state, { picked = i },
                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            enabled = picked == -1,
                            big = question.bigOptions
                        )
                    }
                }
                TextButton(
                    onClick = { picked = NO_ANSWER },
                    enabled = picked == -1,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
                ) { Text("I don't know this one") }
            }
        } else {
            val units = Placement.unitsCleared(cleared)
            // Clearing every level means the whole main course is known; the units after it belong to other courses.
            val start = Course.units.getOrNull(units)?.takeIf { cleared < Placement.levels.size }
            // Numbered the way the Learn tab numbers them: the alphabet has no number.
            val skipped = Course.units.take(units)
            val parts = skipped.filter { it.alphabet }.map { it.title } +
                listOfNotNull(skipped.count { !it.alphabet }.takeIf { it > 0 }?.let { if (it == 1) "Unit 1" else "Units 1–$it" })
            val known = if (parts.size <= 1) parts.joinToString() else parts.dropLast(1).joinToString(", ") + " and " + parts.last()
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(
                        when {
                            start == null -> "You know everything in the course so far"
                            units == 0 -> "Let's start from the beginning"
                            start.alphabet -> "You're starting with ${start.title}"
                            else -> "You're starting at Unit ${Course.unitNumber(start)}"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Text(
                        when {
                            start == null -> "Every unit of the main course is marked as known. Use Practice to keep it fresh."
                            units == 0 -> "${start.title}: ${start.subtitle.lowercase()}. Nothing is skipped."
                            else -> "${start.title}: ${start.subtitle.lowercase()}. $known ${if (parts.size == 1 && skipped.size == 1) "is" else "are"} marked as known."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    Text("${state.right} of ${state.asked} correct", fontWeight = FontWeight.Bold, color = scheme.primary)
                    Spacer(Modifier.height(20.dp))
                    Placement.levels.forEachIndexed { i, item ->
                        val passed = i < cleared
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (passed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (passed) "Known" else "To learn",
                                tint = if (passed) scheme.tertiary else scheme.outline,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                item.name,
                                fontSize = 17.sp,
                                color = if (passed) scheme.onBackground else scheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PushButton(
                        "Start learning",
                        onClick = {
                            progress.placeAt(units)
                            onDone("start")
                        }
                    )
                    // One level below the result, for learners who want a gentler start; with one level cleared
                    // that is the beginning, which the button below already offers.
                    if (cleared >= 2) {
                        val lower = Placement.levels[cleared - 1]
                        TextButton(
                            onClick = {
                                progress.placeAt(Placement.unitsCleared(cleared - 1))
                                onDone("one_lower")
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) { Text("Start one level lower (${lower.name})") }
                    }
                    if (units > 0) {
                        TextButton(onClick = { onDone("beginning") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Start from the beginning instead")
                        }
                    }
                }
            }
        }
    }
}
