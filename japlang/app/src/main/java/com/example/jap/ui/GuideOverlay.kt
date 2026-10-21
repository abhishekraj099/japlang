package com.example.jap.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.jap.audio.Speaker
import com.example.jap.audio.Voice
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Guide
import com.example.jap.data.Progress
import kotlinx.coroutines.delay

/**
 * Runs the guide's tours: which tour is showing, which step, and where each tagged element is on screen.
 * Tours come from [Guide]; whether one starts by itself depends on the learner's settings and what they have seen.
 */
class GuideController(private val progress: Progress, private val voice: Voice) {

    /** Where each tagged element is, in root coordinates. */
    val targets = mutableStateMapOf<String, Rect>()

    var tour by mutableStateOf<Guide.Tour?>(null)
        private set
    var step by mutableIntStateOf(0)
        private set

    val current: Guide.Step? get() = tour?.steps?.getOrNull(step)

    /** Starts the tour for [screen] on a first visit, if the learner has the guide on and the tour is for them. */
    fun maybeStart(screen: String) {
        if (tour != null || !progress.guideOn || screen in progress.toursSeen) return
        val found = Guide.tour(screen) ?: return
        if (found.beginnersOnly && !progress.beginner) return
        begin(found)
    }

    /** Plays [screen]'s tour again, from the "?" button; settings do not matter here. */
    fun replay(screen: String) {
        Guide.tour(screen)?.let(::begin)
    }

    fun hasTour(screen: String) = Guide.tour(screen) != null

    /** The first time a learner taps something new, the guide explains it, if there is an explanation for [id]. */
    fun explain(id: String) = maybeStart(id)

    /** The guide says [text] on her own, as an answer from the helper; it is not remembered as a tour. */
    fun say(text: String, pose: String) = begin(Guide.Tour(SAY, false, listOf(Guide.Step(null, pose, text, tap = false))))

    private fun begin(t: Guide.Tour) {
        tour = t
        step = 0
        speak()
    }

    fun next() {
        val t = tour ?: return
        if (step < t.steps.lastIndex) {
            step++
            speak()
        } else finish()
    }

    /** Reads the current step aloud, when the learner has the guide's voice on. */
    fun speak() {
        voice.enabled = progress.guideVoice
        voice.useGuideVoice(progress)
        current?.let { voice.say(it.text) }
    }

    /** Ends the tour and remembers it, so it does not start again by itself. */
    fun finish() {
        voice.stop()
        tour?.let { if (it.screen != SAY) progress.markTourSeen(it.screen) }
        tour = null
        step = 0
    }

    private companion object {
        const val SAY = "say"
    }

    /** A tagged element was tapped; that moves a "tap this" step on. */
    fun tapped(id: String) {
        val s = current ?: return
        if (s.tap && s.target == id) next()
    }
}

val LocalGuide = staticCompositionLocalOf<GuideController?> { null }

/** Tags an element so tours can point at it by [id], and notices when the learner taps it. */
fun Modifier.guideTarget(id: String): Modifier = composed {
    val guide = LocalGuide.current ?: return@composed this
    DisposableEffect(id) { onDispose { guide.targets.remove(id) } }
    this
        .onGloballyPositioned { guide.targets[id] = it.boundsInRoot() }
        .pointerInput(id) {
            awaitEachGesture {
                // Watch the tap on the way down without taking it, so the element still gets its click.
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (waitForUpOrCancellation(PointerEventPass.Initial) != null) guide.tapped(id)
            }
        }
}

/**
 * Starts [screen]'s tour on a first visit, once the screen has settled so its elements are in place,
 * and ends it if the learner leaves the screen.
 */
@Composable
fun GuideTour(screen: String) {
    val guide = LocalGuide.current ?: return
    LaunchedEffect(screen) {
        delay(TOUR_DELAY_MILLIS)
        guide.maybeStart(screen)
    }
    DisposableEffect(screen) {
        onDispose { if (guide.tour?.screen == screen) guide.finish() }
    }
}

private const val TOUR_DELAY_MILLIS = 700L

/**
 * A small "?" that plays this screen's tour again. It looks [visual] wide but always takes taps across 48dp.
 * [container] and [content] let it match the buttons beside it.
 */
