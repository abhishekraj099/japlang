package com.example.jap.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Config
import com.example.jap.data.Guide
import com.example.jap.data.GuideTalk
import com.example.jap.data.Progress
import com.example.jap.data.SessionKind
import com.example.jap.data.TalkAnswer
import com.example.jap.data.Talks

/**
 * The guide asks short questions aloud in their own voice and the learner answers by speaking. Answers are matched
 * offline; after two misses, or without a microphone, the answers can be tapped instead so a talk always finishes.
 */
@Composable
fun TalkWithGuideScreen(talk: GuideTalk, progress: Progress, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val context = LocalContext.current
    var at by remember { mutableStateOf<String?>(talk.start) }
    // What the learner said to each earlier turn, in order.
    val said = remember { mutableStateListOf<TalkAnswer>() }
    var misses by remember { mutableIntStateOf(0) }
    var tapping by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf(false) }
    // The turn being listened for; a result that arrives after the learner tapped an answer is dropped.
    var listeningFor by remember { mutableStateOf<String?>(null) }
    var romaji by rememberSaveable { mutableStateOf(true) }
    var english by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val turn = talk.turns.firstOrNull { it.id == at }
    // The turns asked so far: the start, then wherever each answer led.
    val byId = remember(talk) { talk.turns.associateBy { it.id } }
    val asked = listOf(byId.getValue(talk.start)) + said.mapNotNull { a -> a.next?.let(byId::get) }

    fun answer(a: TalkAnswer) {
        said += a
        misses = 0
        note = null
        hint = false
        at = a.next
    }

    val speech = rememberSpeechInput(
        onResult = { heard ->
            val t = turn?.takeIf { it.id == listeningFor } ?: return@rememberSpeechInput
            val a = Talks.match(heard, t.answers)
            if (a != null) answer(a) else {
                misses++
                note = "I heard 「${heard.first()}」. Try again!"
            }
        },
        onError = {
            if (turn?.id == listeningFor) {
                misses++
                note = it
            }
        }
    )
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val t = turn
        if (granted && t != null && t.id == listeningFor) speech.start(t.jp.replace(" ", "")) else if (!granted) tapping = true
    }

    fun listen() {
        val t = turn ?: return
        speaker.stop()
        listeningFor = t.id
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) speech.start(t.jp.replace(" ", "")) else mic.launch(Manifest.permission.RECORD_AUDIO)
    }

    // The guide speaks in their own Japanese voice here; lessons get the learner's chosen voice back afterwards.
    DisposableEffect(Guide.current.id) {
        Guide.voiceStyle(Guide.CHARACTER_VOICE + Guide.current.id, "ja", null).let { speaker.useVoice(it.names, it.pitch) }
        onDispose { speaker.useJapaneseVoice(progress) }
    }
    LaunchedEffect(at) {
        if (said.isNotEmpty()) list.animateScrollToItem(asked.size + said.size - 1)
        turn?.let { speaker.speak(it.jp.replace(" ", "")) }
    }
    var recorded by remember { mutableStateOf(false) }
    LaunchedEffect(turn) {
        if (turn == null && !recorded) {
            recorded = true
            progress.finish(talk.id, Config.practiceXp, emptyMap(), SessionKind.PRACTICE)
        }
    }

    SubScreen("Talk with ${Guide.name}", onDone) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            GuideCharacter(
                when {
                    turn == null -> "cheer"
                    misses > 0 -> "think"
                    said.isEmpty() -> "wave"
                    else -> "point"
                },
                72.dp
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(talk.title, style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = romaji, onClick = { romaji = !romaji }, label = { Text("Romaji") })
                    FilterChip(selected = english, onClick = { english = !english }, label = { Text("English") })
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            asked.forEachIndexed { i, t ->
                item { TalkBubble(Guide.name, t.jp, t.romaji.takeIf { romaji }, t.en.takeIf { english }, mine = false, current = t == turn) }
                said.getOrNull(i)?.let { a -> item { TalkBubble("You", a.jp, a.romaji.takeIf { romaji }, a.en.takeIf { english }, mine = true, current = false) } }
            }
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (turn == null) {
                Text("上手！ Great talk.", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
                Text("+${Config.practiceXp} XP", color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                PushButton("Done", onDone)
                return@Column
            }
            note?.let { Text(it, color = scheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)) }
            if (hint) {
                val first = turn.answers.first()
                Text("${first.romaji} · ${first.en}", color = scheme.secondary, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (tapping || misses >= 2) {
                turn.answers.forEach { a ->
                    OptionCard("${a.jp}\n${a.en}", OptionState.IDLE, { answer(a) }, Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { speaker.speak(turn.jp.replace(" ", "")) }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear ${Guide.name} again")
                }
                PushButton(if (speech.listening) "Listening…" else "Speak", ::listen, Modifier.weight(1f), enabled = !speech.listening)
                TextButton(onClick = { hint = true }) { Text("Hint") }
            }
            if (!tapping && misses < 2) TextButton(onClick = { tapping = true }) { Text("Can't speak now? Tap an answer") }
        }
    }
}

@Composable
private fun TalkBubble(who: String, jp: String, romaji: String?, en: String?, mine: Boolean, current: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Text(who, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Panel(
            Modifier.fillMaxWidth(0.86f).padding(top = 4.dp),
            fill = if (mine) scheme.primaryContainer else scheme.surface,
            line = if (current) scheme.primary else scheme.outlineVariant
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(jp, fontSize = 19.sp, color = if (mine) scheme.onPrimaryContainer else scheme.onSurface)
                romaji?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.secondary) }
                en?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (mine) scheme.onPrimaryContainer else scheme.onSurfaceVariant) }
            }
        }
    }
}
