package com.example.jap.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Progress
import java.text.DateFormatSymbols
import java.util.Calendar

private val Box14 = RoundedCornerShape(14.dp)

/**
 * Shown once before the main app: how the streak works, an optional streak goal,
 * and how an omamori protects a missed day.
 */
@Composable
fun StreakIntro(progress: Progress, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var page by rememberSaveable { mutableIntStateOf(0) }
    var choice by rememberSaveable { mutableIntStateOf(0) }

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            AnimatedContent(page, Modifier.weight(1f), label = "page") { current ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (current) {
                        0 -> {
                            SakuraTree(streak = 30, size = 220.dp)
                            IntroText(
                                "A lesson a day keeps your sakura blooming",
                                "Finish at least one lesson every day. Each day in a row adds to your streak and your sakura tree blooms a little more."
                            )
                        }
                        1 -> {
                            SakuraTree(streak = 0, size = 160.dp)
                            IntroText("Set a streak goal", "How many days in a row will you study? Reach it to earn drops.")
                            Config.streakGoals.forEachIndexed { i, goal ->
                                val selected = i == choice
                                Row(
                                    Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(Box14)
                                        .background(if (selected) scheme.primaryContainer else scheme.surface)
                                        .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outline, Box14)
                                        .clickable(role = Role.RadioButton) { choice = i }
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${goal.days} days",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("+${goal.reward}", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                                    DropIcon(22.dp, Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                        else -> {
                            OmamoriIcon(150.dp)
                            IntroText(
                                "Miss a day without losing your streak",
                                "An omamori shields your streak for one missed day. Earn drops by studying and exchange " +
                                    "${Config.protectionCost} of them for an omamori. You can carry up to ${Config.maxProtection}."
                            )
                        }
                    }
                }
            }
            PushButton(
                when (page) {
                    0 -> "Continue"
                    1 -> "Commit to my goal"
                    else -> "Start learning"
                },
                onClick = {
                    when (page) {
                        1 -> {
                            progress.commitStreakGoal(Config.streakGoals[choice].days)
                            page++
                        }
                        2 -> {
                            progress.finishStreakIntro()
                            onDone()
                        }
                        else -> page++
                    }
                }
            )
            if (page == 1) {
                TextButton(onClick = { page++ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Not now")
                }
            }
        }
    }
}

@Composable
private fun IntroText(title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = scheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp)
    )
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
}

private val Sky = Color(0xFFCFE6FA)
private val SkyFade = Color(0xFFF2F8FD)
private val Leaf = Color(0xFF6DB33F)
private val Pink = Color(0xFFE8829C)
private val Blue = Color(0xFF3D8FDB)

/** Streak details: the tree, this month's numbers, the streak goal, a calendar and the wallet. */
@Composable
fun StreakScreen(progress: Progress, onBack: () -> Unit, onStudy: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    // Months back from the current one.
    var monthsBack by rememberSaveable { mutableIntStateOf(0) }
    val month = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        add(Calendar.MONTH, -monthsBack)
    }
    val firstDay = Progress.dayOf(month)
    val length = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthDays = (firstDay until firstDay + length)
    val studiedThisMonth = monthDays.count { it in progress.studiedDays }
    val savedThisMonth = monthDays.count { it in progress.protectedDays }

    BackHandler(onBack = onBack)
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box {
                // Sky behind the top of the page; it scrolls away with the header.
                Canvas(Modifier.matchParentSize()) {
                    drawRect(Brush.verticalGradient(listOf(Sky, SkyFade, scheme.background)))
                    cloud(size.width * 0.06f, size.height * 0.26f, 1.1f)
                    cloud(size.width * 0.55f, size.height * 0.10f, 1.3f)
                    cloud(size.width * 0.70f, size.height * 0.48f, 1.0f)
                }
            Column(Modifier.statusBarsPadding().padding(bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                    Text(
                        "Streak",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    RoundButton(Icons.Rounded.IosShare, "Share") {
                        val text = "I'm on a ${progress.streak}-day Japanese study streak!"
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 20.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${progress.streak}", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                        Text("Day streak", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
                    }
                    SakuraTree(progress.streak, 150.dp)
                }
            }
            }
                Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatTile(Modifier.weight(1f), "$studiedThisMonth", "Days studied this month") { SakuraIcon(30.dp) }
                        StatTile(Modifier.weight(1f), "$savedThisMonth", "Days saved") { OmamoriIcon(30.dp) }
                    }
                    Card {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp)) {
                                SakuraIcon(30.dp, Modifier.align(Alignment.TopStart))
                                SakuraIcon(26.dp, Modifier.align(Alignment.BottomEnd))
                                SakuraIcon(22.dp, Modifier.align(Alignment.BottomStart))
                            }
                            Text(
                                when {
                                    progress.studiedToday -> "You've studied today. See you tomorrow!"
                                    progress.streak > 0 -> "Study today to keep your streak!"
                                    else -> "Start your study streak!"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                            )
                            if (!progress.studiedToday) {
                                Text(
                                    "Study now",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Leaf)
                                        .clickable(role = Role.Button, onClick = onStudy)
                                        .padding(horizontal = 18.dp, vertical = 11.dp)
                                )
                            }
                        }
                    }
                    GoalCard(progress)
                    CalendarCard(progress, month, firstDay, length, monthsBack, onMonth = { monthsBack = it })
                    WalletCard(progress)
                }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1B1B22))
    }
}

