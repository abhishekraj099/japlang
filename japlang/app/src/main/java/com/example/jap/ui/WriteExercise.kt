package com.example.jap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Analytics
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.Progress
import com.example.jap.data.Writing
import com.example.jap.ink.Handwriting
import com.example.jap.ink.LocalHandwriting

private val BoxSize = 44.dp
private val PadSize = 260.dp

/**
 * Write an item from its meaning and sound, one character per box. An item never written correctly is traced over
 * grey letters; after that it is written freely. Both are checked by the handwriting recogniser.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WriteView(item: Item, accent: Color, progress: Progress, done: (Boolean?) -> Unit) {
    val handwriting = LocalHandwriting.current
    val scheme = MaterialTheme.colorScheme
    // Fixed for this exercise, even if the schedule changes while it is on screen.
    val trace = remember { !progress.knowsWriting(item.id) }
    val spellings = remember { Writing.spellings(item) }
    val guide = spellings.first()
    val count = if (trace) Writing.boxes(item, guide) else Writing.boxes(item)
    val boxes = remember { List(count) { mutableStateListOf<List<Offset>>() } }
    var active by remember { mutableIntStateOf(0) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var reading by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<Verdict?>(null) }
    // The grade is the first attempt; "Try again" is for practice.
    var firstTry by remember { mutableStateOf<Boolean?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    // Before tracing a character for the first time, the learner watches it being written stroke by stroke.
    val watched = remember { mutableStateListOf<Int>() }
    val demo = guide.getOrNull(active)?.toString()?.takeIf { trace && active !in watched && hasStrokeOrder(it) }
    val title = if (trace) "Trace it" else "Write it"

    fun check() {
        reading = true
        handwriting.readBoxes(boxes.map { it.toList() }, area.width.toFloat(), area.height.toFloat()) { read ->
            reading = false
            val matched = Writing.match(item, read, Config.writingTop)
            if (firstTry == null) firstTry = matched != null
            attempts++
            Analytics.log(
                Analytics.WRITING_CHECK,
                "item_id" to item.id, "kind" to item.kind.name, "correct" to (matched != null), "attempt" to attempts,
                "mode" to if (trace) "trace" else "free",
                "spelling" to when {
                    matched == null -> null
                    spellings.size == 1 -> "only"
                    matched == spellings[0] -> "kanji"
                    else -> "kana"
                },
                "read" to Writing.guess(read)
            )
            verdict = if (matched != null) Verdict(true, praise(item, matched, spellings))
            else Verdict(false, "We read: ${Writing.guess(read).ifEmpty { "nothing" }}\nAnswer: ${spellings.joinToString("  or  ")}")
        }
    }

    fun retry() {
        boxes.forEach { it.clear() }
        active = 0
        verdict = null
    }

    HandwritingGate(title, accent, onSkip = { done(null) }) {
        Frame(
            title, accent,
            canCheck = boxes.any { it.isNotEmpty() } && !reading,
            verdict = verdict,
            onCheck = ::check,
            onContinue = { done(firstTry ?: false) },
            checkLabel = if (reading) "Checking…" else "Check",
            // Not scrollable: a scrolling parent would steal vertical strokes from the pad.
            scroll = false,
            retry = ::retry
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        if (item.isKana) item.romaji else "${item.emoji} ${item.meaning}".trim(),
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent
                    )
                    // For kana the sound alone does not say which script is wanted.
                    Text(
                        when (item.kind) {
                            Kind.HIRAGANA -> "in hiragana"
                            Kind.KATAKANA -> "in katakana"
                            else -> item.romaji
                        },
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                SpeakButton(item.speak, size = 40.dp)
            }
            Text(
                when {
                    demo != null -> "Watch the order first: start with stroke 1."
                    trace -> "Trace the grey letters, one in each box."
                    spellings.size > 1 -> "Write it in kanji or in kana, one character per box. Leave spare boxes empty."
                    else -> "One character per box."
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Writing.prefix(item).takeIf { it.isNotEmpty() }?.let {
                    Box(Modifier.size(BoxSize), contentAlignment = Alignment.Center) { Text(it, fontSize = 22.sp, color = scheme.onSurfaceVariant) }
                }
                boxes.forEachIndexed { i, strokes ->
                    MiniBox(strokes, area, selected = i == active && verdict == null, accent) { if (verdict == null) active = i }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (demo != null) {
                key(demo) {
                    StrokeOrder(
                        demo, accent, numbered = true, size = PadSize,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(scheme.surface)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { watched += active }) { Text("Now you try ›") }
                }
            } else {
                key(active) {
                    DrawPad(
                        boxes[active],
                        guide = if (trace) guide.getOrNull(active)?.toString() else null,
                        enabled = verdict == null && !reading,
                        padSize = PadSize
                    ) { area = it }
                }
                if (verdict == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (trace && guide.getOrNull(active)?.toString()?.let(::hasStrokeOrder) == true) {
                            TextButton(onClick = { watched -= active }) { Text("Watch") }
                        }
                        TextButton(onClick = { active-- }, enabled = active > 0) { Text("‹ Back") }
                        TextButton(onClick = { boxes[active].clear() }) { Text("Clear") }
                        TextButton(onClick = { active++ }, enabled = active < count - 1) { Text("Next ›") }
                    }
                }
            }
        }
    }
}

/** What a correct answer says, naming the spelling the learner chose when there were two. */
private fun praise(item: Item, matched: String, spellings: List<String>): String = when {
    item.isKana -> "${item.jp} = ${item.romaji}"
    spellings.size == 1 -> "$matched = ${item.meaning}"
    matched == spellings[0] -> "You wrote it in kanji: $matched (${spellings[1]})"
    else -> "You wrote it in kana: $matched. In kanji: ${spellings[0]}"
}

