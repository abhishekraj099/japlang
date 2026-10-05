package com.example.jap.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Assessment
import com.example.jap.data.Config
import com.example.jap.data.Pronunciation
import com.example.jap.data.Progress
import com.example.jap.data.Ruby
import com.example.jap.data.SessionKind
import com.example.jap.data.Video
import com.example.jap.data.VideoLine
import com.example.jap.data.Videos

private val Strip = Color(0xFF262B3A)
private val Good = Color(0xFF3F9B5A)
private val Fair = Color(0xFFE0A100)
private val Poor = Color(0xFFD9573F)
private const val GREAT = 80
private const val FAIR = 60

private fun scoreColour(score: Int) = if (score >= GREAT) Good else if (score >= FAIR) Fair else Poor
private fun verdict(score: Int) = if (score >= GREAT) "Great job" else if (score >= FAIR) "Good effort" else "Keep practising"

/**
 * One video: the YouTube clip on top, then either the subtitles (which follow playback and can be
 * tapped to jump) or speaking practice, where each line is said aloud and scored sound by sound.
 */
@Composable
fun VideoPlayerScreen(video: Video, progress: Progress, startSpeaking: Boolean, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val state = rememberClipPlayer(video)
    var tab by rememberSaveable { mutableIntStateOf(if (startSpeaking) 1 else 0) }
    var reporting by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val results = remember(video.id) { mutableStateMapOf<Int, Assessment>() }
    val speakable = video.speakable
    val unfinished = results.isNotEmpty() && speakable.any { progress.bestScore(video.id, it.index) == null }

    LaunchedEffect(video.id) { progress.watch(video.id) }
    BackHandler { if (unfinished) leaving = true else onBack() }

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (unfinished) leaving = true else onBack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
                }
                Text(
                    video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { reporting = true }) { Icon(Icons.Rounded.ErrorOutline, contentDescription = "Report an error", tint = scheme.onBackground) }
            }

            // YouTube always draws inside a 16:9 area. For a vertical Short the player is made wider
            // than the screen and clipped, so the picture fills the frame's height.
            BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(if (video.short) 0.9f else 16f / 9f).clipToBounds().background(Color.Black)) {
                if (video.short) YouTubeClip(state, Modifier.align(Alignment.Center).requiredSize(maxHeight * 16f / 9f, maxHeight))
                else YouTubeClip(state, Modifier.fillMaxSize())
                ClipControls(state, Modifier.matchParentSize())
                state.error?.let {
                    Box(Modifier.matchParentSize().background(Color.Black).padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "This video can't play right now. Check your connection and try again.",
                            color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth().background(Strip).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LevelPill(video.level, onDark = true)
                listOfNotNull("JLPT ${video.jlpt}", Videos.topic(video.topic)?.name, video.channel).forEach {
                    Text("  •  $it", color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth().background(Strip)) {
                listOf("Subtitle", "Speaking").forEachIndexed { i, label ->
                    val on = tab == i
                    Column(Modifier.weight(1f).clickable(role = Role.Tab) { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label, color = if (on) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, fontSize = 17.sp, modifier = Modifier.padding(vertical = 13.dp)
                        )
                        Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) scheme.secondary else Color.Transparent))
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                if (tab == 0) SubtitlePane(video, state)
                else SpeakingPane(video, state, progress, results)
            }
        }
    }

    if (reporting) ReportSheet(onDismiss = { reporting = false }, reasons = Videos.source.reportReasons()) { text ->
        progress.report("video-${video.id}@${state.second.toInt()}", text)
        reporting = false
    }
    if (leaving) LeaveSheet(
        onStay = { leaving = false },
        onLeave = { leaving = false; onBack() },
        body = "You haven't said every sentence yet. Your best scores so far are kept.",
        leave = "Exit video"
    )
}

/**
 * Covers the YouTube player and takes every tap, so nothing inside the embed (logo, title, links)
 * can open the YouTube app. Tapping plays or pauses; a bar shows how far into the clip it is.
 */
@Composable
private fun ClipControls(state: ClipPlayer, modifier: Modifier) {
    val video = state.video
    val share = ((state.second - video.start) / (video.end - video.start)).toFloat().coerceIn(0f, 1f)
    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClickLabel = if (state.playing) "Pause" else "Play"
        ) { state.toggle() }
    ) {
        if (!state.playing) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
            Box(
                Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.ended) Icons.Rounded.Replay else Icons.Rounded.PlayArrow,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp)
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${clock((state.second - video.start).toInt().coerceAtLeast(0))} / ${clock(video.seconds)}",
                color = Color.White, style = MaterialTheme.typography.labelMedium
            )
            Box(Modifier.padding(start = 10.dp).weight(1f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                Box(Modifier.fillMaxWidth(share).height(4.dp).background(Color.White))
            }
        }
    }
}

