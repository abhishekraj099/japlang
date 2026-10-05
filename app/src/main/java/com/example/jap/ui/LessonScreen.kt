package com.example.jap.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Build
import com.example.jap.data.Choice
import com.example.jap.data.Config
import com.example.jap.data.CourseUnit
import com.example.jap.data.DialogueCard
import com.example.jap.data.Dictate
import com.example.jap.data.Exercise
import com.example.jap.data.Exercises
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.Match
import com.example.jap.data.NoteCard
import com.example.jap.data.Progress
import com.example.jap.data.SessionKind
import com.example.jap.data.Speak
import com.example.jap.data.Teach
import com.example.jap.data.TypeIn
import com.example.jap.data.Write
import com.example.jap.ink.Handwriting
import com.example.jap.ui.theme.Kin
import com.example.jap.ink.LocalHandwriting
import kotlinx.coroutines.delay

/** One run through a list of exercises. [lessonId] is set when finishing it completes a course lesson. */
class Session(
    val title: String,
    val lessonId: String?,
    val color: Color,
    val baseXp: Int,
    val kind: SessionKind,
    val exercises: List<Exercise>,
    /** Set when this run is a test-out: passing it completes the whole unit. */
    val testOut: CourseUnit? = null
)

private class Verdict(val correct: Boolean, val message: String)

/** What a finished session reports back. */
class Outcome(
    val xp: Int,
    val results: Map<String, Boolean>,
    val perfect: Boolean,
    val answered: Int,
    val correct: Int,
    val bestRun: Int
)

/** How questions are shown, from the lesson settings sheet. */
class Display(val kanji: Boolean, val reading: Boolean, val romaji: Boolean, val title: Boolean, val scale: Float)

val LocalDisplay = staticCompositionLocalOf { Display(true, true, true, true, 1f) }

private val FontScales = listOf(0.8f, 0.9f, 1f, 1.15f, 1.3f, 1.5f)

