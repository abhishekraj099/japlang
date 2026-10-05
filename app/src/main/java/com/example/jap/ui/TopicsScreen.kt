package com.example.jap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Course
import com.example.jap.data.Lesson
import com.example.jap.data.Progress
import com.example.jap.data.Scenario
import com.example.jap.data.Scenarios

private val Field = RoundedCornerShape(28.dp)

/**
 * Lesson+: look up an everyday situation and learn it. Searching goes through [Scenarios.source],
 * so pointing that at a server is all a backend needs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicsScreen(
    progress: Progress,
    onScenario: (Scenario) -> Unit,
    onAll: () -> Unit,
    onCourseTopic: (title: String, lessons: List<Lesson>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    var topic by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf<String?>(null) }
    val source = Scenarios.source
    val results = searched?.let(source::search).orEmpty()
    val courseMatches = searched?.let(Course::lessonsAbout).orEmpty()

    fun search(text: String) {
        topic = text
        searched = text.trim().takeIf { it.isNotEmpty() }
        focus.clearFocus()
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("What do you want to learn today?", fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, color = scheme.onBackground)
                Text(
                    "Search an everyday situation and learn the words and phrases for it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            BrandMark(76.dp, Modifier.padding(start = 12.dp), shape = RoundedCornerShape(24.dp))
        }

        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp),
            singleLine = true,
            placeholder = { Text("Try \"restaurant\", \"train\" or \"doctor\"") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (topic.isNotEmpty()) IconButton(onClick = { topic = ""; searched = null }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search(topic) }),
            shape = Field,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = scheme.surface, unfocusedContainerColor = scheme.surface,
                unfocusedBorderColor = scheme.outline
            )
        )
        Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            GradientButton("Find lessons", Icons.Rounded.AutoAwesome, enabled = topic.isNotBlank()) { search(topic) }
        }
        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("introduce", "restaurant", "shopping", "train", "doctor", "weather", "phone", "home").forEach { t ->
                Text(
                    t,
                    color = scheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape).background(scheme.primaryContainer).clickable { search(t) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        searched?.let { query ->
            Section("Results for \"$query\"", null, null)
            if (results.isEmpty() && courseMatches.isEmpty()) {
                Text(
                    "Nothing matches \"$query\" yet. Try one of the topics above.",
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                results.forEach { ScenarioRow(it, progress) { onScenario(it) } }
                if (courseMatches.isNotEmpty()) {
                    Panel(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onCourseTopic(query.replaceFirstChar { it.uppercase() }, courseMatches) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = scheme.secondary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("Mixed practice from the course", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                                Text("${courseMatches.size} course lessons mention \"$query\"", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Section("Everyday situations", "View all", onAll)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(source.curated(), key = { it.id }) { ScenarioCard(it, progress) { onScenario(it) } }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Section(title: String, action: String?, onAction: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

/** Primary call to action: a pill with a soft indigo-to-vermilion gradient. */
@Composable
fun GradientButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(scheme.primary, Color(0xFF5A4FA8), scheme.secondary))
                else Brush.horizontalGradient(listOf(scheme.surfaceVariant, scheme.surfaceVariant))
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ink = if (enabled) Color.White else scheme.onSurfaceVariant
        if (icon != null) Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.padding(end = 10.dp))
        Text(text, color = ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Finished share of a scenario's parts, 0 to 1. */
fun scenarioShare(s: Scenario, progress: Progress) = s.parts.indices.count { s.partId(it) in progress.done } / s.parts.size.toFloat()

/** The tall illustrated card used in the curated row. */
@Composable
fun ScenarioCard(s: Scenario, progress: Progress, width: androidx.compose.ui.unit.Dp = 220.dp, onClick: () -> Unit) {
    val share = scenarioShare(s, progress)
    Column(
        Modifier
            .width(width)
            .height(290.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(s.color), Color(s.dark))))
            .clipToBounds()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        val scene = rememberScene(s.id)
        Box(Modifier.weight(1f).fillMaxWidth().waves(Color.White.copy(alpha = 0.10f), radius = 20.dp)) {
            if (scene != null) {
                SceneImage(scene, s.title, Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(s.dark).copy(alpha = 0.85f)))))
            } else Text(
                s.icon,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 120.sp,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.Center).offset(y = 6.dp)
            )
            if (share >= 1f) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Finished", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
            Text(
                s.level,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.32f)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxWidth(share).height(3.dp).background(Color.White))
        }
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.size(36.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Open", tint = Color.White)
            }
        }
    }
}

/** A compact row used in search results and the full list. */
@Composable
fun ScenarioRow(s: Scenario, progress: Progress, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val share = scenarioShare(s, progress)
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val scene = rememberScene(s.id)
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(s.color), Color(s.dark)))).waves(Color.White.copy(alpha = 0.12f), 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (scene != null) SceneImage(scene, s.title, Modifier.matchParentSize())
                else Text(s.icon, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Text(
                    "${s.level} · ${s.parts.size} parts",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                Meter(share, Color(s.color), Modifier.padding(top = 8.dp), height = 5.dp)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** Every scenario, from "View all". */
@Composable
fun AllScenariosScreen(progress: Progress, onScenario: (Scenario) -> Unit, onBack: () -> Unit) {
    SubScreen("Everyday situations", onBack) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Scenarios.source.all(), key = { it.id }) { ScenarioRow(it, progress) { onScenario(it) } }
        }
    }
}