@Composable
private fun SubtitlePane(video: Video, state: ClipPlayer) {
    val scheme = MaterialTheme.colorScheme
    var furigana by rememberSaveable { mutableStateOf(true) }
    var romaji by rememberSaveable { mutableStateOf(false) }
    var english by rememberSaveable { mutableStateOf(true) }
    val list = rememberLazyListState()
    val current = video.lines.indexOfLast { it.start <= state.second + 0.05f }

    LaunchedEffect(current) {
        if (current >= 0 && state.playing) list.animateScrollToItem((current - 1).coerceAtLeast(0) + 1)
    }

    LazyColumn(state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Show", fontWeight = FontWeight.SemiBold, color = scheme.onBackground, modifier = Modifier.weight(1f))
                Toggle("Furigana", furigana) { furigana = it }
                Toggle("Romaji", romaji) { romaji = it }
                Toggle("English", english) { english = it }
            }
        }
        itemsIndexed(video.lines, key = { _, l -> l.index }) { i, line ->
            val on = i == current
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (on) scheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
                    .clickable { state.seek(line.start) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    clock((line.start - video.start).toInt().coerceAtLeast(0)),
                    color = if (on) scheme.primary else scheme.onSurfaceVariant,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(62.dp).padding(top = if (furigana) 12.dp else 2.dp)
                )
                Column(Modifier.weight(1f)) {
                    RubyLine(line.words, furigana, if (on) scheme.onSurface else scheme.onSurfaceVariant, 20.sp)
                    if (romaji) Text(line.romaji, color = if (on) scheme.secondary else scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    if (english) {
                        if (romaji) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant)
                        Text(line.en, color = if (on) scheme.onSurface else scheme.onSurfaceVariant, modifier = Modifier.padding(top = if (romaji) 0.dp else 4.dp))
                    }
                }
            }
        }
        item {
            Text(
                "Clip from \"${video.source}\" by ${video.channel} on YouTube. Japanese subtitles by the uploader.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            )
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp).clip(CircleShape).background(if (on) scheme.primary else scheme.surfaceVariant)
            .clickable(role = Role.Switch) { onChange(!on) }.padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

/** Japanese with furigana over kanji; words wrap whole. [colours] tints each word, e.g. by score. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RubyLine(words: List<List<Ruby>>, furigana: Boolean, colour: Color, size: TextUnit, colours: List<Color>? = null) {
    FlowRow(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        words.forEachIndexed { w, word ->
            val ink = colours?.getOrNull(w) ?: colour
            Row(Modifier.padding(end = 2.dp)) {
                word.forEach { piece ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (furigana) Text(piece.reading, fontSize = size * 0.5f, lineHeight = size * 0.6f, color = ink.copy(alpha = 0.8f), maxLines = 1)
                        Text(piece.text, fontSize = size, fontWeight = FontWeight.Medium, color = ink, lineHeight = size * 1.25f)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeakingPane(video: Video, state: ClipPlayer, progress: Progress, results: MutableMap<Int, Assessment>) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val speaker = LocalSpeaker.current
    val lines = video.speakable
    var at by rememberSaveable { mutableIntStateOf(0) }
    var translate by rememberSaveable { mutableStateOf(false) }
    var detail by remember { mutableStateOf(false) }
    if (lines.isEmpty()) return
    val line = lines[at.coerceIn(0, lines.lastIndex)]
    val result = results[line.index]

    val speech = rememberSpeechInput(
        onResult = { texts ->
            val a = Pronunciation.assess(line, texts)
            results[line.index] = a
            val finished = progress.scoreLine(video.id, line.index, a.score, lines.map { it.index })
            if (finished) {
                val scores = lines.mapNotNull { progress.bestScore(video.id, it.index) }
                progress.finish(
                    "video-${video.id}", Config.practiceXp, emptyMap(), SessionKind.PRACTICE,
                    perfect = scores.all { it >= GREAT }, answered = scores.size, correct = scores.count { it >= FAIR }
                )
                Toast.makeText(context, "Every sentence spoken! +${Config.practiceXp} XP", Toast.LENGTH_LONG).show()
            }
        },
        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { at-- }, enabled = at > 0 && !speech.listening) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous sentence", modifier = Modifier.size(34.dp))
            }
            Text("Sentence ${at + 1}/${lines.size}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { at++ }, enabled = at < lines.lastIndex && !speech.listening) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next sentence", modifier = Modifier.size(34.dp))
            }
        }

        Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(scheme.surfaceVariant).padding(20.dp)) {
            if (result != null) {
                Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(result.score, 78.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(verdict(result.score), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                        Text(
                            "View pronunciation detail", color = scheme.primary, textDecoration = TextDecoration.Underline,
                            modifier = Modifier.padding(top = 4.dp).clickable { detail = true }
                        )
                    }
                }
            }
            RubyLine(line.words, true, scheme.onSurface, 24.sp, result?.wordOk?.map { if (it) Good else Poor })
            Text(line.romaji, color = scheme.secondary, modifier = Modifier.padding(top = 6.dp))
            if (translate) Text(line.en, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.playLine(line) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Play this line from the video", tint = scheme.onSurfaceVariant) }
                Box(Modifier.width(1.dp).height(22.dp).background(scheme.outlineVariant))
                IconButton(onClick = { translate = !translate }) {
                    Icon(Icons.Rounded.Translate, contentDescription = "Show translation", tint = if (translate) scheme.primary else scheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                progress.bestScore(video.id, line.index)?.let {
                    Text("Best $it", style = MaterialTheme.typography.labelLarge, color = scoreColour(it))
                }
            }
        }

        Text(
            if (speech.listening) "Listening… tap to stop" else if (result == null) "Tap the mic and say the sentence" else "Tap the mic to try again",
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp)
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundAction(Icons.Rounded.RecordVoiceOver, "Hear it slowly", enabled = !speech.listening) { state.pause(); speaker.speak(line.text) }
            MicButton(speech.listening, speech.level) {
                if (speech.listening) speech.stop() else { state.pause(); speech.start(line.text) }
            }
            RoundAction(Icons.AutoMirrored.Rounded.ArrowForward, "Next sentence", enabled = at < lines.lastIndex && !speech.listening) { at++ }
        }
    }

    if (detail && result != null) DetailSheet(line, result, onPlay = { state.playLine(line) }, onClose = { detail = false })
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(62.dp).clip(CircleShape).background(scheme.surfaceVariant).clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = label, tint = if (enabled) scheme.onSurface else scheme.outline) }
}

@Composable
private fun MicButton(listening: Boolean, level: Float, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val wave = rememberInfiniteTransition(label = "mic")
    val phase by wave.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "phase")
    Box(
        Modifier.size(92.dp).clip(CircleShape)
            .background(
                if (listening) Brush.linearGradient(listOf(Fair, Color(0xFFF2B53A)))
                else Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
            )
            .clickable(role = Role.Button, onClickLabel = if (listening) "Stop" else "Speak", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (listening) {
            Canvas(Modifier.size(44.dp)) {
                val bars = 5
                val gap = size.width / (bars * 2 - 1)
                repeat(bars) { i ->
                    val swing = if (i % 2 == 0) phase else 1f - phase
                    val h = size.height * (0.3f + 0.7f * (0.35f * swing + 0.65f * level).coerceIn(0f, 1f))
                    drawLine(Color.White, Offset(gap * (i * 2 + 0.5f), (size.height - h) / 2), Offset(gap * (i * 2 + 0.5f), (size.height + h) / 2), gap, StrokeCap.Round)
                }
            }
        } else Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun ScoreRing(score: Int, size: Dp) {
    val scheme = MaterialTheme.colorScheme
    val colour = scoreColour(score)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = this.size.minDimension * 0.09f
            val box = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(scheme.outlineVariant, 0f, 360f, false, Offset(stroke / 2, stroke / 2), box, style = Stroke(stroke))
            drawArc(colour, -90f, 360f * score / 100f, false, Offset(stroke / 2, stroke / 2), box, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("$score", color = colour, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailSheet(line: VideoLine, result: Assessment, onPlay: () -> Unit, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = scheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pronunciation detail", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
            Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(result.score, 104.dp)
                Text(verdict(result.score), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(start = 20.dp))
            }
            Dashed()
            Heard("Sample", line.text, Good, onPlay)
            Heard("You said", result.heard, scheme.onSurface) { speaker.speak(result.heard) }
            Dashed()
            Text("Needs more practice", color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
            if (result.issues.isEmpty()) {
                Text("Every sound matched. Keep it just like that!", color = Good, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            }
            result.issues.forEach { issue ->
                Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(scheme.surfaceVariant).padding(16.dp)) {
                    Text(issue.word, color = Poor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    issue.notes.forEach { Text(it, color = Poor, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
            Text(
                "Scores compare the sounds the recogniser heard with the subtitle. Pitch accent isn't scored yet.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )
        }
    }
}

@Composable
private fun Heard(label: String, text: String, colour: Color, onPlay: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        IconButton(onClick = onPlay) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Play $label", tint = scheme.onSurface) }
        Column(Modifier.padding(start = 6.dp, top = 4.dp)) {
            Text(label, color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            Text(text.ifEmpty { "—" }, color = colour, fontSize = 19.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun Dashed() {
    val colour = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(colour, Offset.Zero, Offset(size.width, 0f), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
    }
}