@Composable
fun LessonScreen(
    session: Session,
    progress: Progress,
    onExit: () -> Unit,
    onFinish: (Outcome) -> Unit
) {
    val speaker = LocalSpeaker.current
    var paused by remember { mutableStateOf(false) }
    var explaining by remember { mutableStateOf(false) }
    var tuning by remember { mutableStateOf(false) }
    speaker.muted = !progress.lessonAudio
    val display = Display(progress.showKanji, progress.showReading, progress.showRomaji, progress.showTitle, FontScales[progress.fontStep.coerceIn(0, 5)])
    val queue = remember { mutableStateListOf<Exercise>().also { it.addAll(session.exercises) } }
    var index by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(queue.isEmpty()) }
    var confirmExit by remember { mutableStateOf(false) }
    var graded by remember { mutableIntStateOf(0) }
    var right by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(0) }
    var bestRun by remember { mutableIntStateOf(0) }
    val results = remember { mutableMapOf<String, Boolean>() }
    val retried = remember { mutableSetOf<Exercise>() }

    val gained = session.baseXp + if (graded == right) Config.perfectBonus else 0

    /** [correct] is null for screens that are not graded. A missed exercise comes back once at the end. */
    fun advance(exercise: Exercise, correct: Boolean?) {
        if (correct != null) {
            graded++
            val id = exercise.itemId
            if (correct) {
                right++
                run++
                if (run > bestRun) bestRun = run
                if (id != null && id !in results) results[id] = true
            } else {
                run = 0
                if (id != null) results[id] = false
                if (retried.add(exercise)) queue.add(exercise)
            }
        }
        if (index + 1 >= queue.size) finished = true else index++
    }

    fun outcome() = Outcome(gained, results, graded == right, graded, right, bestRun)

    BackHandler {
        if (finished) onFinish(outcome()) else confirmExit = true
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (finished) {
                Summary(session, gained, graded, right) { onFinish(outcome()) }
            } else {
                val share by animateFloatAsState(index / queue.size.toFloat(), label = "progress")
                val current = queue[index]
                val hint = (current as? Choice)?.let { it.hint.ifEmpty { it.explain } }.orEmpty()
                Meter(share, session.color, Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp), height = 10.dp)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(Icons.Rounded.Pause, "Pause", MaterialTheme.colorScheme.surfaceVariant) { paused = true }
                    Spacer(Modifier.weight(1f))
                    RoundIcon(Icons.Rounded.Lightbulb, "Explain", Color(0xFFFFF1CC), Color(0xFFE0A100), enabled = hint.isNotEmpty()) { explaining = true }
                    RoundIcon(Icons.Rounded.MoreVert, "Lesson settings", MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(start = 10.dp)) { tuning = true }
                }
                CompositionLocalProvider(LocalDisplay provides display) {
                    key(index) {
                        ExerciseView(current, session.color) { advance(current, it) }
                    }
                }
                if (explaining) ExplainSheet(current, hint, session.color) { explaining = false }
            }
        }
    }

    if (confirmExit || paused) {
        LeaveSheet(onStay = { confirmExit = false; paused = false }, onLeave = onExit)
    }
    if (tuning) DisplaySheet(progress) { tuning = false }
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, fill: Color,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(if (enabled) fill else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = if (enabled) tint else MaterialTheme.colorScheme.outline) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveSheet(
    onStay: () -> Unit,
    onLeave: () -> Unit,
    body: String = "The lesson isn't finished yet. Keep going so you don't lose what you've done.",
    leave: String = "Exit lesson"
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onStay, containerColor = scheme.surface, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SakuraIcon(72.dp, Modifier.padding(top = 8.dp))
            Text("Want to leave now?", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(top = 16.dp))
            Text(
                body,
                color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
            )
            GradientButton("Continue learning", onClick = onStay)
            Text(
                leave,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(CircleShape).background(scheme.surfaceVariant)
                    .clickable(role = Role.Button, onClick = onLeave).padding(vertical = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplainSheet(exercise: Exercise, hint: String, accent: Color, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val word = (exercise as? Choice)?.let { if (it.listen) "" else it.prompt }.orEmpty()
    ModalBottomSheet(onDismissRequest = onClose, containerColor = scheme.surface, dragHandle = null) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Explain", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
            Column(Modifier.padding(vertical = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(scheme.surfaceVariant).padding(18.dp)) {
                if (word.isNotEmpty()) {
                    Text(word, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = scheme.outlineVariant)
                }
                Text(hint, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            }
            GradientButton("Got it", onClick = onClose)
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** What to show in questions, audio, and text size. */
@Composable
private fun DisplaySheet(progress: Progress, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var kanji by remember { mutableStateOf(progress.showKanji) }
    var reading by remember { mutableStateOf(progress.showReading) }
    var romaji by remember { mutableStateOf(progress.showRomaji) }
    var title by remember { mutableStateOf(progress.showTitle) }
    var audio by remember { mutableStateOf(progress.lessonAudio) }
    var font by remember { mutableIntStateOf(progress.fontStep) }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(scheme.surface).padding(22.dp)) {
            Text("Show", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            ToggleRow("Japanese (私)", kanji) { kanji = it }
            ToggleRow("Reading (わたし)", reading) { reading = it }
            ToggleRow("Romaji (watashi)", romaji) { romaji = it }
            ToggleRow("Question title", title, bold = true) { title = it }
            ToggleRow("Lesson audio", audio, bold = true) { audio = it }
            Text("Text size", fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(top = 14.dp))
            Slider(value = font.toFloat(), onValueChange = { font = it.toInt() }, valueRange = 0f..5f, steps = 4)
            Text(
                if (font == 2) "Default" else if (font < 2) "Smaller" else "Larger",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            GradientButton("Save") {
                progress.saveDisplay(kanji, reading, romaji, title, audio, font)
                onClose()
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, bold: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun ExerciseView(exercise: Exercise, accent: Color, done: (Boolean?) -> Unit) {
    when (exercise) {
        is Teach -> TeachView(exercise.item, accent, done)
        is NoteCard -> NoteView(exercise, accent, done)
        is DialogueCard -> DialogueView(exercise, accent, done)
        is Choice -> ChoiceView(exercise, accent, done)
        is Match -> MatchView(exercise, accent, done)
        is Build -> BuildView(exercise, accent, done)
        is TypeIn -> TypeInView(exercise, accent, done)
        is Speak -> SpeakView(exercise, accent, done)
        is Write -> WriteView(exercise.item, accent, done)
        is Dictate -> DictateView(exercise.item, accent, done)
    }
}

/** Shared layout: instruction, body, then either the check button or the verdict with Continue. */
@Composable
private fun Frame(
    instruction: String,
    accent: Color,
    canCheck: Boolean,
    verdict: Verdict?,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
    checkLabel: String = "Check",
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        if (LocalDisplay.current.title) Text(
            instruction,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
        if (verdict == null) {
            PushButton(checkLabel, onCheck, Modifier.padding(20.dp), enabled = canCheck, color = accent)
        } else {
            val fill = if (verdict.correct) scheme.tertiaryContainer else scheme.errorContainer
            val ink = if (verdict.correct) scheme.onTertiaryContainer else scheme.onErrorContainer
            Column(Modifier.fillMaxWidth().background(fill).padding(20.dp)) {
                Text(
                    if (verdict.correct) "Correct!" else "Not quite",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )
                Text(verdict.message, color = ink, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                PushButton("Continue", onContinue, color = if (verdict.correct) scheme.tertiary else scheme.error)
            }
        }
    }
}

private fun promptSize(text: String) = when {
    text.length <= 2 -> 80.sp
    text.length <= 8 -> 40.sp
    else -> 26.sp
}

@Composable
private fun TeachView(item: Item, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { speaker.speak(item.speak) }
    Frame("New", accent, canCheck = true, verdict = null, onCheck = { done(null) }, onContinue = {}, checkLabel = "Got it") {
        Spacer(Modifier.height(16.dp))
        Panel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (item.emoji.isNotEmpty()) Text(item.emoji, fontSize = 44.sp)
                if (item.reading.isNotEmpty()) Text(item.reading, fontSize = 18.sp, color = scheme.onSurfaceVariant)
                Text(
                    item.jp,
                    fontSize = if (item.jp.length <= 2) 112.sp else 44.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(item.romaji, fontSize = 22.sp, color = accent, fontWeight = FontWeight.Bold)
                if (item.meaning.isNotEmpty()) {
                    Text(item.meaning, fontSize = 20.sp, color = scheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                }
                SpeakButton(item.speak, Modifier.padding(top = 16.dp), size = 56.dp)
            }
        }
        if (item.note.isNotEmpty()) {
            Text(
                item.note,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun NoteView(card: NoteCard, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    Frame("Grammar", accent, canCheck = true, verdict = null, onCheck = { done(null) }, onContinue = {}, checkLabel = "Got it") {
        Column(Modifier.fillMaxWidth()) {
            Text(card.note.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
            Panel(Modifier.fillMaxWidth().padding(vertical = 12.dp), fill = scheme.primaryContainer, line = scheme.primary) {
                Text(
                    card.note.pattern,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                )
            }
            Text(card.note.body, style = MaterialTheme.typography.bodyLarge, color = scheme.onBackground)
            if (card.examples.isNotEmpty()) {
                Text(
                    "Examples",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }
            card.examples.forEach { s ->
                Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { speaker.speak(s.text) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.tokens.joinToString(" "), fontSize = 20.sp, color = scheme.onSurface)
                            Text(s.romaji, color = accent)
                            Text(s.en, color = scheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Play audio", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueView(card: DialogueCard, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    val first = card.lines.firstOrNull()?.speaker
    Frame("Conversation", accent, canCheck = true, verdict = null, onCheck = { done(null) }, onContinue = {}, checkLabel = "Continue") {
        Text(
            "Tap a line to hear it.",
            color = scheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        card.lines.forEach { line ->
            val mine = line.speaker != first
            Column(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = if (mine) Alignment.End else Alignment.Start
            ) {
                Text(line.speaker, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                Panel(
                    Modifier.fillMaxWidth(0.88f).padding(top = 4.dp),
                    fill = if (mine) scheme.primaryContainer else scheme.surface,
                    line = if (mine) scheme.primary else scheme.outlineVariant
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { speaker.speak(line.jp.replace(" ", "")) }
                            .padding(14.dp)
                    ) {
                        Text(line.jp, fontSize = 19.sp, color = scheme.onSurface)
                        Text(line.romaji, color = accent)
                        Text(line.en, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceView(exercise: Choice, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    var picked by remember { mutableIntStateOf(-1) }
    var verdict by remember { mutableStateOf<Verdict?>(null) }
    LaunchedEffect(Unit) { exercise.speak?.let(speaker::speak) }

    fun stateOf(i: Int) = when {
        verdict == null -> if (i == picked) OptionState.SELECTED else OptionState.IDLE
        i == exercise.answer -> OptionState.CORRECT
        i == picked -> OptionState.WRONG
        else -> OptionState.DIM
    }

    Frame(
        exercise.instruction, accent,
        canCheck = picked >= 0,
        verdict = verdict,
        onCheck = {
            verdict = Verdict(picked == exercise.answer, exercise.explain)
            (exercise.after ?: exercise.speak)?.let(speaker::speak)
        },
        onContinue = { done(verdict?.correct) }
    ) {
        Spacer(Modifier.height(8.dp))
        if (exercise.listen) {
            SpeakButton(exercise.speak.orEmpty(), size = 104.dp)
        } else {
            val display = LocalDisplay.current
            // With the Japanese hidden, the reading takes its place so there is still something to answer.
            val main = if (display.kanji || exercise.sub.isEmpty()) exercise.prompt else exercise.sub
            if (display.reading && display.kanji && exercise.sub.isNotEmpty()) {
                Text(exercise.sub, fontSize = 15.sp * display.scale, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Text(
                main,
                fontSize = promptSize(main) * display.scale,
                fontWeight = FontWeight.SemiBold,
                color = if (exercise.sub.isNotEmpty() || exercise.romaji.isNotEmpty()) accent else scheme.onBackground,
                textAlign = TextAlign.Center
            )
            if (display.romaji && exercise.romaji.isNotEmpty()) {
                Text(exercise.romaji, fontSize = 18.sp * display.scale, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            exercise.speak?.let { SpeakButton(it, Modifier.padding(top = 12.dp)) }
        }
        Spacer(Modifier.height(24.dp))
        if (exercise.bigOptions) {
            exercise.options.chunked(2).forEachIndexed { r, row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEachIndexed { c, option ->
                        val i = r * 2 + c
                        OptionCard(option, stateOf(i), { picked = i }, Modifier.weight(1f), enabled = verdict == null, big = true)
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            exercise.options.forEachIndexed { i, option ->
                OptionCard(
                    option, stateOf(i), { picked = i },
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    enabled = verdict == null
                )
            }
        }
    }
}

@Composable
private fun MatchView(exercise: Match, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val left = remember { exercise.pairs.map { it.first }.shuffled() }
    val right = remember { exercise.pairs.map { it.second }.shuffled() }
    val answer = remember { exercise.pairs.toMap() }
    val matched = remember { mutableStateListOf<String>() }
    var pickedLeft by remember { mutableStateOf<String?>(null) }
    var pickedRight by remember { mutableStateOf<String?>(null) }
    var miss by remember { mutableStateOf(false) }

    fun settle() {
        val l = pickedLeft ?: return
        val r = pickedRight ?: return
        if (answer[l] == r) {
            matched += l
            pickedLeft = null
            pickedRight = null
        } else {
            miss = true
        }
    }

    LaunchedEffect(miss) {
        if (miss) {
            delay(600)
            pickedLeft = null
            pickedRight = null
            miss = false
        }
    }

    fun stateOf(isMatched: Boolean, isPicked: Boolean) = when {
        isMatched -> OptionState.CORRECT
        isPicked && miss -> OptionState.WRONG
        isPicked -> OptionState.SELECTED
        else -> OptionState.IDLE
    }

    Frame(
        "Match the pairs", accent,
        canCheck = matched.size == exercise.pairs.size,
        verdict = null,
        onCheck = { done(null) },
        onContinue = {},
        checkLabel = "Continue"
    ) {
        Spacer(Modifier.height(8.dp))
        left.indices.forEach { i ->
            val l = left[i]
            val r = right[i]
            val leftDone = l in matched
            val rightDone = matched.any { answer[it] == r }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OptionCard(
                    l, stateOf(leftDone, pickedLeft == l),
                    onClick = {
                        speaker.speak(l)
                        pickedLeft = l
                        settle()
                    },
                    Modifier.weight(1f).heightIn(min = 64.dp),
                    enabled = !leftDone && !miss
                )
                OptionCard(
                    r, stateOf(rightDone, pickedRight == r),
                    onClick = {
                        pickedRight = r
                        settle()
                    },
                    Modifier.weight(1f).heightIn(min = 64.dp),
                    enabled = !rightDone && !miss
                )
            }
        }
    }
}

@Composable
private fun Tile(text: String, used: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (used) scheme.surfaceVariant else scheme.surface)
            .border(2.dp, scheme.outline, shape)
            .clickable(enabled = !used, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text, fontSize = 20.sp, color = if (used) Color.Transparent else scheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildView(exercise: Build, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    // Indices into the pool, so repeated words stay distinct tiles.
    val chosen = remember { mutableStateListOf<Int>() }
    var verdict by remember { mutableStateOf<Verdict?>(null) }

    Frame(
        "Build the sentence", accent,
        canCheck = chosen.isNotEmpty(),
        verdict = verdict,
        onCheck = {
            val ok = chosen.map { exercise.pool[it] } == exercise.tokens
            verdict = Verdict(ok, "${exercise.tokens.joinToString(" ")}\n${exercise.romaji}")
            speaker.speak(exercise.tokens.joinToString(""))
        },
        onContinue = { done(verdict?.correct) }
    ) {
        Text(
            exercise.en,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        Panel(Modifier.fillMaxWidth().padding(vertical = 12.dp), fill = scheme.surfaceVariant, line = scheme.outlineVariant) {
            FlowRow(
                Modifier.fillMaxWidth().heightIn(min = 124.dp).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chosen.forEach { i ->
                    Tile(exercise.pool[i], used = false) { if (verdict == null) chosen.remove(i) }
                }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exercise.pool.forEachIndexed { i, token ->
                Tile(token, used = i in chosen) { if (verdict == null) chosen.add(i) }
            }
        }
    }
}

@Composable
private fun TypeInView(exercise: TypeIn, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<Verdict?>(null) }
    LaunchedEffect(Unit) { speaker.speak(exercise.speak) }

    fun check() {
        if (text.isBlank() || verdict != null) return
        val ok = Exercises.normalise(text) in exercise.answers
        verdict = Verdict(ok, "${exercise.prompt} = ${exercise.answers.first()}")
        speaker.speak(exercise.speak)
    }

    Frame(
        "Type it in romaji", accent,
        canCheck = text.isNotBlank(),
        verdict = verdict,
        onCheck = ::check,
        onContinue = { done(verdict?.correct) }
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            exercise.prompt,
            fontSize = promptSize(exercise.prompt),
            fontWeight = FontWeight.Medium,
            color = scheme.onBackground,
            textAlign = TextAlign.Center
        )
        SpeakButton(exercise.speak, Modifier.padding(top = 12.dp, bottom = 24.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = verdict == null,
            singleLine = true,
            label = { Text("Romaji") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { check() })
        )
    }
}

@Composable
private fun SpeakView(exercise: Speak, accent: Color, done: (Boolean?) -> Unit) {
    val phrase = exercise.phrase
    val speaker = LocalSpeaker.current
    val scheme = MaterialTheme.colorScheme
    var heard by remember { mutableStateOf<String?>(null) }
    var unavailable by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<Verdict?>(null) }
    LaunchedEffect(Unit) { speaker.speak(phrase.jp) }

    val recogniser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val texts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (texts.isNotEmpty()) {
            // The recogniser usually answers in kanji, so compare against every known spelling.
            val targets = (phrase.heard + phrase.jp).map(Exercises::normalise)
            val said = texts.map(Exercises::normalise)
            heard = texts.first()
            if (said.any { s -> targets.any { s == it || s.contains(it) } }) {
                verdict = Verdict(true, "${phrase.jp}\n${phrase.en}")
            }
        }
    }

    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, phrase.jp)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        try {
            recogniser.launch(intent)
        } catch (e: ActivityNotFoundException) {
            unavailable = true
        }
    }

    // Skipping is never counted as a mistake: recognition depends on the device and the room.
    Frame(
        "Say it aloud", accent,
        canCheck = true,
        verdict = verdict,
        onCheck = { done(null) },
        onContinue = { done(true) },
        checkLabel = "Skip"
    ) {
        Spacer(Modifier.height(8.dp))
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phrase.jp, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = scheme.onSurface, textAlign = TextAlign.Center)
                Text(phrase.romaji, color = accent, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Text(phrase.en, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                SpeakButton(phrase.jp, Modifier.padding(top = 12.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        if (verdict == null) {
            PushButton("Tap to speak", ::listen, enabled = !unavailable)
        }
        when {
            unavailable -> Text(
                "Speech recognition is not available on this device. You can skip this one.",
                color = scheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            heard != null && verdict == null -> Text(
                "We heard: $heard\nListen again and have another go.",
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/** Square pad the learner draws on with a finger. [guide] is shown faintly underneath for tracing. */
@Composable
private fun DrawPad(
    strokes: SnapshotStateList<List<Offset>>,
    guide: String? = null,
    enabled: Boolean = true,
    onSize: (IntSize) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    var current by remember { mutableStateOf(emptyList<Offset>()) }
    val shape = RoundedCornerShape(20.dp)
    val ink = scheme.onSurface
    val rule = scheme.outline
    Box(
        Modifier
            .size(280.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(2.dp, scheme.outline, shape)
            .onSizeChanged(onSize),
        contentAlignment = Alignment.Center
    ) {
        if (guide != null) Text(guide, fontSize = if (guide.length > 1) 110.sp else 190.sp, color = ink.copy(alpha = 0.14f))
        Canvas(
            Modifier.fillMaxSize().pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { current = listOf(it) },
                    onDrag = { change, _ -> current = current + change.position },
                    onDragEnd = {
                        strokes.add(current)
                        current = emptyList()
                    },
                    onDragCancel = { current = emptyList() }
                )
            }
        ) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
            drawLine(rule, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f, pathEffect = dash)
            drawLine(rule, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f, pathEffect = dash)
            (strokes + listOf(current)).forEach { points ->
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (p in points.drop(1)) lineTo(p.x, p.y)
                    }
                    drawPath(path, ink, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
private fun WriteView(item: Item, accent: Color, done: (Boolean?) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var guide by remember { mutableStateOf(true) }

    // Not scrollable: a scrolling parent would steal vertical strokes from the canvas.
    Frame(
        "Write it", accent,
        canCheck = strokes.isNotEmpty(),
        verdict = null,
        onCheck = { done(true) },
        onContinue = {},
        checkLabel = "Done",
        scroll = false
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                if (item.isKana) item.romaji else "${item.meaning}  ·  ${item.romaji}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(Modifier.width(12.dp))
            SpeakButton(item.speak, size = 40.dp)
        }
        DrawPad(strokes, guide = if (guide) item.jp else null)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { strokes.clear() }) { Text("Clear") }
            TextButton(onClick = { guide = !guide }) { Text(if (guide) "Hide guide" else "Show guide") }
        }
    }
}

@Composable
private fun DictateView(item: Item, accent: Color, done: (Boolean?) -> Unit) {
    val speaker = LocalSpeaker.current
    val handwriting = LocalHandwriting.current
    val scheme = MaterialTheme.colorScheme
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var reading by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<Verdict?>(null) }
    val ready = handwriting.state == Handwriting.State.READY
    val failed = handwriting.state == Handwriting.State.FAILED
    LaunchedEffect(Unit) { speaker.speak(item.speak) }

    // The sound alone does not say which script is wanted.
    val script = when (item.kind) {
        Kind.HIRAGANA -> "in hiragana"
        Kind.KATAKANA -> "in katakana"
        else -> "the kanji for \"${item.meaning}\""
    }

    fun check() {
        reading = true
        handwriting.read(strokes.toList(), area.width.toFloat(), area.height.toFloat()) { guesses ->
            reading = false
            val answer = "${item.jp} = ${item.romaji}"
            verdict = when {
                guesses == null -> Verdict(false, "$answer\nYour writing could not be read.")
                Handwriting.matches(item.jp, guesses) -> Verdict(true, answer)
                else -> Verdict(false, "$answer\nYour writing looked like: ${guesses.firstOrNull().orEmpty()}")
            }
        }
    }

    // Without the model there is nothing to check against, so the task can only be skipped.
    Frame(
        "Listen and write $script", accent,
        canCheck = failed || (ready && strokes.isNotEmpty() && !reading),
        verdict = verdict,
        onCheck = { if (failed) done(null) else check() },
        onContinue = { done(verdict?.correct) },
        checkLabel = if (failed) "Skip" else "Check",
        scroll = false
    ) {
        SpeakButton(item.speak, Modifier.padding(bottom = 12.dp), size = 64.dp)
        DrawPad(strokes, guide = if (verdict != null) item.jp else null, enabled = verdict == null) { area = it }
        when {
            verdict != null -> Unit
            failed -> {
                Text(
                    "Handwriting checking needs an internet connection the first time, to download its language pack.",
                    color = scheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(onClick = { handwriting.prepare() }) { Text("Try again") }
            }
            !ready -> Text(
                "Getting the handwriting language pack ready…",
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            else -> TextButton(onClick = { strokes.clear() }, modifier = Modifier.padding(top = 8.dp)) { Text("Clear") }
        }
    }
}

@Composable
private fun Summary(session: Session, gained: Int, graded: Int, right: Int, onContinue: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accuracy = if (graded == 0) 100 else right * 100 / graded
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Seal("合格", 120.dp)
            Text(
                when (session.kind) {
                    SessionKind.LESSON -> "Lesson complete!"
                    SessionKind.CHALLENGE -> "Challenge complete!"
                    else -> "Practice complete!"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(session.title, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Rounded.Star, Kin, "+$gained", "XP earned", Modifier.weight(1f))
                StatCard(Icons.Rounded.TrackChanges, scheme.tertiary, "$accuracy%", "Accuracy", Modifier.weight(1f))
            }
            val koban = Progress.coinsFor(session.kind, graded == right)
            if (koban > 0) {
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    DropIcon(22.dp)
                    Text(
                        "+$koban drops",
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            session.testOut?.let { unit ->
                val passed = accuracy >= Config.testOutPercent
                Text(
                    if (passed) "Passed! Every lesson in ${unit.title} is now complete."
                    else "You need ${Config.testOutPercent}% to test out. Keep practising and try again.",
                    color = if (passed) scheme.tertiary else scheme.error,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            if (graded == right) {
                Text(
                    "No mistakes: +${Config.perfectBonus} bonus XP",
                    color = scheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        PushButton("Continue", onContinue, color = session.color)
    }
}
