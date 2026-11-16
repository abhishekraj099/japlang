package com.example.jap.ui

import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.example.jap.audio.FAST_RATE
import com.example.jap.audio.NORMAL_RATE
import com.example.jap.audio.SLOW_RATE
import com.example.jap.audio.VoiceChoice
import androidx.compose.material3.RadioButton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.Reminder
import com.example.jap.audio.LocalSpeaker
import com.example.jap.audio.LocalVoice
import com.example.jap.data.BadgeRule
import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.CrashLog
import com.example.jap.data.Guide
import com.example.jap.data.League
import com.example.jap.data.Progress
import com.example.jap.data.Skill
import com.example.jap.data.Skills
import com.example.jap.data.Tip
import com.example.jap.ui.theme.Kin
import com.example.jap.ui.theme.Kon
import com.example.jap.ui.theme.Nami
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Where the shortcuts on the Me tab lead. */
enum class MeLink { TROPHIES, TIPS, RANKING, SETTINGS }

private val links = listOf(
    Triple(MeLink.TROPHIES, Icons.Rounded.EmojiEvents, "Trophies"),
    Triple(MeLink.TIPS, Icons.Rounded.Lightbulb, "Tips"),
    Triple(MeLink.RANKING, Icons.Rounded.Leaderboard, "Ranking"),
    Triple(MeLink.SETTINGS, Icons.Rounded.Settings, "Settings")
)

