package com.example.jap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.League
import com.example.jap.data.Progress
import com.example.jap.ui.theme.Kin
import com.example.jap.ui.theme.Kon
import com.example.jap.ui.theme.Nami

private val medals = listOf(Color(0xFFE0A100), Color(0xFF9AA3AF), Color(0xFFB9722D))

/** Weekly points table: everyone ranked by the XP they earned since Monday. */
@Composable
fun LeagueScreen(progress: Progress, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val xp = progress.weekXp
    val table = remember(xp, progress.name) { League.table(progress.week, progress.weekDay, progress.name, xp) }
    val rank = table.indexOfFirst { it.you } + 1
    val daysLeft = 7 - progress.weekDay

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Kon)
                    .waves(Nami.copy(alpha = 0.12f), radius = 28.dp)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Kin, modifier = Modifier.size(44.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Text("THIS WEEK", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text("You are #$rank", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        "$xp XP · ${if (daysLeft == 1) "ends tonight" else "$daysLeft days left"}",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        itemsIndexed(table) { i, rival ->
            val medal = medals.getOrNull(i)
            Panel(
                Modifier.fillMaxWidth(),
                fill = if (rival.you) scheme.primaryContainer else scheme.surface,
                line = if (rival.you) scheme.primary else scheme.outlineVariant
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(medal ?: Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            fontWeight = FontWeight.Bold,
                            color = if (medal != null) Color.White else scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (rival.you) scheme.primary else scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            rival.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (rival.you) scheme.onPrimary else scheme.onPrimaryContainer
                        )
                    }
                    Text(
                        if (rival.you) "${rival.name} (you)" else rival.name,
                        fontWeight = if (rival.you) FontWeight.Bold else FontWeight.Medium,
                        color = if (rival.you) scheme.onPrimaryContainer else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Text(
                        "${rival.xp} XP",
                        fontWeight = FontWeight.Bold,
                        color = if (rival.you) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.width(72.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        item {
            Text(
                "Sample table: the other learners are placeholders until online accounts are connected.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
