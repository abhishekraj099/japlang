package com.example.jap.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Course
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.Progress

private const val DRILL_SIZE = 12

private val StopSize = 84.dp
private val StopRow = 150.dp
private val StopTop = 12.dp
private val RoadColour = Color(0xFFE6E3EC)
private val Rim = Color(0xFFDCD8EE)
private val Faded = Color(0xFFC6C3D0)

/** A titled row of characters, e.g. the か row. */
private class KanaRow(val title: String, val items: List<Item>)

/** One stop on the alphabet road. [id] is what goes into the learner's finished set. */
private sealed interface Stop {
    val id: String
}

/** A single character with its own short lesson. */
private class CharStop(val item: Item, val row: KanaRow) : Stop {
    override val id get() = "abc-${item.id}"
}

/** A whole row of voiced or blended sounds as one lesson, used on the Combined tab. */
private class RowStop(val row: KanaRow) : Stop {
    override val id get() = "abc-row-${row.items.first().id}"
}

/** A quiz on everything since the previous test. */
private class TestStop(val title: String, val items: List<Item>, override val id: String) : Stop

private class Section(val title: String, val stops: List<Stop>)

/** The chart's rows in one section, with single-character rows (ん) folded into the row before. */
private fun chartRows(kind: Kind, section: Int): List<KanaRow> {
    val rows = Course.kanaChart(kind).getOrNull(section)?.second.orEmpty().map { it.filterNotNull() }.filter { it.isNotEmpty() }
    val merged = mutableListOf<List<Item>>()
    rows.forEach { row -> if (row.size == 1 && merged.isNotEmpty()) merged[merged.lastIndex] = merged.last() + row else merged += row }
    return merged.map { KanaRow("${it.first().jp}-row", it) }
}

private fun sectionsFor(tab: Int): List<Section> = when (tab) {
    0, 1 -> {
        val kind = if (tab == 0) Kind.HIRAGANA else Kind.KATAKANA
        chartRows(kind, 0).map { row ->
            Section(row.title, row.items.map { CharStop(it, row) } + TestStop("${row.title} test", row.items, "abc-test-${row.items.first().id}"))
        }
    }
    else -> listOf(Kind.HIRAGANA to "Hiragana", Kind.KATAKANA to "Katakana").map { (kind, name) ->
        val rows = chartRows(kind, 1) + chartRows(kind, 2)
        Section(
            "$name · voiced & blended",
            rows.map { RowStop(it) } + TestStop("$name voiced & blended test", rows.flatMap { it.items }, "abc-test-combined-$name")
        )
    }
}

/** Horizontal place of the n-th stop in a section: left, centre, right, centre, left… */
private fun column(index: Int) = listOf(0.20f, 0.50f, 0.80f, 0.50f)[index % 4]

private enum class StopLook { DONE, CURRENT, LOCKED }

/** One entry in the scrolling list. */
private sealed interface Entry
private class Heading(val title: String) : Entry
private class Place(val stop: Stop, val x: Float, val nextX: Float?, val look: StopLook) : Entry
private class Certificate(val title: String, val earned: Boolean) : Entry

/**
 * Every character on a winding road through a Japanese landscape, one row at a time, each row
 * closed by a test; stops open one after another. The plain table is a tap away.
 */
