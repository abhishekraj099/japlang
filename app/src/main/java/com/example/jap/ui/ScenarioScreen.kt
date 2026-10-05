package com.example.jap.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Item
import com.example.jap.data.Progress
import com.example.jap.data.Scenario
import com.example.jap.data.ScenarioPart
import com.example.jap.data.Sentence

private const val REPORT_LIMIT = 200
private val PartColours = listOf(Color(0xFF3F9B5A), Color(0xFFE0A100), Color(0xFFD9573F), Color(0xFF34408F))

/**
 * One everyday situation: an illustrated header, then a swipeable card per part showing its key
 * vocabulary or sentence patterns, and a button to start the part on screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioScreen(scenario: Scenario, progress: Progress, onLearn: (part: Int) -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pager = rememberPagerState { scenario.parts.size }
    var reporting by remember { mutableStateOf(false) }
    val share = scenarioShare(scenario, progress)
    BackHandler(onBack = onBack)

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize()) {
            // Header art.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clipToBounds()
                    .background(Brush.verticalGradient(listOf(Color(scenario.dark), Color(scenario.color), scheme.background)))
                    .waves(Color.White.copy(alpha = 0.08f), radius = 26.dp)
            ) {
                val scene = rememberScene(scenario.id)
                if (scene != null) {
                    // Crop so the face sits a little below the header buttons. A 3:4 picture shown full width
                    // in this header is about twice as tall as the visible strip.
                    BoxWithConstraints(Modifier.matchParentSize()) {
                        val shown = maxHeight.value / (maxWidth.value * 4f / 3f)
                        val top = (scenario.imageFocus - shown * 0.58f).coerceIn(0f, 1f - shown)
                        val bias = if (shown >= 1f) 0f else top / (1f - shown) * 2f - 1f
                        SceneImage(scene, scenario.title, Modifier.matchParentSize(), BiasAlignment(0f, bias))
                    }
                    // Fade the picture into the page so the title below reads cleanly.
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Transparent, scheme.background))))
                } else Text(
                    scenario.icon,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 120.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
                )
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glass(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Glass(onClick = { reporting = true }) { Icon(Icons.Rounded.ErrorOutline, contentDescription = "Report a problem", tint = Color.White) }
                    Row(
                        Modifier.padding(start = 10.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { share }, modifier = Modifier.size(22.dp), color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f), strokeWidth = 3.dp, strokeCap = StrokeCap.Round
                        )
                        Text("${(share * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Text(scenario.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                Text(
                    scenario.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp)
                )
                HorizontalPager(
                    pager,
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    pageSpacing = 14.dp,
                    modifier = Modifier.weight(1f).padding(top = 8.dp)
                ) { page ->
                    PartCard(page, scenario.parts[page], scenario.partId(page) in progress.done)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(scenario.parts.size) { i ->
                        val width by animateDpAsState(if (i == pager.currentPage) 28.dp else 8.dp, label = "dot")
                        Box(
                            Modifier.padding(horizontal = 3.dp).width(width).height(8.dp).clip(CircleShape)
                                .background(if (i == pager.currentPage) Color(scenario.color) else scheme.outlineVariant)
                        )
                    }
                }
            }
            Box(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                val done = scenario.partId(pager.currentPage) in progress.done
                GradientButton(if (done) "Practise this part again" else "Learn this part") { onLearn(pager.currentPage) }
            }
        }
    }

    if (reporting) ReportSheet(onDismiss = { reporting = false }) { text ->
        progress.report(scenario.partId(pager.currentPage), text)
        reporting = false
    }
}

@Composable
private fun Glass(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PartCard(index: Int, part: ScenarioPart, finished: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tab = remember { mutableStateMapOf<Int, Int>() }
    val showing = tab[index] ?: 0
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxSize().clip(shape).background(scheme.surface).border(1.dp, scheme.outlineVariant, shape).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(26.dp).clip(CircleShape).background(PartColours[index % PartColours.size]))
            Text("${index + 1}. ${part.title}", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.weight(1f).padding(start = 10.dp))
            if (finished) Icon(Icons.Rounded.CheckCircle, contentDescription = "Finished", tint = scheme.tertiary)
        }
        // Segmented switch.
        Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(CircleShape).background(scheme.surfaceVariant).padding(4.dp)) {
            listOf("Vocabulary", "Sentence patterns").forEachIndexed { i, label ->
                val selected = showing == i
                val fill by animateColorAsState(if (selected) scheme.primary else Color.Transparent, label = "tab")
                Text(
                    label,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(CircleShape).background(fill).clickable(role = Role.Tab) { tab[index] = i }.padding(vertical = 11.dp)
                )
            }
        }
        Text(
            if (showing == 0) "KEY VOCABULARY (${part.vocab.size})" else "KEY PATTERNS (${part.patterns.size})",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showing == 0) {
                part.vocab.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { VocabTile(it, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                part.patterns.forEach { PatternTile(it) }
            }
        }
    }
}

@Composable
private fun VocabTile(item: Item, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).border(1.dp, scheme.outlineVariant, shape).clickable { speaker.speak(item.speak) }.padding(12.dp)) {
        if (item.reading.isNotEmpty()) Text(item.reading, fontSize = 11.sp, color = scheme.onSurfaceVariant)
        Text(item.jp, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
        Text(item.meaning, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun PatternTile(s: Sentence) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, scheme.outlineVariant, shape).clickable { speaker.speak(s.text) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(s.romaji, style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
            Text(s.en, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Play", tint = scheme.onSurfaceVariant)
    }
}

/** Lets the learner describe a mistake in the content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(onDismiss: () -> Unit, reasons: List<String> = emptyList(), onSend: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = scheme.surface, dragHandle = null) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Report a problem", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
            reasons.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.RadioButton) { reason = r }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = reason == r, onClick = { reason = r })
                    Text(r, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                }
            }
            Text("What's wrong?", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(REPORT_LIMIT) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("A wrong translation, a typo, audio that doesn't match…") },
                shape = RoundedCornerShape(16.dp)
            )
            Text("${text.length}/$REPORT_LIMIT", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp))
            GradientButton("Send", enabled = text.isNotBlank() || reason != null) {
                onSend(listOfNotNull(reason?.let { "[$it]" }, text.trim().ifEmpty { null }).joinToString(" "))
                Toast.makeText(context, "Thanks! Your report was saved.", Toast.LENGTH_SHORT).show()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
