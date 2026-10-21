package com.example.jap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker
import com.example.jap.data.Course
import com.example.jap.data.Item
import com.example.jap.data.Progress

private const val DECK_SIZE = 20

/** Searchable list of every word and kanji in the course. */
@Composable
fun WordsScreen(progress: Progress, onBack: () -> Unit, onCards: (List<Item>) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { Course.allItems.filter { !it.isKana } }
    val shown = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) all
        else all.filter { q in it.jp || q in it.reading || q in it.romaji || q in it.meaning.lowercase() }
    }

    SubScreen("Words", onBack) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                label = { Text("Search Japanese, romaji or English") }
            )
            PushButton(
                "Flashcards (${shown.size.coerceAtMost(DECK_SIZE)})",
                onClick = { onCards(shown.shuffled().take(DECK_SIZE)) },
                modifier = Modifier.padding(vertical = 12.dp),
                enabled = shown.isNotEmpty(),
                color = scheme.secondary
            )
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (shown.isEmpty()) {
                item { Text("No words match \"$query\".", color = scheme.onSurfaceVariant) }
            }
            items(shown, key = { it.id }) { item ->
                val learned = item.id in progress.srs
                Panel(Modifier.fillMaxWidth(), line = if (learned) scheme.tertiary else scheme.outline) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Play audio") { speaker.speak(item.speak) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.jp, fontSize = 22.sp, color = scheme.onSurface)
                            Text(item.romaji, color = scheme.primary)
                        }
                        Text(
                            "${item.emoji} ${item.meaning}".trim(),
                            color = scheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** Flip cards: a card marked "Again" goes to the back of the deck. */
@Composable
fun FlashcardScreen(items: List<Item>, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val speaker = LocalSpeaker.current
    val deck = remember { mutableStateListOf<Item>().also { it.addAll(items) } }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    SubScreen("Flashcards", onBack) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val card = deck.getOrNull(index)
            if (card == null) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(80.dp))
                    Text(
                        "Deck finished",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Text("${items.size} cards", color = scheme.onSurfaceVariant)
                }
                PushButton("Done", onBack)
            } else {
                Meter(index / deck.size.toFloat(), scheme.secondary)
                Panel(Modifier.weight(1f).fillMaxWidth().padding(vertical = 20.dp)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clickable(onClickLabel = "Flip card") {
                                flipped = !flipped
                                if (flipped) speaker.speak(card.speak)
                            }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            card.jp,
                            fontSize = if (card.jp.length <= 2) 96.sp else 44.sp,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (flipped) {
                            if (card.reading.isNotEmpty()) Text(card.reading, fontSize = 20.sp, color = scheme.onSurfaceVariant)
                            Text(card.romaji, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                            Text(
                                "${card.emoji} ${card.meaning}".trim(),
                                fontSize = 24.sp,
                                color = scheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Text("Tap to flip", color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PushButton(
                        "Again",
                        onClick = {
                            deck.add(card)
                            flipped = false
                            index++
                        },
                        modifier = Modifier.weight(1f),
                        color = scheme.error
                    )
                    PushButton(
                        "Got it",
                        onClick = {
                            flipped = false
                            index++
                        },
                        modifier = Modifier.weight(1f),
                        color = scheme.tertiary
                    )
                }
            }
        }
    }
}
