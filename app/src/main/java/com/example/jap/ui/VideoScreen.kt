package com.example.jap.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.jap.data.Progress
import com.example.jap.data.Video
import com.example.jap.data.VideoSection
import com.example.jap.data.Videos

private val Pill = RoundedCornerShape(28.dp)
private const val AUTO_ADVANCE_MS = 5000L
private const val SHORTS_ON_HOME = 4

/** Colour for a difficulty label; unknown levels fall back to the theme. */
@Composable
fun levelColour(level: String): Color = when (level.lowercase()) {
    "easy" -> Color(0xFF3F9B5A)
    "medium" -> Color(0xFF3C6FD8)
    "hard" -> Color(0xFFD9573F)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun LevelPill(level: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val colour = levelColour(level)
    Text(
        level,
        color = if (onDark) Color.White else colour,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.clip(CircleShape).background(if (onDark) colour else colour.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/** The JLPT level, e.g. "N3", as a small outlined tag. */
@Composable
fun JlptTag(jlpt: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val ink = if (onDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        jlpt, color = ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
        modifier = modifier.clip(CircleShape).border(1.dp, ink.copy(alpha = 0.6f), CircleShape).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

fun clock(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)

/**
 * The Video tab: real Japanese clips with subtitles to read along and lines to say aloud.
 * Everything listed comes from [Videos.source], so a server can supply it later.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoScreen(
    progress: Progress,
    onOpen: (video: Video, speaking: Boolean) -> Unit,
    onSeeMore: (VideoSection) -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf<String?>(null) }
    val source = Videos.source

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Video", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = scheme.onBackground)
            Row(
                Modifier.weight(1f).padding(horizontal = 14.dp).height(50.dp).clip(Pill).background(scheme.surfaceVariant).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = scheme.onSurfaceVariant)
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (query.isEmpty()) Text("Search videos", color = scheme.onSurfaceVariant, maxLines = 1)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = scheme.onSurface, fontSize = 16.sp),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) Icon(
                    Icons.Rounded.Close, contentDescription = "Clear", tint = scheme.onSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).clickable { query = ""; focus.clearFocus() }
                )
            }
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(scheme.surfaceVariant).clickable(role = Role.Button, onClickLabel = "History", onClick = onHistory),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.History, contentDescription = "History", tint = scheme.onSurface) }
        }

        // Topic chips.
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TopicChip("✨", "All", topic == null) { topic = null } }
            items(source.topics(), key = { it.id }) { t -> TopicChip(t.icon, t.name, topic == t.id) { topic = t.id } }
        }

        when {
            query.isNotBlank() -> {
                val results = Videos.search(query)
                if (results.isEmpty()) Text(
                    "No videos match \"${query.trim()}\". Try a topic like anime or food.",
                    color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                VideoGrid(results, progress, onOpen)
            }
            topic != null -> VideoGrid(Videos.filter(topic = topic), progress, onOpen)
            else -> {
                val picks = source.recommended().sortedBy { it.id in progress.watched }
                if (picks.isNotEmpty()) {
                    val pager = rememberPagerState { picks.size }
                    val scope = rememberCoroutineScope()
                    val dragged by pager.interactionSource.collectIsDraggedAsState()
                    // Move on by itself every few seconds, pausing while the learner is swiping.
                    LaunchedEffect(dragged, pager.settledPage) {
                        if (dragged) return@LaunchedEffect
                        delay(AUTO_ADVANCE_MS)
                        pager.animateScrollToPage((pager.currentPage + 1) % picks.size)
                    }
                    HorizontalPager(
                        pager,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        pageSpacing = 12.dp,
                        // A short swipe is enough to turn the card.
                        flingBehavior = PagerDefaults.flingBehavior(pager, snapPositionalThreshold = 0.15f)
                    ) { page ->
                        HeroCard(picks[page], onOpen)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(picks.size) { i ->
                            Box(
                                Modifier.clip(CircleShape).clickable(onClickLabel = "Show recommendation ${i + 1}") {
                                    scope.launch { pager.animateScrollToPage(i) }
                                }.padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    Modifier.size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                                        .background(if (i == pager.currentPage) scheme.primary else scheme.outlineVariant)
                                )
                            }
                        }
                    }
                }
                source.sections().forEach { section ->
                    val list = Videos.inSection(section)
                    if (list.isEmpty()) return@forEach
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, modifier = Modifier.weight(1f))
                        Row(
                            Modifier.clip(CircleShape).clickable { onSeeMore(section) }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("See more", color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                        }
                    }
                    if (section.short) {
                        // Shorts: a 2 x 2 grid of tall cards.
                        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            list.take(SHORTS_ON_HOME).chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    row.forEach { v -> ShortCard(v, Modifier.weight(1f)) { onOpen(v, false) } }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(list, key = { it.id }) { v -> VideoCard(v, progress, Modifier.width(250.dp)) { onOpen(v, false) } }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun TopicChip(icon: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.clip(CircleShape).background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
            .clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Text(
            name, color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun Thumb(video: Video, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF1F2333))) {
        AsyncImage(model = video.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun HeroCard(video: Video, onOpen: (Video, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(26.dp)).clickable { onOpen(video, false) }) {
        Thumb(video, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to scheme.primary.darken(0.45f))))
        Text(
            "RECOMMENDED FOR YOU",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(bottomEnd = 18.dp)).background(scheme.secondary).padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(video.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                LevelPill(video.level, onDark = true)
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 14.dp).size(18.dp))
                Text(clock(video.seconds), color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                Text(
                    video.channel, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp).weight(1f, fill = false)
                )
            }
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().height(54.dp).clip(CircleShape).background(Color.White)
                    .clickable(role = Role.Button) { onOpen(video, true) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = scheme.primary)
                Text("Start speaking", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A thumbnail with the clip length, its title, difficulty and how much has been spoken. */
@Composable
fun VideoCard(video: Video, progress: Progress, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val spoken = video.speakable.count { progress.bestScore(video.id, it.index) != null }
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp))) {
            Thumb(video, Modifier.matchParentSize())
            Text(
                clock(video.seconds), color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 3.dp)
            )
            if (video.id in progress.watched) Icon(
                Icons.Rounded.CheckCircle, contentDescription = "Watched", tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp)
            )
        }
        Text(
            video.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 2,
            overflow = TextOverflow.Ellipsis, lineHeight = 21.sp, modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp)
        )
        Row(Modifier.padding(top = 6.dp, start = 2.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelPill(video.level)
            JlptTag(video.jlpt, Modifier.padding(start = 6.dp))
            Text(
                if (spoken > 0) "Spoken $spoken/${video.speakable.size}" else Videos.topic(video.topic)?.name.orEmpty(),
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** A tall 9:16 card for a vertical Short, with its title over the picture. */
@Composable
fun ShortCard(video: Video, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick)) {
        Thumb(video, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Text(
            clock(video.seconds), color = Color.White, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LevelPill(video.level, onDark = true)
                JlptTag(video.jlpt, Modifier.padding(start = 6.dp), onDark = true)
            }
        }
    }
}

/** Two cards per row inside a scrolling page. */
@Composable
private fun VideoGrid(videos: List<Video>, progress: Progress, onOpen: (Video, Boolean) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        videos.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { v -> VideoCard(v, progress, Modifier.weight(1f)) { onOpen(v, false) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** "See more": every video in a section, with topic and level filters. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoListScreen(section: VideoSection, progress: Progress, onOpen: (Video, Boolean) -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var topic by rememberSaveable { mutableStateOf(section.topic) }
    var level by rememberSaveable { mutableStateOf(section.level) }
    var jlpt by rememberSaveable { mutableStateOf(section.jlpt) }
    var draftJlpt by rememberSaveable { mutableStateOf(jlpt) }
    var editing by rememberSaveable { mutableStateOf(false) }
    // Choices in the open panel only apply on "Apply".
    var draftTopic by rememberSaveable { mutableStateOf(topic) }
    var draftLevel by rememberSaveable { mutableStateOf(level) }

    SubScreen(section.title, onBack, action = {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(if (editing) scheme.primaryContainer else scheme.surfaceVariant)
                .clickable(role = Role.Button, onClickLabel = "Filter") { draftTopic = topic; draftLevel = level; draftJlpt = jlpt; editing = !editing },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Tune, contentDescription = "Filter", tint = scheme.onSurface) }
    }) {
        AnimatedVisibility(editing) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                FilterGroup("Topic", listOf<Pair<String?, String>>(null to "All") + Videos.source.topics().map { it.id to it.name }, draftTopic) { draftTopic = it }
                FilterGroup("Level", listOf<Pair<String?, String>>(null to "All") + Videos.source.levels().map { it to it }, draftLevel) { draftLevel = it }
                FilterGroup("JLPT", listOf<Pair<String?, String>>(null to "All") + Videos.source.jlptLevels().map { it to it }, draftJlpt) { draftJlpt = it }
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Reset", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(CircleShape).background(scheme.surfaceVariant)
                            .clickable { draftTopic = null; draftLevel = null; draftJlpt = null }.padding(vertical = 15.dp)
                    )
                    Box(Modifier.weight(1f)) { GradientButton("Apply") { topic = draftTopic; level = draftLevel; jlpt = draftJlpt; editing = false } }
                }
            }
        }
        val list = Videos.filter(level, topic, section.short, jlpt)
        if (list.isEmpty()) Text(
            "No videos match these filters yet.", color = scheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)
        )
        LazyVerticalGrid(
            GridCells.Fixed(2),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(list, key = { it.id }) { v ->
                if (v.short) ShortCard(v) { onOpen(v, false) } else VideoCard(v, progress) { onOpen(v, false) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, options: List<Pair<String?, String>>, selected: String?, onPick: (String?) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(title, color = scheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, name) ->
            val on = id == selected
            Text(
                name,
                color = if (on) scheme.onPrimaryContainer else scheme.onSurface,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(CircleShape).background(if (on) scheme.primaryContainer else scheme.surfaceVariant)
                    .clickable { onPick(id) }.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

/** Videos opened before, newest first. */
@Composable
fun VideoHistoryScreen(progress: Progress, onOpen: (Video, Boolean) -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val list = progress.history.mapNotNull(Videos::byId)
    SubScreen("History", onBack, action = {
        if (list.isNotEmpty()) Text(
            "Clear", color = scheme.secondary, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(CircleShape).clickable { progress.clearHistory() }.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }) {
        if (list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = scheme.outline, modifier = Modifier.size(56.dp))
                Text("Nothing watched yet", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = scheme.onSurface, modifier = Modifier.padding(top = 12.dp))
                Text("Videos you open will show up here.", color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            return@SubScreen
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.id }) { v ->
                val spoken = v.speakable.count { progress.bestScore(v.id, it.index) != null }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onOpen(v, false) }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(140.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))) {
                        Thumb(v, Modifier.matchParentSize())
                        Text(
                            clock(v.seconds), color = Color.White, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(v.title, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            DateUtils.getRelativeTimeSpanString(progress.watched[v.id] ?: 0L).toString(),
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            LevelPill(v.level)
                            Text("Spoken $spoken/${v.speakable.size}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
