package com.example.jap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.jap.data.Exercises
import com.example.jap.data.Kind
import com.example.jap.data.Progress
import kotlinx.coroutines.delay

private enum class Phase { READY, PLAYING, OVER }

/** A timed round of quick-fire questions on what the learner has studied. */
@Composable
fun GameScreen(progress: Progress, onBack: () -> Unit, script: Kind? = null) {
    val scheme = MaterialTheme.colorScheme
    // Given a script, drill its basic characters; otherwise beginners with little learned yet
    // play with the basic hiragana.
    val pool = remember {
        if (script != null) return@remember Course.kanaChart(script).first().second.flatten().filterNotNull()
        val learned = progress.srs.keys.mapNotNull(Course::item)
        if (learned.size >= Config.gameMinPool) learned
        else Course.allItems.filter { it.kind == Kind.HIRAGANA && it.jp.length == 1 }
    }
    var phase by remember { mutableStateOf(Phase.READY) }
    var left by remember { mutableIntStateOf(Config.gameSeconds) }
    var score by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(Exercises.quick(pool.random(), pool)) }
    var picked by remember { mutableIntStateOf(-1) }

    LaunchedEffect(phase) {
        if (phase != Phase.PLAYING) return@LaunchedEffect
        while (left > 0) {
            delay(1000)
            left--
        }
        progress.recordGame(score)
        progress.finish(null, (score / 2).coerceAtMost(Config.gameMaxXp), emptyMap())
        phase = Phase.OVER
    }

    // Show right or wrong for a moment, then move on.
    LaunchedEffect(picked) {
        if (picked >= 0) {
            delay(350)
            question = Exercises.quick(pool.random(), pool)
            picked = -1
        }
    }

    fun start() {
        left = Config.gameSeconds
        score = 0
        picked = -1
        question = Exercises.quick(pool.random(), pool)
        phase = Phase.PLAYING
    }

    SubScreen("Speed round", onBack) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (phase) {
                Phase.READY, Phase.OVER -> {
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(96.dp))
                        if (phase == Phase.OVER) {
                            Text("$score", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                            Text("correct answers", color = scheme.onSurfaceVariant)
                        } else {
                            Text(
                                "How many can you answer in ${Config.gameSeconds} seconds?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            "Best: ${progress.bestGame}",
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    PushButton(if (phase == Phase.OVER) "Play again" else "Start", ::start)
                }

                Phase.PLAYING -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Meter(left / Config.gameSeconds.toFloat(), scheme.primary, Modifier.weight(1f))
                        Text(
                            "${left}s",
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    Text(
                        "Score $score",
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            question.prompt,
                            fontSize = if (question.prompt.length <= 2) 96.sp else 44.sp,
                            color = scheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                    question.options.chunked(2).forEachIndexed { r, row ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEachIndexed { c, option ->
                                val i = r * 2 + c
                                val state = when {
                                    picked < 0 -> OptionState.IDLE
                                    i == question.answer -> OptionState.CORRECT
                                    i == picked -> OptionState.WRONG
                                    else -> OptionState.DIM
                                }
                                OptionCard(
                                    option, state,
                                    onClick = {
                                        if (i == question.answer) score++
                                        picked = i
                                    },
                                    Modifier.weight(1f).heightIn(min = 72.dp),
                                    enabled = picked < 0
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