/** One small box in the strip, showing what was drawn in it. */
@Composable
private fun MiniBox(strokes: List<List<Offset>>, area: IntSize, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val ink = scheme.onSurface
    Canvas(
        Modifier
            .size(BoxSize)
            .clip(shape)
            .background(scheme.surface)
            .border(if (selected) 3.dp else 1.dp, if (selected) accent else scheme.outline, shape)
            .clickable(role = Role.Button, onClickLabel = "Write in this box", onClick = onClick)
    ) {
        if (area.width == 0) return@Canvas
        val scale = size.width / area.width
        strokes.forEach { points ->
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x * scale, points[0].y * scale)
                    points.drop(1).forEach { lineTo(it.x * scale, it.y * scale) }
                }
                drawPath(path, ink, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/**
 * Shows [content] once the handwriting model is on the device. It downloads by itself when the app starts, so this
 * only shows while that is still under way or waiting for a connection; [onSkip] leaves the exercise with no penalty.
 */
@Composable
internal fun HandwritingGate(title: String, accent: Color, onSkip: () -> Unit, content: @Composable () -> Unit) {
    val handwriting = LocalHandwriting.current
    if (handwriting.state == Handwriting.State.READY) {
        content()
        return
    }
    Frame(title, accent, canCheck = true, verdict = null, onCheck = onSkip, onContinue = {}, checkLabel = "Skip for now") {
        Spacer(Modifier.height(24.dp))
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Draw, contentDescription = null, tint = accent, modifier = Modifier.size(44.dp))
                if (handwriting.state == Handwriting.State.UNSUPPORTED) {
                    GateText(
                        "Handwriting isn't available on this phone",
                        "This phone can't check Japanese handwriting, so writing exercises are skipped. It doesn't count against you."
                    )
                } else if (handwriting.state == Handwriting.State.OFFLINE) {
                    GateText(
                        "Handwriting will be ready once you're online",
                        "Checking your writing needs a one-time download. It starts by itself when you connect, " +
                            "then works offline. You can skip this one; it doesn't count against you."
                    )
                } else {
                    GateText("Getting handwriting ready", "This happens once and only takes a moment.")
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp), color = accent)
                }
            }
        }
    }
}

@Composable
private fun GateText(title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    if (body.isNotEmpty()) Text(body, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
}
