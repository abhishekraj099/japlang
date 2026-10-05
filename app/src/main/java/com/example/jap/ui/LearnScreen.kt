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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.pointer.pointerInput
import com.example.jap.data.Kind
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.jap.data.LessonType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.CourseUnit
import com.example.jap.data.Daily
import com.example.jap.data.Lesson
import com.example.jap.data.Progress

private val CardShape = RoundedCornerShape(20.dp)
private const val KANJI_DIGITS = "一二三四五六七八九"

/** A number in kanji numerals, e.g. 十四 for 14. */
private fun kanjiNumber(n: Int): String = when {
    n <= 0 -> "$n"
    n < 10 -> "${KANJI_DIGITS[n - 1]}"
    n < 100 -> (if (n / 10 > 1) "${KANJI_DIGITS[n / 10 - 1]}" else "") + "十" + (if (n % 10 > 0) "${KANJI_DIGITS[n % 10 - 1]}" else "")
    else -> "$n"
}

/** "第三課" for unit 3: the way chapters are numbered in a Japanese textbook. */
private fun chapter(number: Int) = "第${kanjiNumber(number)}課"

/** Practice stops are round; the others (theory, speaking, writing, review…) are rounded squares. */
private fun stopShape(lesson: Lesson): Shape =
    if (lesson.type == LessonType.PRACTICE || lesson.type == LessonType.KANA) CircleShape else RoundedCornerShape(24.dp)

@Composable
fun LearnScreen(
    progress: Progress,
    onOpen: (Lesson) -> Unit,
    onTestOut: (CourseUnit) -> Unit,
    onChallenge: () -> Unit,
    onStreak: () -> Unit,
    onKanaGame: (Kind) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = Course.courses.firstOrNull { it.id == progress.trackId } ?: Course.courses.first()
    val levels = track.levels.map { Course.unitsOf(it) }
    // Units are numbered through the whole course, like chapters of a book.
    val numbering = levels.flatten().filterNot { it.alphabet }
    var script by rememberSaveable { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<CourseUnit?>(null) }
    var picker by remember { mutableStateOf(false) }
    val next = numbering.flatMap { it.lessons }.firstOrNull { it.id !in progress.done && Course.unlocked(it, progress.done) }
    val scripts = levels.flatten().filter { it.alphabet }
    val kana = scripts.getOrNull(script.coerceAtMost(scripts.lastIndex))

    // The whole course is one path. Note where each level starts in the list so the label can
    // follow the scroll and picking a level can jump to it. Index 0 is the Today card.
    val starts = remember(track.id, script) {
        var at = 1
        levels.map { units ->
            val start = at
            if (units.any { it.alphabet }) at += 1 + (kana?.lessons?.size ?: 0)
            units.filterNot { it.alphabet }.forEach { at += 1 + it.lessons.size }
            start
        }
    }
    val list = rememberLazyListState()
    val visible by remember(starts) {
        derivedStateOf { starts.indexOfLast { it <= list.firstVisibleItemIndex + 1 }.coerceAtLeast(0) }
    }
    // Open where the learner left off: the level they picked, otherwise the first with unfinished work.
    LaunchedEffect(track.id) {
        val target = progress.levelIndex.takeIf { it in levels.indices }
            ?: levels.indexOfFirst { units -> units.any { u -> u.lessons.any { it.id !in progress.done } } }.coerceAtLeast(0)
        if (target > 0) list.scrollToItem(starts[target])
    }
    val scope = rememberCoroutineScope()
    val label = if (track.levels.size > 1) "${track.title} · ${track.levels[visible].name}" else track.title

    Column(modifier.fillMaxSize()) {
        TopBar(progress, label, onPick = { picker = true }, onStreak = onStreak, onSettings = onSettings)
        // A sideways swipe while the alphabet is on screen switches between hiragana and katakana.
        val alphabetShown = scripts.isNotEmpty() && visible == 0 && levels.firstOrNull()?.any { it.alphabet } == true
        LazyColumn(
            Modifier
                .fillMaxSize()
                .pointerInput(alphabetShown, scripts.size) {
                    if (!alphabetShown) return@pointerInput
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onHorizontalDrag = { _, delta -> dx += delta },
                        onDragEnd = {
                            val threshold = 60.dp.toPx()
                            if (dx < -threshold) script = (script + 1).coerceAtMost(scripts.lastIndex)
                            if (dx > threshold) script = (script - 1).coerceAtLeast(0)
                        }
                    )
                },
            state = list,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            item("today") { TodayCard(progress, onChallenge) }
            levels.forEach { units ->
                if (kana != null && units.any { it.alphabet }) {
                    val nextKana = kana.lessons.firstOrNull { it.id !in progress.done }
                    item("alphabet") {
                        AlphabetHeader(
                            units.filter { it.alphabet }, script, progress.done,
                            onScript = { script = it }, onList = { sheet = kana },
                            onGame = { onKanaGame(kana.lessons.firstNotNullOfOrNull { l -> l.items.firstOrNull()?.kind } ?: Kind.HIRAGANA) }
                        )
                    }
                    itemsIndexed(kana.lessons, key = { _, lesson -> lesson.id }) { i, lesson ->
                        Stop(lesson, i, kana, progress.done, nextKana, onOpen)
                    }
                }
                units.filterNot { it.alphabet }.forEach { unit ->
                    item(unit.id) {
                        UnitHeader(numbering.indexOf(unit) + 1, unit, progress.done, onList = { sheet = unit }, onTestOut = { onTestOut(unit) })
                    }
                    itemsIndexed(unit.lessons, key = { _, lesson -> lesson.id }) { i, lesson ->
                        Stop(lesson, i, unit, progress.done, next, onOpen)
                    }
                }
            }
        }
    }

    if (picker) {
        CourseSheet(track.id, visible, progress.done, onDismiss = { picker = false }, onPick = { id, level ->
            picker = false
            progress.selectLevel(id, level)
            // Same course: jump to the level. Another course opens there through LaunchedEffect above.
            if (id == track.id) scope.launch { list.animateScrollToItem(starts[level]) } else script = 0
        })
    }

    sheet?.let { unit ->
        UnitSheet(unit, progress.done, onDismiss = { sheet = null }, onOpen = { lesson ->
            sheet = null
            onOpen(lesson)
        })
    }
}

