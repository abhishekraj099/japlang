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
import com.example.jap.data.Course
import com.example.jap.data.Placement
import com.example.jap.data.Progress
import kotlinx.coroutines.delay

private const val NO_ANSWER = -2
private const val FEEDBACK_MILLIS = 700L

/**
 * Placement test. Questions start at the first level and step up after two correct answers
 * in a row; two misses at a level end the test there.
 */
@Composable
fun PlacementScreen(progress: Progress, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val top = Placement.levels.lastIndex
    val used = remember { mutableSetOf<String>() }
    var level by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var asked by remember { mutableIntStateOf(1) }
    var right by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(Placement.question(0, used)) }
    // -1 while waiting for an answer, NO_ANSWER for "I don't know", else the chosen option.
    var picked by remember { mutableIntStateOf(-1) }
    // Levels cleared, once the test is over.
    var result by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(picked) {
        if (picked == -1) return@LaunchedEffect
        delay(FEEDBACK_MILLIS)
        var passedTop = false
        if (picked == question.answer) {
            right++
            run++
            if (run >= Config.placementStepUp) {
                if (level == top) passedTop = true else {
                    level++
                    run = 0
                    misses = 0
                }
            }
        } else {
            run = 0
            misses++
        }
        when {
            passedTop -> result = top + 1
            misses >= Config.placementMaxMisses || asked >= Config.placementMaxQuestions -> result = level
            else -> {
                asked++
                question = Placement.question(level, used)
                picked = -1
            }
        }
    }

    val cleared = result
    SubScreen("Find your level", onBack = onDone) {
        if (cleared == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Level ${level + 1} of ${top + 1} · ${Placement.levels[level].name}",
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Question $asked", color = scheme.onSurfaceVariant)
                }
                Meter((level + run / Config.placementStepUp.toFloat()) / (top + 1), scheme.primary, height = 8.dp)
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
            val start = Course.units.getOrNull(units)
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(
                        when {
                            start == null -> "You know everything in the course so far"
                            units == 0 -> "Let's start from the beginning"
                            else -> "You're starting at Unit ${units + 1}"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Text(
                        when {
                            start == null -> "Every unit is unlocked. Use Practice to keep it fresh."
                            units == 0 -> "${start.title}: ${start.subtitle.lowercase()}. Nothing is skipped."
                            else -> "${start.title}: ${start.subtitle.lowercase()}. The $units earlier units are marked as known."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    Text("$right of $asked correct", fontWeight = FontWeight.Bold, color = scheme.primary)
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
                            onDone()
                        }
                    )
                    if (units > 0) {
                        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Start from the beginning instead")
                        }
                    }
                }
            }
        }
    }
}
