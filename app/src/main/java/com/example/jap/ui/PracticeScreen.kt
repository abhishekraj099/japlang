package com.example.jap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Exam
import com.example.jap.data.Item
import com.example.jap.data.Progress

private const val SESSION_SIZE = 12

@Composable
fun PracticeScreen(
    progress: Progress,
    onReview: (title: String, items: List<Item>) -> Unit,
    onWords: () -> Unit,
    onSpeak: () -> Unit,
    onDictate: () -> Unit,
    onGame: () -> Unit,
    onExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val srs = progress.srs
    val due = remember(srs) { progress.due(System.currentTimeMillis()) }
    val learned = remember(srs) { srs.keys.mapNotNull(Course::item) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("${due.size}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                Text(
                    when {
                        learned.isEmpty() -> "Finish a lesson and what you learn will come back here for review."
                        due.isEmpty() -> "Nothing is due right now. Come back later, or practice anyway."
                        else -> "items are ready for review. Reviewing on time moves them into long-term memory."
                    },
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PushButton(
                    "Review now",
                    onClick = { onReview("Review", due.shuffled().take(SESSION_SIZE)) },
                    enabled = due.isNotEmpty()
                )
                PushButton(
                    "Practice everything learned",
                    onClick = { onReview("Practice", learned.shuffled().take(SESSION_SIZE)) },
                    modifier = Modifier.padding(top = 10.dp),
                    enabled = learned.isNotEmpty(),
                    color = scheme.secondary
                )
            }
        }
        Activity(Icons.Rounded.Style, "Words & flashcards", "Look up every word in the course and flip through cards.", onWords)
        Activity(Icons.Rounded.Mic, "Speaking", "Say everyday phrases aloud and get them checked.", onSpeak)
        Activity(Icons.Rounded.Draw, "Listen & write", "Hear a character, write it by hand and have it checked.", onDictate)
        Activity(
            Icons.Rounded.Bolt, "Speed round",
            "Answer as many as you can in ${Config.gameSeconds} seconds." + if (progress.bestGame > 0) " Best: ${progress.bestGame}." else "",
            onGame
        )
        Activity(
            Icons.Rounded.Quiz, Exam.title,
            "A timed practice test with explanations." + if (progress.bestExam >= 0) " Best: ${progress.bestExam}%." else "",
            onExam
        )
    }
}

@Composable
private fun Activity(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Panel(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}
