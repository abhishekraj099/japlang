package com.example.jap.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
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
import com.example.jap.data.Guide
import com.example.jap.data.GuideTalk
import com.example.jap.data.Item
import com.example.jap.data.Memory
import com.example.jap.data.Progress
import com.example.jap.data.Review
import com.example.jap.data.Stories
import com.example.jap.data.Talks

internal const val SESSION_SIZE = 12

@Composable
fun PracticeScreen(
    progress: Progress,
    /** [due] is true for the spaced-repetition review, false for practising anything learned. */
    onReview: (title: String, items: List<Item>, due: Boolean) -> Unit,
    onWords: () -> Unit,
    onSpeak: () -> Unit,
    onDictate: () -> Unit,
    onWriting: (items: List<Item>) -> Unit,
    onGame: () -> Unit,
    onExam: () -> Unit,
    onStories: () -> Unit,
    onTalk: (GuideTalk) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val srs = progress.srs
    val learned = remember(srs) { srs.keys.mapNotNull(Course::item) }
    val plan = remember(srs) {
        Review.plan(progress.dueSchedule(writing = false, System.currentTimeMillis()), progress.reviewedToday(writing = false), progress.daysAway)
    }
    val writePlan = remember(progress.writeSrs) {
        Review.plan(progress.dueSchedule(writing = true, System.currentTimeMillis()), progress.reviewedToday(writing = true), progress.daysAway)
    }
    val writeNew = remember(srs, progress.writeSrs) { progress.writeNew() }
    // Grammar points come back inside a revision, a few per session, and are not part of the daily cap.
    val grammarDue = remember(srs) { minOf(Review.dueGrammar(srs).size, GRAMMAR_PER_SESSION) }
    val dueToday = plan.today.size + grammarDue
    val items = remember(srs) { Review.itemsOnly(srs) }

    GuideTour("practice")
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Panel(Modifier.fillMaxWidth().guideTarget("review-card")) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Revision", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                        Text(
                            if (dueToday == 0) "Your memory is up to date" else "≈ ${minutesFor(dueToday)} min · weakest first",
                            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant
                        )
                    }
                    // Only today's share is shown, never the whole backlog.
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$dueToday", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = scheme.primary, lineHeight = 38.sp)
                        Text("due today", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                }
                if (items.isNotEmpty()) {
                    Text(
                        "Memory strength · ${items.size} item${if (items.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    MemoryStrength(items)
                }
                Text(
                    when {
                        learned.isEmpty() -> "Finish a lesson and what you learn will come back here for review."
                        plan.capReached && grammarDue > 0 ->
                            "You've done today's ${Config.reviewDailyCap} word reviews. $grammarDue grammar ${if (grammarDue == 1) "point is" else "points are"} still due."
                        plan.capReached -> "You've done today's ${Config.reviewDailyCap} reviews. Great work! The rest will be here tomorrow."
                        plan.welcomeBack -> "Welcome back! Catch-up mode: these are today's reviews, weakest words first. " +
                            "The rest are spread over the next ${plan.daysToClear - 1} days."
                        plan.catchUp -> "Catch-up mode: weakest words first. About ${plan.daysToClear} days to catch up."
                        plan.backlog == 0 && grammarDue > 0 ->
                            "Some grammar points are starting to fade. Reviewing them now makes each one last longer."
                        plan.backlog == 0 -> "Nothing is due. Each word comes back just before you'd forget it."
                        else -> "These words are starting to fade. Reviewing them now makes each memory last longer."
                    },
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 16.dp)
                )
                // Due grammar alone is enough to start a revision.
                val count = minOf(plan.today.size, SESSION_SIZE) + grammarDue
                PushButton(
                    if (count == 0) "Nothing due" else "Start revision · $count",
                    onClick = { onReview("Revision", Review.pick(true, plan, srs, SESSION_SIZE), true) },
                    enabled = count > 0
                )
                PushButton(
                    "Practise weakest words",
                    onClick = {
                        // Anything learned, the least remembered first, so extra practice goes where it helps most.
                        onReview("Practice", Review.pick(false, plan, srs, SESSION_SIZE), false)
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    enabled = learned.isNotEmpty(),
                    color = scheme.secondary
                )
            }
        }
        Activity(Icons.Rounded.Style, "Words & flashcards", "Look up every word in the course and flip through cards.", onWords)
        Activity(
            Icons.AutoMirrored.Rounded.MenuBook, "Stories",
            "Short stories written with the words you know. Unlocked: ${Stories.unlocked(progress.done).size} of ${Stories.all.size}.",
            onStories
        )
        TalkCard(progress.done, onTalk)
        Activity(Icons.Rounded.Mic, "Speaking", "Say everyday phrases aloud and get them checked.", onSpeak)
        Box(Modifier.guideTarget("writing-practice")) {
            Activity(
                Icons.Rounded.Edit, "Writing practice",
                when {
                    writePlan.capReached -> "You've done today's ${Config.reviewDailyCap} writing reviews. More tomorrow."
                    writePlan.welcomeBack -> "Welcome back! ${writePlan.today.size} to write today, weakest first. " +
                        "The rest are spread over the next ${writePlan.daysToClear - 1} days."
                    writePlan.catchUp -> "Catch-up mode: ${writePlan.today.size} to write today, weakest first. About ${writePlan.daysToClear} days to catch up."
                    writePlan.today.isNotEmpty() -> "${writePlan.today.size} due for writing. Write each word from its meaning and sound."
                    writeNew.isNotEmpty() -> "Nothing due. Start writing words you already read: ${writeNew.size} to go."
                    progress.writeSrs.isNotEmpty() -> "Nothing is due for writing right now. Come back later."
                    else -> "Finish a lesson first; words come here to be written by hand."
                }
            ) {
                // Due reviews first; when none are due, words learned for reading but never written.
                val items = writePlan.today.take(Config.writingSessionSize).shuffled()
                    .ifEmpty { if (writePlan.capReached) emptyList() else writeNew.shuffled().take(Config.writingSessionSize) }
                if (items.isNotEmpty()) onWriting(items)
            }
        }
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