@Composable
private fun Stop(lesson: Lesson, i: Int, unit: CourseUnit, done: Set<String>, next: Lesson?, onOpen: (Lesson) -> Unit) {
    PathStop(
        glyph = lesson.icon,
        label = lesson.title,
        color = Color(unit.color),
        state = when {
            lesson.id in done -> StopState.DONE
            lesson == next -> StopState.CURRENT
            Course.unlocked(lesson, done) -> StopState.OPEN
            else -> StopState.LOCKED
        },
        swing = pathSwing(i),
        nextSwing = if (i < unit.lessons.lastIndex) pathSwing(i + 1) else null,
        shape = stopShape(lesson),
        onClick = { onOpen(lesson) }
    )
}

/** Header of the alphabet section, with a switch between hiragana and katakana. */
@Composable
private fun AlphabetHeader(
    units: List<CourseUnit>, script: Int, done: Set<String>,
    onScript: (Int) -> Unit, onList: () -> Unit, onGame: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val unit = units[script.coerceAtMost(units.lastIndex)]
    val tint = Color(unit.color)
    Panel(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), fill = tint.copy(alpha = 0.08f).compositeOver(scheme.surface), line = tint.copy(alpha = 0.25f)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onList).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Alphabet", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                    Text(
                        "${unit.lessons.count { it.id in done }}/${unit.lessons.size} lessons",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                Row(Modifier.clip(CircleShape).background(scheme.surface).padding(4.dp)) {
                    units.forEachIndexed { i, u ->
                        val selected = i == script
                        Text(
                            u.title.take(4),
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else scheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) Color(u.color) else Color.Transparent)
                                .clickable(role = Role.Tab) { onScript(i) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Swipe to switch",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tint)
                        .clickable(role = Role.Button, onClick = onGame)
                        .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        "Kana game",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/** Every lesson of a unit with what it covers; opened from the unit header. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSheet(unit: CourseUnit, done: Set<String>, onDismiss: () -> Unit, onOpen: (Lesson) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = scheme.surface) {
        Text(
            unit.title,
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${unit.lessons.count { it.id in done }}/${unit.lessons.size} lessons",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        HorizontalDivider(color = scheme.outlineVariant)
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(unit.lessons, key = { it.id }) { lesson ->
                val open = Course.unlocked(lesson, done)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = open, role = Role.Button) { onOpen(lesson) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(stopShape(lesson))
                            .background(if (open) Color(unit.color).copy(alpha = 0.15f) else scheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (open) {
                            Text(lesson.icon, fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(unit.color))
                        } else {
                            Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(lesson.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (open) scheme.onSurface else scheme.onSurfaceVariant)
                        if (lesson.summary.isNotEmpty()) {
                            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    if (lesson.id in done) Icon(Icons.Rounded.CheckCircle, contentDescription = "Finished", tint = scheme.tertiary)
                }
            }
        }
    }
}

@Composable
private fun TopBar(progress: Progress, label: String, onPick: () -> Unit, onStreak: () -> Unit, onSettings: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                .clickable(role = Role.DropdownList, onClickLabel = "Change course", onClick = onPick)
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
        }
        Spacer(Modifier.width(8.dp))
        Pill(onStreak) {
            SakuraIcon(20.dp)
            Text("${progress.streak}", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.padding(start = 4.dp))
        }
        Pill(onStreak, Modifier.padding(start = 8.dp)) {
            DropIcon(18.dp)
            Text("${progress.coins}", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.padding(start = 4.dp))
        }
        Box(
            Modifier
                .padding(start = 8.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Settings", onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Pill(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(CircleShape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** Daily goal, the day's challenge and quests, folded into one card above the path. */
@Composable
private fun TodayCard(progress: Progress, onChallenge: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val quests = Daily.quests(progress)
    var expanded by rememberSaveable { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (progress.todayXp / progress.goal.toFloat()).coerceAtMost(1f) },
                        modifier = Modifier.size(48.dp),
                        color = scheme.secondary,
                        trackColor = scheme.surfaceVariant,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text("${progress.todayXp.coerceAtMost(progress.goal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text("Today", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    Text(
                        "${progress.todayXp.coerceAtMost(progress.goal)} of ${progress.goal} XP · ${quests.count { it.done }}/${quests.size} quests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                if (progress.challengeDoneToday) {
                    Icon(Icons.Rounded.Check, contentDescription = "Challenge done", tint = scheme.tertiary)
                } else {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.secondary)
                            .clickable(role = Role.Button, onClick = onChallenge)
                            .padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = scheme.onSecondary, modifier = Modifier.size(18.dp))
                        Text("Challenge", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.onSecondary)
                    }
                }
            }
            HorizontalDivider(color = scheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's quests", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    quests.forEach { quest ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (quest.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (quest.done) "Done" else "Not done",
                                tint = if (quest.done) scheme.tertiary else scheme.outline
                            )
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Row {
                                    Text(quest.title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
                                    Text("${quest.current.coerceAtMost(quest.target)}/${quest.target}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                                }
                                Meter(quest.current / quest.target.toFloat(), if (quest.done) scheme.tertiary else scheme.primary, Modifier.padding(top = 6.dp), height = 5.dp)
                            }
                        }
                    }
                    Text(
                        "Daily challenge: +${Config.challengeXp} XP and +${Config.challengeCoins} drops",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitHeader(number: Int, unit: CourseUnit, done: Set<String>, onList: () -> Unit, onTestOut: () -> Unit) {
    val finished = unit.lessons.count { it.id in done }
    Column(
        Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(Color(unit.color), Color(unit.dark))))
            .waves(Color.White.copy(alpha = 0.09f), radius = 26.dp)
            .clickable(onClickLabel = "Show lessons", onClick = onList)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(unit.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(unit.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                Text(
                    "$finished/${unit.lessons.size} lessons",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            // Chapter number set vertically, as on the spine of a Japanese book.
            Column(Modifier.padding(start = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                chapter(number).forEach { glyph ->
                    Text("$glyph", fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).clip(CircleShape).background(Color.Black.copy(alpha = 0.22f))) {
                Box(
                    Modifier
                        .fillMaxWidth(finished / unit.lessons.size.toFloat())
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            if (finished < unit.lessons.size) {
                Row(
                    Modifier
                        .padding(start = 12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(role = Role.Button, onClick = onTestOut)
                        .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null, tint = Color(unit.dark), modifier = Modifier.size(16.dp))
                    Text(
                        "Test out",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(unit.dark),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