@Composable
fun AlphabetScreen(
    progress: Progress,
    onCharacter: (item: Item, row: List<Item>, id: String) -> Unit,
    onRow: (title: String, items: List<Item>, id: String) -> Unit,
    onTest: (title: String, items: List<Item>, id: String) -> Unit,
    onDrill: (title: String, items: List<Item>) -> Unit,
    onCertificate: (key: String, course: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var table by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf("Hiragana", "Katakana", "Combined")
    val scripts = Course.units.filter { it.alphabet }
    val accent = when (tab) {
        0 -> scripts.getOrNull(0)?.let { Color(it.color) } ?: scheme.secondary
        1 -> scripts.getOrNull(1)?.let { Color(it.color) } ?: scheme.primary
        else -> scheme.tertiary
    }
    val sections = remember(tab) { sectionsFor(tab) }

    fun finished(stop: Stop) = stop.id in progress.done || (stop is CharStop && stop.item.id in progress.srs)

    // Lay the road out once per state: stops open one after another through the whole tab.
    val entries = run {
        val all = sections.flatMap { it.stops }
        val current = all.firstOrNull { !finished(it) }
        var open = true
        val list = mutableListOf<Entry>()
        sections.forEach { section ->
            list += Heading(section.title)
            section.stops.forEachIndexed { i, stop ->
                val look = when {
                    finished(stop) -> StopLook.DONE
                    stop == current && open -> StopLook.CURRENT
                    else -> StopLook.LOCKED
                }
                if (look != StopLook.DONE) open = false
                list += Place(stop, column(i), if (i < section.stops.lastIndex) column(i + 1) else null, look)
            }
        }
        list += Certificate("${tabs[tab]} certificate", all.all(::finished))
        list
    }
    val listState = rememberLazyListState()
    LaunchedEffect(tab) {
        val at = entries.indexOfFirst { it is Place && it.look == StopLook.CURRENT }
        listState.scrollToItem((at - 1).coerceAtLeast(0))
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(scheme.surface).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = scheme.surface,
                modifier = Modifier.weight(1f),
                divider = {},
                indicator = {
                    TabRowDefaults.PrimaryIndicator(Modifier.tabIndicatorOffset(tab, matchContentSize = true), color = accent, width = 48.dp)
                }
            ) {
                tabs.forEachIndexed { i, name ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(
                                name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = if (tab == i) scheme.onSurface else scheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            IconButton(onClick = { table = !table }) {
                Icon(
                    if (table) Icons.Rounded.Route else Icons.Rounded.GridView,
                    contentDescription = if (table) "Show as path" else "Show as table",
                    tint = scheme.onSurfaceVariant
                )
            }
        }

        if (table) {
            val rows = sections.flatMap { s -> s.stops.mapNotNull { (it as? CharStop)?.row ?: (it as? RowStop)?.row } }.distinctBy { it.title }
            KanaTable(progress, rows, accent, Modifier.weight(1f))
            PushButton(
                "Practice ${tabs[tab]}",
                onClick = { onDrill("${tabs[tab]} practice", rows.flatMap { it.items }.shuffled().take(DRILL_SIZE)) },
                modifier = Modifier.padding(20.dp),
                color = accent
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                AlphabetScenery(Modifier.fillMaxSize())
                LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 40.dp)) {
                    entries.forEach { entry ->
                        when (entry) {
                            is Heading -> item { RowHeading(entry.title) }
                            is Place -> item(entry.stop.id) {
                                RoadStop(entry, accent) {
                                    when (val stop = entry.stop) {
                                        is CharStop -> onCharacter(stop.item, stop.row.items, stop.id)
                                        is RowStop -> onRow(stop.row.title, stop.row.items, stop.id)
                                        is TestStop -> onTest(stop.title, stop.items, stop.id)
                                    }
                                }
                            }
                            is Certificate -> item { CertificateCard(entry, accent) { onCertificate(tabs[tab].lowercase(), "${tabs[tab]} course") } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowHeading(title: String) {
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 8.dp)
    )
}

/**
 * One stop with the road leading down to the next: straight down from this stop,
 * then across to the next one, with a dashed centre line like a lane marking.
 */
@Composable
private fun RoadStop(entry: Place, accent: Color, onClick: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(StopRow)
            .drawBehind {
                val next = entry.nextX ?: return@drawBehind
                val cy = (StopTop + StopSize / 2).toPx()
                val x1 = size.width * entry.x
                val x2 = size.width * next
                val y2 = size.height + cy
                val corner = 26.dp.toPx()
                val road = Path().apply {
                    moveTo(x1, cy)
                    if (x1 == x2) {
                        lineTo(x2, y2)
                    } else {
                        val dir = if (x2 > x1) 1f else -1f
                        lineTo(x1, y2 - corner)
                        quadraticTo(x1, y2, x1 + dir * corner, y2)
                        lineTo(x2, y2)
                    }
                }
                drawPath(road, RoadColour, style = Stroke(26.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(
                    road, Color.White,
                    style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 9.dp.toPx())))
                )
            }
    ) {
        Box(Modifier.offset(x = maxWidth * entry.x - StopSize / 2, y = StopTop)) {
            StopBadge(entry, accent, onClick)
        }
    }
}

@Composable
private fun StopBadge(entry: Place, accent: Color, onClick: () -> Unit) {
    val speaker = LocalSpeaker.current
    val look = entry.look
    val stop = entry.stop
    val fill = when (look) {
        StopLook.DONE -> accent
        StopLook.CURRENT -> accent.copy(alpha = 0.12f).compositeOverWhite()
        StopLook.LOCKED -> Color.White
    }
    val ring = when (look) {
        StopLook.DONE -> accent
        StopLook.CURRENT -> accent
        StopLook.LOCKED -> Color(0xFFE8E6EF)
    }
    val ink = when (look) {
        StopLook.DONE -> Color.White
        StopLook.CURRENT -> accent
        StopLook.LOCKED -> Faded
    }
    Box(Modifier.size(StopSize, StopSize + 6.dp)) {
        // A rim under the disc gives it a raised, button-like edge.
        Box(Modifier.align(Alignment.BottomCenter).size(StopSize).clip(CircleShape).background(if (look == StopLook.LOCKED) Rim else accent.darken(0.8f)))
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(StopSize)
                .clip(CircleShape)
                .background(fill)
                .border(3.dp, ring, CircleShape)
                .clickable(enabled = look != StopLook.LOCKED, role = Role.Button) {
                    if (stop is CharStop) speaker.speak(stop.item.speak)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (stop) {
                    is CharStop -> Text(stop.item.jp, fontSize = 36.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, color = ink)
                    is RowStop -> Text(stop.row.title, fontSize = if (stop.row.title.length > 5) 14.sp else 17.sp, fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1)
                    is TestStop -> Icon(Icons.Rounded.WorkspacePremium, contentDescription = "Row test", tint = ink, modifier = Modifier.size(34.dp))
                }
                if (look == StopLook.LOCKED && stop !is TestStop) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = Faded, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (look == StopLook.DONE) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Finished", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun Color.compositeOverWhite() = Color(
    red * alpha + (1 - alpha), green * alpha + (1 - alpha), blue * alpha + (1 - alpha), 1f
)

/** A certificate at the end of the road, coloured in once every stop is finished. */
@Composable
private fun CertificateCard(entry: Certificate, accent: Color, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val paper = if (entry.earned) Color(0xFFFFFBF2) else Color(0xFFF1F0F3)
    val line = if (entry.earned) accent else Color(0xFF9E9CA8)
    val seal = if (entry.earned) Color(0xFFD9A72C) else Color(0xFFB4B2BC)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .clickable(enabled = entry.earned, role = Role.Button, onClickLabel = "Open certificate", onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.size(170.dp, 120.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(line, Offset(0f, 0f), Size(w, h * 0.86f), CornerRadius(w * 0.04f))
            drawRoundRect(paper, Offset(w * 0.05f, h * 0.07f), Size(w * 0.90f, h * 0.72f), CornerRadius(w * 0.02f))
            listOf(0.22f, 0.32f).forEachIndexed { i, y ->
                val half = if (i == 0) 0.22f else 0.30f
                drawLine(line.copy(alpha = 0.6f), Offset(w * (0.5f - half), h * y), Offset(w * (0.5f + half), h * y), h * 0.025f, cap = StrokeCap.Round)
            }
            drawLine(line.copy(alpha = 0.6f), Offset(w * 0.18f, h * 0.55f), Offset(w * 0.36f, h * 0.55f), h * 0.03f, cap = StrokeCap.Round)
            drawLine(line.copy(alpha = 0.6f), Offset(w * 0.64f, h * 0.55f), Offset(w * 0.82f, h * 0.55f), h * 0.03f, cap = StrokeCap.Round)
            // Rosette with ribbon tails.
            val c = Offset(w * 0.5f, h * 0.58f)
            listOf(-1f, 1f).forEach { side ->
                val tail = Path().apply {
                    moveTo(c.x + side * w * 0.03f, c.y)
                    lineTo(c.x + side * w * 0.07f, h * 0.98f)
                    lineTo(c.x + side * w * 0.03f, h * 0.90f)
                    lineTo(c.x, h * 0.98f)
                    close()
                }
                drawPath(tail, seal.darken(0.85f))
            }
            drawCircle(seal, h * 0.16f, c)
            drawCircle(paper.copy(alpha = 0.5f), h * 0.09f, c)
        }
        Text(
            if (entry.earned) "${entry.title} earned! Tap to open" else "Finish every row to earn your ${entry.title}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (entry.earned) FontWeight.SemiBold else FontWeight.Normal,
            color = if (entry.earned) accent else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 40.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun KanaTable(progress: Progress, rows: List<KanaRow>, accent: Color, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val cell = RoundedCornerShape(12.dp)
    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            item(row.title) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.items.forEach { item ->
                        val learned = item.id in progress.srs
                        Column(
                            Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(cell)
                                .background(if (learned) accent.copy(alpha = 0.14f) else scheme.surface)
                                .border(1.dp, if (learned) accent else scheme.outlineVariant, cell)
                                .clickable(onClickLabel = "Play ${item.romaji}") { speaker.speak(item.speak) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(item.jp, fontSize = 24.sp, color = scheme.onSurface)
                            Text(item.romaji, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                    // Keep short rows the same cell width as full ones.
                    repeat((5 - row.items.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
