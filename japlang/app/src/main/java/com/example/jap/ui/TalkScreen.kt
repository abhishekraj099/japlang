package com.example.jap.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Exercises
import com.example.jap.data.Lesson
import com.example.jap.data.LessonType
import com.example.jap.data.Line
import com.example.jap.data.Progress
import com.example.jap.data.SessionKind
import kotlinx.coroutines.delay

/** Rough time the speech engine needs for a line, so the partner's turn can finish before yours. */
private fun speakingMillis(line: Line) = 900L + line.jp.length * 160L

private fun said(line: Line, heard: List<String>): Boolean {
    val targets = (line.heard + line.jp).map(Exercises::normalise)
    return heard.map(Exercises::normalise).any { said -> targets.any { said == it || said.contains(it) } }
}

/** The conversations in the course, to act out by voice. */
@Composable
fun TalkScreen(onScene: (Lesson) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val scenes = Course.conversations
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Talk", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
            Text(
                "Act out real conversations. Listen to your partner, then say your lines aloud.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        items(scenes, key = { it.id }) { scene ->
            val unit = Course.unitOf(scene)
            Panel(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onScene(scene) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(unit.color)).waves(Color.White.copy(alpha = 0.15f), 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(scene.icon, fontFamily = FontFamily.Serif, fontSize = 26.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(scene.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Text(
                            "${scene.dialogue.size} lines · ${scene.dialogue.map { it.speaker }.distinct().joinToString(" & ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Role-play: the learner picks a part, the app speaks the other part, and the learner's own lines
 * are checked by the phone's speech recogniser.
 */
@Composable
fun RolePlayScreen(scene: Lesson, progress: Progress, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val roles = scene.dialogue.map { it.speaker }.distinct()
    var role by remember { mutableStateOf<String?>(null) }
    // Index of the line being played; equal to the line count once the scene is over.
    var at by remember { mutableIntStateOf(0) }
    val passed = remember { mutableStateListOf<Int>() }
    var heard by remember { mutableStateOf<String?>(null) }
    var noMic by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    val recogniser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val texts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (texts.isEmpty()) return@rememberLauncherForActivityResult
        if (said(scene.dialogue[at], texts)) {
            passed += at
            heard = null
            at++
        } else {
            heard = texts.first()
        }
    }

    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, scene.dialogue[at].jp)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        try {
            recogniser.launch(intent)
        } catch (e: ActivityNotFoundException) {
            noMic = true
        }
    }

    val mine = role
    // The partner's lines play by themselves; the learner's line waits for them.
    LaunchedEffect(mine, at) {
        if (mine == null || at >= scene.dialogue.size) return@LaunchedEffect
        list.animateScrollToItem(at)
        val line = scene.dialogue[at]
        speaker.speak(line.jp.replace(" ", ""))
        if (line.speaker != mine) {
            delay(speakingMillis(line))
            at++
        }
    }

    val myLines = scene.dialogue.indices.filter { scene.dialogue[it].speaker == mine }
    val over = mine != null && at >= scene.dialogue.size
    LaunchedEffect(over) {
        if (over && !recorded) {
            recorded = true
            progress.finish(null, Config.practiceXp, emptyMap(), SessionKind.PRACTICE, perfect = passed.size == myLines.size)
        }
    }

    SubScreen(scene.title, onBack) {
        when {
            mine == null -> Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("Choose your part", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
                Text(
                    "We'll play the other part. Say your lines when it's your turn.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                roles.forEach { name ->
                    OptionCard("Play $name", OptionState.IDLE, { role = name }, Modifier.fillMaxWidth().padding(bottom = 10.dp))
                }
            }
            over -> Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Seal("上手", 120.dp)
                Text(
                    "You said ${passed.size} of ${myLines.size} lines",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text("+${Config.practiceXp} XP", color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))
                PushButton("Done", onBack)
                TextButton(onClick = {
                    passed.clear()
                    at = 0
                    role = null
                    recorded = false
                }) { Text("Play again") }
            }
            else -> Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    state = list,
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(scene.dialogue) { i, line ->
                        if (i <= at) Bubble(line, line.speaker == mine, i == at, i in passed)
                    }
                }
                val line = scene.dialogue[at]
                if (line.speaker == mine) {
                    Column(
                        Modifier.fillMaxWidth().background(scheme.surface).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            when {
                                noMic -> "Speech recognition isn't available on this device. Read the line aloud and skip."
                                heard != null -> "We heard: $heard. Listen and try again."
                                else -> "Your turn. Say the line aloud."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (heard != null || noMic) scheme.error else scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { speaker.speak(line.jp.replace(" ", "")) }) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                                Text("Listen", modifier = Modifier.padding(start = 6.dp))
                            }
                            PushButton("Speak", ::listen, Modifier.weight(1f), enabled = !noMic)
                            TextButton(onClick = {
                                heard = null
                                at++
                            }) { Text("Skip") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bubble(line: Line, mine: Boolean, current: Boolean, passed: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(line.speaker, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            if (passed) Icon(Icons.Rounded.Check, contentDescription = "Said", tint = scheme.tertiary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
            if (mine && current) Icon(Icons.Rounded.Mic, contentDescription = null, tint = scheme.secondary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
        Panel(
            Modifier.fillMaxWidth(0.86f).padding(top = 4.dp),
            fill = if (mine) scheme.primaryContainer else scheme.surface,
            line = if (current) scheme.primary else scheme.outlineVariant
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(line.jp, fontSize = 19.sp, color = if (mine) scheme.onPrimaryContainer else scheme.onSurface)
                Text(line.romaji, style = MaterialTheme.typography.bodyMedium, color = scheme.secondary)
                Text(line.en, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}