/** White card with a soft bottom edge, as used across the streak page. */
@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.outlineVariant)
            .padding(bottom = 3.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
    ) { content() }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String, icon: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(start = 8.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun GoalCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val goal = Config.streakGoals.firstOrNull { it.days == progress.streakGoal }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (goal == null || progress.goalClaimed) {
                Text(
                    if (goal != null) "Goal of ${goal.days} days reached. Pick a new one:" else "Set a streak goal",
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Config.streakGoals.forEach { option ->
                        OptionCard("${option.days}d", OptionState.IDLE, { progress.commitStreakGoal(option.days) }, Modifier.weight(1f))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Goal: ${goal.days} days in a row", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.weight(1f))
                    Text("+${goal.reward}", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    DropIcon(20.dp, Modifier.padding(start = 4.dp))
                }
                Meter(progress.streak / goal.days.toFloat(), scheme.secondary, Modifier.padding(top = 10.dp), height = 8.dp)
                Text(
                    "${progress.streak.coerceAtMost(goal.days)} of ${goal.days} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(progress: Progress, month: Calendar, firstDay: Long, length: Int, monthsBack: Int, onMonth: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val today = progress.day
    val monthName = DateFormatSymbols.getInstance().months[month.get(Calendar.MONTH)]
    // Monday-first column of the 1st.
    val lead = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$monthName ${month.get(Calendar.YEAR)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MonthButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", true) { onMonth(monthsBack + 1) }
                MonthButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", monthsBack > 0) { onMonth(monthsBack - 1) }
            }
            Row(Modifier.padding(top = 14.dp)) {
                weekdays.forEach {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            (0 until (lead + length + 6) / 7).forEach { week ->
                Row(Modifier.padding(top = 8.dp)) {
                    (0 until 7).forEach { column ->
                        val date = week * 7 + column - lead + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date in 1..length) {
                                val day = firstDay + date - 1
                                val studied = day in progress.studiedDays
                                val saved = day in progress.protectedDays
                                when {
                                    studied -> BlossomOutline(44.dp, Pink, filled = Pink.copy(alpha = 0.18f))
                                    saved -> BlossomOutline(44.dp, Blue, filled = Blue.copy(alpha = 0.12f))
                                    day == today -> Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.surfaceVariant))
                                }
                                Text(
                                    "$date",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (studied || day == today) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        studied -> Pink
                                        saved -> Blue
                                        day > today -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
                                        else -> scheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BlossomOutline(16.dp, Pink)
                Text("Studied", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                BlossomOutline(16.dp, Blue, Modifier.padding(start = 18.dp))
                Text("Saved by omamori", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun MonthButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .padding(start = 8.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(scheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) scheme.onSurface else scheme.outline)
    }
}

@Composable
private fun WalletCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropIcon(40.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("${progress.coins}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue)
                    Text("Water drops", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                OmamoriIcon(44.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("${progress.omamori} / ${Config.maxProtection}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue)
                    Text("Omamori", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = scheme.outlineVariant, modifier = Modifier.padding(vertical = 14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append("Exchange ")
                        withStyle(SpanStyle(color = Blue, fontWeight = FontWeight.Bold)) { append("${Config.protectionCost}") }
                        append(" drops for 1 omamori")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val can = progress.canBuyOmamori()
                Text(
                    "Exchange",
                    fontWeight = FontWeight.Bold,
                    color = if (can) Color.White else scheme.outline,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (can) Blue else scheme.surfaceVariant)
                        .clickable(enabled = can, role = Role.Button) { progress.buyOmamori() }
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                )
            }
            Text(
                "An omamori saves your streak on a day you miss.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
