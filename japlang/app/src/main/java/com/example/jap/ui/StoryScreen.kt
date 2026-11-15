package com.example.jap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Course
import com.example.jap.data.Stories
import com.example.jap.data.Story

/** Every story in course order: unlocked ones open, the rest greyed with the unit that unlocks them. */
@Composable
fun StoriesScreen(done: Set<String>, onStory: (Story) -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val open = Stories.unlocked(done).toSet()
    SubScreen("Stories", onBack) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Stories.all, key = { it.id }) { story ->
                val unlocked = story in open
                Panel(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = unlocked, role = Role.Button) { onStory(story) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(if (unlocked) scheme.primaryContainer else scheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (unlocked) Icons.AutoMirrored.Rounded.MenuBook else Icons.Rounded.Lock,
                                contentDescription = if (unlocked) null else "Locked",
                                tint = if (unlocked) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(story.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (unlocked) scheme.onSurface else scheme.onSurfaceVariant)
                            Text(
                                if (unlocked) "${story.lines.size} lines · ${story.questions.size} questions"
                                else "Finish ${Course.units.firstOrNull { it.id == story.unlockAfter }?.title.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant
                            )
                        }
                        if (story.id in done) Icon(Icons.Rounded.CheckCircle, contentDescription = "Finished", tint = scheme.tertiary)
                    }
                }
            }
        }
    }
}

/** A story to read line by line; tap a line to hear it. English starts hidden so the learner reads the Japanese first. */
@Composable
fun StoryScreen(story: Story, onQuiz: () -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    var romaji by rememberSaveable { mutableStateOf(true) }
    var english by rememberSaveable { mutableStateOf(false) }
    SubScreen(story.title, onBack) {
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = romaji, onClick = { romaji = !romaji }, label = { Text("Romaji") })
            FilterChip(selected = english, onClick = { english = !english }, label = { Text("English") })
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(story.lines) { line ->
                Panel(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable(onClickLabel = "Play audio", role = Role.Button) { speaker.speak(line.tokens.joinToString("")) }
                            .padding(14.dp)
                    ) {
                        if (line.speaker.isNotEmpty()) {
                            Text(line.speaker, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
                        }
                        Text(line.tokens.joinToString(" "), fontSize = 20.sp, color = scheme.onSurface)
                        if (romaji) Text(line.romaji, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        if (english) Text(line.en, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        PushButton("Check your understanding", onClick = onQuiz, modifier = Modifier.padding(20.dp))
    }
}