@Composable
fun MeScreen(progress: Progress, onLink: (MeLink) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GuideTour("me")
        Box {
            ProfileHeader(progress)
            GuideHelpButton("me", Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        Panel(Modifier.fillMaxWidth().guideTarget("me-links")) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                links.filter { it.first != MeLink.RANKING || progress.rankingOn }.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { (link, icon, label) ->
                            Column(
                                Modifier.weight(1f).clickable(role = Role.Button) { onLink(link) }.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.size(48.dp).clip(CircleShape).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer)
                                }
                                Text(label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
        WeekCard(progress)
        SkillsCard(progress)
        RankLadder(progress)
        Section("Trophies in progress", "See all") { onLink(MeLink.TROPHIES) }
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                val next = Config.badges.filterNot(progress::earned).sortedByDescending(progress::progressOf).take(3)
                if (next.isEmpty()) {
                    Text("Every trophy earned. Well done!", color = scheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
                next.forEachIndexed { i, badge ->
                    BadgeRow(badge, progress)
                    if (i < next.lastIndex) HorizontalDivider(color = scheme.outlineVariant)
                }
            }
        }
        if (progress.rankingOn) {
            Section("Ranking", "See more") { onLink(MeLink.RANKING) }
            RankingPreview(progress)
        }
        Section("Learning tips", "See all") { onLink(MeLink.TIPS) }
        Course.tips.take(2).forEach { TipCard(it, progress) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileHeader(progress: Progress) {
    val rank = Config.rank(progress.xp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Kon)
            .waves(Nami.copy(alpha = 0.10f), 26.dp)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Nami), contentAlignment = Alignment.Center) {
            Text(progress.name.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Kon)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(progress.name, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(progress.email, style = MaterialTheme.typography.bodySmall, color = Nami.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${progress.xp} XP · ${rank.japanese} ${rank.name}",
                style = MaterialTheme.typography.labelLarge,
                color = Kin,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Right answers per skill over the last [Skills.WINDOW]; "–" until there are enough to mean anything. */
@Composable
private fun SkillsCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your skills", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Skill.entries.forEach { skill ->
                val percent = Skills.percent(progress.skillLog[skill].orEmpty())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(skill.label, Modifier.width(84.dp), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    Meter((percent ?: 0) / 100f, scheme.primary, Modifier.weight(1f), height = 10.dp)
                    Text(
                        percent?.let { "$it%" } ?: "–", Modifier.width(48.dp), textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** This week's XP per day as bars, with today's totals. */
@Composable
private fun WeekCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val today = progress.day
    val monday = today - progress.weekDay
    val days = (0 until 7).map { monday + it }
    val most = days.maxOf { progress.stats(it).xp }.coerceAtLeast(progress.goal)
    val stat = progress.stats(today)
    val accuracy = if (stat.answered == 0) 0 else stat.correct * 100 / stat.answered
    val dateLabel = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date())
    val names = listOf("M", "T", "W", "T", "F", "S", "S")

    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("This week", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Text("Today, $dateLabel", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Text("${stat.xp} XP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(top = 10.dp))
                Text("$accuracy% correct", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Row(Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, day ->
                    val xp = progress.stats(day).xp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(14.dp).height(96.dp).clip(CircleShape).background(scheme.surfaceVariant), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((xp / most.toFloat()).coerceIn(0f, 1f))
                                    .clip(CircleShape)
                                    .background(if (day == today) scheme.secondary else scheme.primary)
                            )
                        }
                        Text(
                            names[i],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (day == today) scheme.secondary else scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Titles earned by total XP, from newcomer to master. */
@Composable
private fun RankLadder(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val ranks = Config.ranks
    val current = ranks.indexOf(Config.rank(progress.xp))
    val nextRank = ranks.getOrNull(current + 1)
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rank", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
                Text(
                    nextRank?.let { "${it.xp - progress.xp} XP to ${it.name}" } ?: "Highest rank",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Row(Modifier.padding(top = 14.dp)) {
                ranks.forEachIndexed { i, rank ->
                    val reached = i <= current
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(if (i == current) 40.dp else 32.dp)
                                .clip(CircleShape)
                                .background(if (reached) scheme.primary else scheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                rank.japanese.take(1),
                                fontFamily = FontFamily.Serif,
                                fontSize = if (i == current) 18.sp else 14.sp,
                                color = if (reached) scheme.onPrimary else scheme.onSurfaceVariant
                            )
                        }
                        Text(
                            rank.japanese,
                            fontFamily = FontFamily.Serif,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (reached) scheme.onSurface else scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, action: String, onAction: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onBackground, modifier = Modifier.weight(1f))
        Text(action, color = scheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(role = Role.Button, onClick = onAction))
    }
}

@Composable
private fun BadgeRow(badge: BadgeRule, progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val earned = progress.earned(badge)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (earned) Kin else scheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (earned) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = if (earned) "Earned" else "Locked",
                tint = if (earned) Color.White else scheme.onSurfaceVariant
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(badge.title, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(badge.how, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            if (!earned) Meter(progress.progressOf(badge), scheme.secondary, Modifier.padding(top = 8.dp), height = 5.dp)
        }
    }
}

/** Top of this week's table, with how each place moved since yesterday. */
@Composable
private fun RankingPreview(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val today = League.table(progress.week, progress.weekDay, progress.name, progress.weekXp)
    val yesterday = if (progress.weekDay == 0) today else League.table(progress.week, progress.weekDay - 1, progress.name, progress.weekXp)
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            today.take(5).forEachIndexed { i, rival ->
                val before = yesterday.indexOfFirst { it.name == rival.name }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            before > i -> Icons.Rounded.KeyboardArrowUp
                            before < i -> Icons.Rounded.KeyboardArrowDown
                            else -> Icons.Rounded.Remove
                        },
                        contentDescription = null,
                        tint = when {
                            before > i -> scheme.tertiary
                            before < i -> scheme.error
                            else -> scheme.outline
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text("${i + 1}", fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Box(Modifier.size(34.dp).clip(CircleShape).background(if (rival.you) scheme.primary else scheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(rival.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (rival.you) scheme.onPrimary else scheme.onPrimaryContainer)
                    }
                    Text(
                        if (rival.you) "${rival.name} (you)" else rival.name,
                        fontWeight = if (rival.you) FontWeight.Bold else FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Text("${rival.xp} XP", color = scheme.onSurfaceVariant)
                }
            }
            Text(
                SAMPLE_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TipCard(tip: Tip, progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val liked = tip.title in progress.favouriteTips
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(tip.title, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Text(tip.body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = { progress.toggleTip(tip.title) }) {
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (liked) "Remove from favourites" else "Add to favourites",
                    tint = if (liked) scheme.secondary else scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrophiesScreen(progress: Progress, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SubScreen("Trophies", onBack) {
        val earned = Config.badges.count(progress::earned)
        Text(
            "$earned of ${Config.badges.size} earned",
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Config.badges) { badge -> Panel(Modifier.fillMaxWidth()) { BadgeRow(badge, progress) } }
        }
    }
}

@Composable
fun TipsScreen(progress: Progress, onBack: () -> Unit) {
    SubScreen("Learning tips", onBack) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Favourites first.
            items(Course.tips.sortedByDescending { it.title in progress.favouriteTips }) { TipCard(it, progress) }
        }
    }
}

@Composable
fun SettingsScreen(progress: Progress, onRetakeTest: () -> Unit, onLegal: (LegalDoc) -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val voice = LocalVoice.current
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val crashes = remember { CrashLog.read() }

    fun setReminder(on: Boolean) {
        progress.updateReminder(on)
        if (on) Reminder.schedule(context) else Reminder.cancel(context)
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) setReminder(true)
    }

    SubScreen("Settings", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Daily goal", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    Text("${Config.goal(progress.goal)?.name ?: "Custom"} · ${progress.goal} XP a day", color = scheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Config.goals.forEach { option ->
                            OptionCard(
                                "${option.xp} XP",
                                if (progress.goal == option.xp) OptionState.SELECTED else OptionState.IDLE,
                                { progress.updateGoal(option.xp) },
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    Text("Follow your phone, or always use light or dark.", color = scheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                            OptionCard(
                                label,
                                if (progress.theme == mode) OptionState.SELECTED else OptionState.IDLE,
                                { progress.updateTheme(mode) },
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    VoiceRow(
                        "Japanese voice", "Who reads words and sentences in lessons.",
                        speaker.voices, progress.japaneseVoice, defaultLabel = "Phone default",
                        onPick = { key ->
                            progress.updateJapaneseVoice(key)
                            speaker.useJapaneseVoice(progress)
                            speaker.preview(VOICE_SAMPLE)
                        },
                        onMore = { runCatching { context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) } }
                    )
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        Text("Speed", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(SLOW_RATE to "Slow", NORMAL_RATE to "Normal", FAST_RATE to "Natural").forEach { (rate, label) ->
                                OptionCard(
                                    label,
                                    if (progress.speechRate == rate) OptionState.SELECTED else OptionState.IDLE,
                                    {
                                        progress.updateSpeechRate(rate)
                                        speaker.useRate(rate)
                                        speaker.preview(VOICE_SAMPLE)
                                    },
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            if (Guide.characters.size > 1) {
                Panel(Modifier.fillMaxWidth()) {
                  Column(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Your guide", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Text("Who shows you around and answers your questions.", color = scheme.onSurfaceVariant)
                        GuidePicker(progress, Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    // One switch for every guide: when off, tips and answers are shown as text only.
                    SwitchRow(
                        Icons.Rounded.RecordVoiceOver, "Guide voice",
                        "${Guide.characters.joinToString(" and ") { it.name }} read tips and answers aloud. Off: text only.",
                        progress.guideVoice, progress::updateGuideVoice
                    )
                    if (progress.guideVoice) {
                        TextButton(
                            onClick = {
                                voice.enabled = true
                                voice.useGuideVoice(progress)
                                voice.say("Hi, I'm ${Guide.name}! This is how I sound.")
                            },
                            modifier = Modifier.padding(start = 52.dp, bottom = 8.dp)
                        ) { Text("Hear ${Guide.name}") }
                        HorizontalDivider(color = scheme.outlineVariant)
                        val own = Guide.CHARACTER_VOICE + Guide.current.id
                        VoiceRow(
                            "${Guide.name}'s voice", "The English voice tips and answers are read in.",
                            voice.voices, progress.guideVoiceName.ifEmpty { own }, defaultLabel = null,
                            onPick = { key ->
                                // Their own voice is stored as the default, so it follows the guide if they switch.
                                progress.updateGuideVoiceName(if (key == own) "" else key)
                                voice.useGuideVoice(progress)
                                voice.enabled = true
                                voice.say("Hi, I'm ${Guide.name}! This is how I sound.")
                            },
                            onMore = { runCatching { context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) } }
                        )
                    }
                  }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = scheme.onSurfaceVariant)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text("Daily reminder", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            Text("A nudge around ${Config.reminderHour}:00 to keep your streak.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = progress.reminder,
                            onCheckedChange = { on ->
                                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                if (on && needsPermission) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else setReminder(on)
                            }
                        )
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(Icons.Rounded.Face, "Guide tips", "${Guide.name} shows you around each screen the first time. Tap ? to see the tips again.", progress.guideOn, progress::updateGuide)
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(Icons.Rounded.MusicNote, "Sound effects", "A chime when you answer correctly.", progress.soundEffects, progress::updateSoundEffects)
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(Icons.Rounded.Leaderboard, "Weekly ranking", "Show the points table. The other learners are samples for now.", progress.rankingOn, progress::updateRanking)
                    HorizontalDivider(color = scheme.outlineVariant)
                    Action(Icons.Rounded.Quiz, "Retake the placement test", "Skip ahead if you have learned elsewhere.", onRetakeTest)
                    HorizontalDivider(color = scheme.outlineVariant)
                    Action(Icons.Rounded.RestartAlt, "Reset progress", "Erase XP, streak, drops and lessons on this device.") { confirmReset = true }
                    HorizontalDivider(color = scheme.outlineVariant)
                    Action(Icons.AutoMirrored.Rounded.Logout, "Sign out", progress.email) { confirmSignOut = true }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Privacy & data", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
                    Text(
                        "Your progress is stored only on this phone.", style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp)
                    )
                    Action(Icons.Rounded.Policy, "Privacy policy", "What the app stores and what it shares") { onLegal(LegalDoc.PRIVACY) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    Action(Icons.Rounded.Description, "Terms of use", "The rules for using Manabi") { onLegal(LegalDoc.TERMS) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    Action(Icons.Rounded.Copyright, "Licenses", "Stroke order data and open-source libraries") { onLegal(LegalDoc.LICENSES) }
                    if (crashes.isNotBlank()) {
                        HorizontalDivider(color = scheme.outlineVariant)
                        Action(Icons.Rounded.BugReport, "Send a crash report", "Share the error details saved on this phone") {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, "Manabi crash report")
                                .putExtra(Intent.EXTRA_TEXT, crashes)
                            if (Config.contactEmail.isNotBlank()) send.putExtra(Intent.EXTRA_EMAIL, arrayOf(Config.contactEmail))
                            runCatching { context.startActivity(Intent.createChooser(send, null)) }
                        }
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    Action(Icons.Rounded.DeleteForever, "Delete all my data", "Account, progress, settings and logs") { confirmDelete = true }
                }
            }
            if (!speaker.available) {
                Panel(Modifier.fillMaxWidth(), fill = scheme.errorContainer, line = scheme.error) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("No Japanese voice found", fontWeight = FontWeight.SemiBold, color = scheme.onErrorContainer)
                        Text(
                            "Audio is off because this device has no Japanese text-to-speech voice. Install one and restart the app.",
                            color = scheme.onErrorContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        TextButton(onClick = {
                            // Not every device ships a TTS engine that handles this intent.
                            runCatching { context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) }
                        }) { Text("Install voice data") }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all progress?") },
            text = { Text("This erases your XP, streak, drops, omamori, finished lessons and review history. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    progress.reset()
                    confirmReset = false
                }) { Text("Reset", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all your data?") },
            text = { Text("This erases your account details, progress, streak, settings, usage log and crash log from this phone. The app will restart as if newly installed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    progress.deleteEverything()
                    // Start again from a clean process, so nothing held in memory survives.
                    val restart = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    restart?.let(context::startActivity)
                    Runtime.getRuntime().exit(0)
                }) { Text("Delete everything", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your progress stays on this device and is there when you sign back in.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    progress.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

/** Said when a Japanese voice or speed is picked: "Hello, let's study Japanese." */
private const val VOICE_SAMPLE = "こんにちは。いっしょに日本語を勉強しましょう。"

/**
 * One voice setting: the voice in use, and a list to pick another. The guides' own voices come first, with their
 * picture, then every voice installed on the phone; each pick is played straight away so the learner hears it.
 * A key is [Guide.CHARACTER_VOICE] plus a guide id, an engine voice id, or empty for [defaultLabel] (left out when null).
 */
@Composable
private fun VoiceRow(
    title: String,
    body: String,
    voices: List<VoiceChoice>,
    selected: String,
    defaultLabel: String?,
    onPick: (String) -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val current = Guide.characters.firstOrNull { Guide.CHARACTER_VOICE + it.id == selected }?.let { "${it.name}'s voice" }
        ?: voices.firstOrNull { it.name == selected }?.label
        ?: defaultLabel.orEmpty()
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Choose $title") { open = true }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = scheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Text(current, color = scheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
    if (open) VoiceDialog(title, voices, selected, defaultLabel, onPick, onMore) { open = false }
}

/** The list of voices for [VoiceRow]: the guides' own voices with their picture, then the phone's voices. */
@Composable
private fun VoiceDialog(
    title: String,
    voices: List<VoiceChoice>,
    selected: String,
    defaultLabel: String?,
    onPick: (String) -> Unit,
    onMore: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Tap a voice to hear it.", style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)
                )
                Guide.characters.forEach { c ->
                    VoiceOption(Guide.CHARACTER_VOICE + c.id == selected, "${c.name}'s voice", c.voice, c.id) {
                        onPick(Guide.CHARACTER_VOICE + c.id)
                    }
                }
                Text(
                    "Other voices on this phone", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                defaultLabel?.let { VoiceOption(selected.isEmpty(), it, "") { onPick("") } }
                voices.forEach { v -> VoiceOption(v.name == selected, v.label, if (v.online) "Needs internet" else "") { onPick(v.name) } }
                TextButton(onClick = onMore, modifier = Modifier.padding(top = 4.dp)) { Text("Get more voices") }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
}

/** One voice in [VoiceDialog]; [character] adds that guide's picture. */
@Composable
private fun VoiceOption(chosen: Boolean, label: String, detail: String, character: String? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (chosen) scheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = chosen, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(label, color = scheme.onSurface, fontWeight = if (character != null) FontWeight.SemiBold else FontWeight.Normal)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        if (character != null) GuideCharacter("wave", 56.dp, character = character)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, body: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun Action(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}
