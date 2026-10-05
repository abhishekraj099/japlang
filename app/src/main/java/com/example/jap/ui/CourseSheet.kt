package com.example.jap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Course
import com.example.jap.data.Level
import com.example.jap.data.Track

private val CardShape = RoundedCornerShape(20.dp)
private val ChipShape = RoundedCornerShape(12.dp)

/**
 * Pick what to study: every course with its lesson count; tapping a course with several levels
 * opens its level chips, a single-level course is chosen straight away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSheet(current: String, currentLevel: Int, done: Set<String>, onDismiss: () -> Unit, onPick: (track: String, level: Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(current) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.background,
        dragHandle = null
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Select course", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = scheme.onSurfaceVariant) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Course.courses, key = { it.id }) { track ->
                val selected = track.id == current
                val expanded = track.id == open && track.levels.size > 1
                val tint = Color(track.color)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(if (selected) tint.copy(alpha = 0.10f) else scheme.surface)
                        .border(if (selected) 1.5.dp else 1.dp, if (selected) tint else scheme.outlineVariant, CardShape)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                if (track.levels.size > 1) open = if (open == track.id) "" else track.id else onPick(track.id, 0)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Emblem(track)
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(track.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (selected) tint else scheme.onSurface)
                            val total = Course.lessonCount(track)
                            val finished = track.unitIds.sumOf { id -> Course.units.firstOrNull { it.id == id }?.lessons?.count { it.id in done } ?: 0 }
                            Text(
                                if (finished > 0) "$finished / $total lessons" else "$total lessons",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) tint else scheme.onSurfaceVariant
                            )
                        }
                        if (track.levels.size > 1) {
                            Icon(
                                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (expanded) "Hide levels" else "Show levels",
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }
                    AnimatedVisibility(expanded) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            track.levels.chunked(2).forEachIndexed { r, row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEachIndexed { c, level ->
                                        val index = r * 2 + c
                                        LevelChip(level, tint, selected && index == currentLevel, Modifier.weight(1f)) { onPick(track.id, index) }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A kanji on a tinted tile with the wave pattern, standing for the course. */
@Composable
private fun Emblem(track: Track) {
    val tint = Color(track.color)
    Box(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = 0.16f))
            .waves(tint.copy(alpha = 0.22f), radius = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(track.icon, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, color = Color(track.dark))
    }
}

@Composable
private fun LevelChip(level: Level, tint: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(ChipShape)
            .background(if (selected) tint else scheme.surface)
            .border(1.dp, if (selected) tint else scheme.outlineVariant, ChipShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (level.rank == 0) {
            Text("あ", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = if (selected) Color.White else tint)
        } else {
            // Difficulty bars: one to three filled.
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                (1..3).forEach { i ->
                    val on = i <= level.rank
                    Box(
                        Modifier
                            .width(5.dp)
                            .height((6 + i * 4).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    selected && on -> Color.White
                                    selected -> Color.White.copy(alpha = 0.35f)
                                    on -> tint
                                    else -> scheme.outlineVariant
                                }
                            )
                    )
                }
            }
        }
        Text(
            level.name,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else scheme.onSurface,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
