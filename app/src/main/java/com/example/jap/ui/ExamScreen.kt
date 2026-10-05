package com.example.jap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.jap.data.Exam
import com.example.jap.data.Progress
import kotlinx.coroutines.delay

private enum class ExamPhase { INTRO, RUNNING, RESULT }

@Composable
fun ExamScreen(progress: Progress, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val questions = Exam.questions
    // Option order is fixed for one sitting so the review matches what was shown.
    val options = remember { questions.map { it.options.shuffled() } }
    val picks = remember { mutableStateListOf<Int>().also { list -> repeat(questions.size) { list.add(-1) } } }
    var phase by remember { mutableStateOf(ExamPhase.INTRO) }
    var index by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(Exam.minutes * 60) }
    var confirmLeave by remember { mutableStateOf(false) }

    // Read from state at call time: the timer calls finish() long after this composition.
    fun rightCount() = questions.indices.count { picks[it] >= 0 && options[it][picks[it]] == questions[it].options.first() }
    val right = rightCount()
    val percent = right * 100 / questions.size
    val passed = percent >= Exam.passPercent

    fun finish() {
        val score = rightCount() * 100 / questions.size
        progress.recordExam(score)
        if (score >= Exam.passPercent) progress.finish(null, Config.examPassXp, emptyMap())
        phase = ExamPhase.RESULT
    }

    LaunchedEffect(phase) {
        if (phase != ExamPhase.RUNNING) return@LaunchedEffect
        while (left > 0) {
            delay(1000)
            left--
        }
        if (phase == ExamPhase.RUNNING) finish()
    }

    SubScreen(Exam.title, onBack = { if (phase == ExamPhase.RUNNING) confirmLeave = true else onBack() }) {
        when (phase) {
            ExamPhase.INTRO -> Column(Modifier.fillMaxSize().padding(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Quiz, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(72.dp))
                    Text(
                        "${questions.size} questions · ${Exam.minutes} minutes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Vocabulary, kanji, grammar and reading in the style of JLPT N5. " +
                            "You see the answers and explanations at the end. " +
                            "${Exam.passPercent}% or more is a pass.",
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (progress.bestExam >= 0) {
                        Text(
                            "Your best: ${progress.bestExam}%",
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                PushButton("Start test", onClick = { phase = ExamPhase.RUNNING })
            }

            ExamPhase.RUNNING -> {
                val question = questions[index]
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${question.section} · ${index + 1} / ${questions.size}",
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "%d:%02d".format(left / 60, left % 60),
                            fontWeight = FontWeight.Bold,
                            color = if (left < 60) scheme.error else scheme.onBackground
                        )
                    }
                    Meter((index + 1) / questions.size.toFloat(), scheme.primary, height = 8.dp)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                        if (question.passage.isNotEmpty()) {
                            Panel(Modifier.fillMaxWidth().padding(bottom = 16.dp), fill = scheme.surfaceVariant, line = scheme.outlineVariant) {
                                Text(question.passage, fontSize = 20.sp, color = scheme.onSurface, modifier = Modifier.padding(16.dp))
                            }
                        }
                        Text(
                            question.prompt,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onBackground,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        options[index].forEachIndexed { i, option ->
                            OptionCard(
                                option,
                                if (picks[index] == i) OptionState.SELECTED else OptionState.IDLE,
                                { picks[index] = i },
                                Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            )
                        }
                    }
                    Row(Modifier.padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PushButton(
                            "Back",
                            onClick = { index-- },
                            modifier = Modifier.weight(1f),
                            enabled = index > 0,
                            color = scheme.secondary
                        )
                        if (index < questions.lastIndex) {
                            PushButton("Next", onClick = { index++ }, modifier = Modifier.weight(1f))
                        } else {
                            PushButton("Finish", onClick = ::finish, modifier = Modifier.weight(1f), color = scheme.tertiary)
                        }
                    }
                }
            }

            ExamPhase.RESULT -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$percent%",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (passed) scheme.tertiary else scheme.error
                        )
                        Text(
                            if (passed) "Passed · $right of ${questions.size} correct" else "Not yet · $right of ${questions.size} correct",
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground
                        )
                        Text(
                            "Go through the explanations below.",
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                }
                itemsIndexed(questions) { i, question ->
                    val answer = question.options.first()
                    val mine = options[i].getOrNull(picks[i])
                    val ok = mine == answer
                    Panel(
                        Modifier.fillMaxWidth(),
                        fill = if (ok) scheme.tertiaryContainer else scheme.errorContainer,
                        line = if (ok) scheme.tertiary else scheme.error
                    ) {
                        val ink = if (ok) scheme.onTertiaryContainer else scheme.onErrorContainer
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("${i + 1}. ${question.section}", style = MaterialTheme.typography.labelLarge, color = ink)
                            if (question.passage.isNotEmpty()) Text(question.passage, color = ink)
                            Text(question.prompt, fontWeight = FontWeight.Bold, color = ink, modifier = Modifier.padding(vertical = 4.dp))
                            if (!ok) Text("Your answer: ${mine ?: "none"}", color = ink)
                            Text("Correct answer: $answer", color = ink)
                            Text(question.explain, color = ink, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                item { PushButton("Done", onBack, Modifier.padding(top = 8.dp)) }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the test?") },
            text = { Text("Your answers will be lost.") },
            confirmButton = { TextButton(onClick = onBack) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep going") } }
        )
    }
}