/**
 * The guide's spoken mini-conversations. Unfinished talks come first and only the next few show, so the card stays
 * short as more unlock; "Show all" lists the rest in place.
 */
@Composable
private fun TalkCard(done: Set<String>, onTalk: (GuideTalk) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val open = Talks.unlocked(done).sortedBy { it.id in done }
    var all by rememberSaveable { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
                    GuideCharacter("wave", 36.dp)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Talk with ${Guide.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(
                        if (open.isEmpty()) "Finish ${Course.units.firstOrNull { it.id == Talks.all.firstOrNull()?.unlockAfter }?.title.orEmpty()} to unlock your first talk."
                        else "Answer ${Guide.name}'s questions out loud. Unlocked: ${open.size} of ${Talks.all.size}.",
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant
                    )
                }
            }
            (if (all) open else open.take(TALKS_SHOWN)).forEach { talk ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(CircleShape).clickable(role = Role.Button) { onTalk(talk) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(talk.title, Modifier.weight(1f), color = scheme.onSurface, fontWeight = FontWeight.SemiBold)
                    if (talk.id in done) Icon(Icons.Rounded.CheckCircle, contentDescription = "Finished", tint = scheme.tertiary)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                }
            }
            if (open.size > TALKS_SHOWN) {
                TextButton(onClick = { all = !all }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (all) "Show fewer" else "Show all ${open.size}")
                }
            }
        }
    }
}

private const val TALKS_SHOWN = 3

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

/** About eight seconds an answer, rounded up to whole minutes. */
private fun minutesFor(items: Int) = ((items.coerceAtMost(SESSION_SIZE) * 8 + 59) / 60).coerceAtLeast(1)