@Composable
fun GuideHelpButton(
    screen: String,
    modifier: Modifier = Modifier,
    container: Color = Color.Unspecified,
    content: Color = Color.Unspecified,
    visual: androidx.compose.ui.unit.Dp = 38.dp
) {
    val guide = LocalGuide.current ?: return
    if (!guide.hasTour(screen)) return
    val scheme = MaterialTheme.colorScheme
    val fill = if (container == Color.Unspecified) scheme.surface else container
    val ink = if (content == Color.Unspecified) scheme.onSurfaceVariant else content
    Box(
        modifier
            .size(maxOf(48.dp, visual))
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Show tips for this screen") { guide.replay(screen) }
            .semantics { contentDescription = "Show tips for this screen" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(visual)
                .clip(CircleShape)
                .background(fill)
                .then(if (container == Color.Unspecified) Modifier.border(1.dp, scheme.outlineVariant, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text("?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ink)
        }
    }
}

/**
 * Draws the current step over everything: a dimmed screen with a spotlight on the step's element, and the guide
 * with a speech bubble. Taps outside the spotlight are held back; on a "tap this" step the spotlight lets the tap
 * through to the element underneath.
 */
@Composable
fun GuideOverlay(guide: GuideController) {
    val step = guide.current ?: return
    val tour = guide.tour ?: return
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val hole = step.target?.let { guide.targets[it] }?.inflate(pad)
    val pulse by rememberInfiniteTransition(label = "spot").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = with(density) { maxHeight.toPx() }
        val width = with(density) { maxWidth.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color(0xFF0E1230).copy(alpha = 0.62f))
            if (hole != null) {
                val corner = CornerRadius(14.dp.toPx())
                drawRoundRect(Color.Black, hole.topLeft, hole.size, corner, blendMode = BlendMode.Clear)
                drawRoundRect(Color(0xFFFFD34D).copy(alpha = if (step.tap) pulse else 0.9f), hole.topLeft, hole.size, corner, style = Stroke(3.dp.toPx()))
            }
        }
        // Hold back taps everywhere except, on a "tap this" step, inside the spotlight.
        val through = if (step.tap && hole != null) hole else null
        val blocks = if (through == null) listOf(Rect(0f, 0f, width, height)) else listOf(
            Rect(0f, 0f, width, through.top), Rect(0f, through.bottom, width, height),
            Rect(0f, through.top, through.left, through.bottom), Rect(through.right, through.top, width, through.bottom)
        )
        blocks.filter { it.width > 0 && it.height > 0 }.forEach { r ->
            Box(
                Modifier
                    .offset { IntOffset(r.left.toInt(), r.top.toInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
            )
        }
        // The guide stands away from the spotlight: below or above it, and on the side it is not on. Without a
        // spotlight she alternates sides from step to step, so she pops in from either edge.
        val atTop = hole != null && hole.center.y > height * 0.5f
        val onRight = when (step.side) {
            "left" -> false
            "right" -> true
            // A target well to one side keeps her on the other; one near the middle lets her alternate.
            else -> if (hole != null && kotlin.math.abs(hole.center.x - width / 2) > width * 0.15f) hole.center.x < width * 0.5f else guide.step % 2 == 1
        }
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = if (atTop) Alignment.TopStart else Alignment.BottomStart) {
            key(tour.screen, guide.step) {
                val shown = remember { MutableTransitionState(false).apply { targetState = true } }
                val enter = slideInHorizontally(spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { if (onRight) it else -it } + fadeIn()
                AnimatedVisibility(shown, enter = enter) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                        if (!onRight) GuideCharacter(step.pose, 110.dp)
                        Bubble(
                            step, guide, tour,
                            Modifier.weight(1f).padding(start = if (onRight) 0.dp else 4.dp, end = if (onRight) 4.dp else 0.dp, bottom = 24.dp)
                        )
                        if (onRight) GuideCharacter(step.pose, 110.dp, flip = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Bubble(step: Guide.Step, guide: GuideController, tour: Guide.Tour, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val last = guide.step == tour.steps.lastIndex
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Guide.name, fontWeight = FontWeight.Bold, color = Color(0xFFD9475C), modifier = Modifier.weight(1f))
            if (tour.steps.size > 1) Text("${guide.step + 1}/${tour.steps.size}", color = Color(0xFF6E6A7C), fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
            IconButton(onClick = guide::speak, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear ${Guide.name} say it again", tint = Color(0xFF6E6A7C))
            }
        }
        Text(step.text, color = Color(0xFF1B1B22), fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // A single tip needs only "Got it!"; a longer tour can be skipped part-way.
            if (tour.steps.size > 1) TextButton(onClick = guide::finish) { Text(if (last) "Close" else "Skip tour", color = Color(0xFF6E6A7C)) }
            else Spacer(Modifier.width(1.dp))
            if (step.tap && step.target != null && guide.targets[step.target] != null) {
                Text("Tap the glowing spot", fontWeight = FontWeight.SemiBold, color = scheme.primary, modifier = Modifier.padding(end = 8.dp))
            } else {
                TextButton(onClick = guide::next) { Text(if (last) "Got it!" else "Next", fontWeight = FontWeight.Bold, color = scheme.primary) }
            }
        }
    }
}

/** Gives the guide the voice chosen in Settings: their own character voice unless the learner picked another. */
fun Voice.useGuideVoice(progress: Progress) =
    Guide.voiceStyle(progress.guideVoiceName, "en", Guide.current).let { useVoice(it.names, it.pitch) }

/** Reads lessons in the Japanese voice chosen in Settings: a character's voice, one engine voice, or the default. */
fun Speaker.useJapaneseVoice(progress: Progress) =
    Guide.voiceStyle(progress.japaneseVoice, "ja", null).let { useVoice(it.names, it.pitch) }
