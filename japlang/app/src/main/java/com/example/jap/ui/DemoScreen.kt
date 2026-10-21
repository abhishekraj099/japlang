package com.example.jap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalVoice
import com.example.jap.data.Guide
import com.example.jap.data.Progress
import com.example.jap.data.Tree
import kotlinx.coroutines.launch

/**
 * A short welcome before sign-in: the guide introduces herself and the app in a few swipeable pages, each read
 * aloud. Pages come from assets/guide.json; the learner can swipe, tap Next, or skip at any time.
 */
@Composable
fun DemoScreen(progress: Progress, onDone: () -> Unit) {
    val pages = Guide.demo
    if (pages.isEmpty()) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val scheme = MaterialTheme.colorScheme
    val voice = LocalVoice.current
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    LaunchedEffect(pager.currentPage) {
        voice.enabled = progress.guideVoice
        voice.useGuideVoice(progress)
        pages[pager.currentPage].let { voice.say("${it.title}. ${it.text}") }
    }
    DisposableEffect(Unit) { onDispose { voice.stop() } }
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "学び", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    color = scheme.primary, modifier = Modifier.padding(start = 16.dp).weight(1f)
                )
                if (!last) TextButton(onClick = onDone, modifier = Modifier.height(48.dp)) { Text("Skip", fontWeight = FontWeight.SemiBold) }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DemoArt(index, page.pose)
                    Text(
                        page.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp)
                    )
                    Text(
                        page.text, fontSize = 17.sp, lineHeight = 26.sp, color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)
                    )
                    if (index == 0 && Guide.characters.size > 1) {
                        // New learners pick their guide straight away; they can change it later in Settings.
                        Text(
                            "Choose your guide", fontWeight = FontWeight.SemiBold, color = scheme.onBackground,
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
                        )
                        GuidePicker(progress, Modifier.fillMaxWidth()) {
                            voice.useGuideVoice(progress)
                            voice.say("${page.title}. ${page.text}")
                        }
                    }
                }
            }
            // Where the learner is: one dot per page, the current one long.
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics { contentDescription = "Page ${pager.currentPage + 1} of ${pages.size}" },
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val width by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot")
                    val colour by animateColorAsState(if (selected) scheme.primary else scheme.outlineVariant, label = "dotColour")
                    Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(width).clip(CircleShape).background(colour))
                }
            }
            PushButton(
                if (last) "Get started" else "Next",
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
            )
        }
    }
}

/** The picture on each page: the guide on her own, the guide with the three scripts, then the sakura she helps grow. */
@Composable
private fun DemoArt(index: Int, pose: String) {
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        when (index) {
            0 -> {
                Box(Modifier.size(230.dp).clip(CircleShape).background(Color(0xFFFFE6EE)))
                GuideCharacter(pose, 190.dp)
            }
            1 -> {
                Box(Modifier.size(230.dp).clip(CircleShape).background(Color(0xFFE6ECFA)))
                Row(Modifier.align(Alignment.TopCenter).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("あ" to "hiragana", "ア" to "katakana", "学" to "kanji").forEach { (glyph, name) -> ScriptCard(glyph, name) }
                }
                GuideCharacter(pose, 150.dp, Modifier.align(Alignment.BottomCenter).offset(y = 6.dp))
            }
            else -> {
                LivingTree(Tree.State(Tree.Stage.BLOSSOM, Tree.Condition.HEALTHY, 1f, 30, 0, false, true, 40, 0), 270.dp)
                GuideCharacter(pose, 110.dp, Modifier.align(Alignment.BottomStart).offset(x = (-4).dp, y = 6.dp))
            }
        }
    }
}

@Composable
private fun ScriptCard(glyph: String, name: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(scheme.surface).border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(glyph, fontSize = 30.sp, fontFamily = FontFamily.Serif, color = scheme.onSurface)
        Text(name, fontSize = 12.sp, color = scheme.onSurfaceVariant)
    }
}
