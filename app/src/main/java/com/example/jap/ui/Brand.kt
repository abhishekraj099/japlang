package com.example.jap.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.jap.R
import com.example.jap.ui.theme.Hinomaru
import com.example.jap.ui.theme.Kon
import com.example.jap.ui.theme.Nami
import kotlinx.coroutines.delay

private const val SPLASH_MILLIS = 1700L
private const val RISE_MILLIS = 900

/**
 * Seigaiha, the traditional "blue sea and waves" pattern: rows of overlapping crests, each three
 * concentric arcs. Drawn from [top] down to the bottom of the area.
 *
 * With [fill] each crest is painted solid first, which hides the rows above and whatever is behind;
 * without it the hidden part of each crest is clipped away instead, so the pattern can sit on a gradient.
 */
fun DrawScope.seigaiha(line: Color, radius: Float, top: Float = 0f, strokeWidth: Float = radius * 0.08f, fill: Color? = null) {
    val stroke = Stroke(strokeWidth)
    val rings = listOf(0.95f, 0.64f, 0.34f)
    var row = 0
    var y = top + radius
    while (y - radius < size.height) {
        var x = if (row % 2 == 0) 0f else radius
        while (x - radius < size.width) {
            val centre = Offset(x, y)
            if (fill != null) {
                drawCircle(fill, radius, centre)
                rings.forEach { drawCircle(line, radius * it, centre, style = stroke) }
            } else {
                // The crests of the next two rows cover the lower part of this one.
                val covered = Path().apply {
                    addOval(Rect(Offset(x - radius, y + radius / 2), radius))
                    addOval(Rect(Offset(x + radius, y + radius / 2), radius))
                    addOval(Rect(Offset(x, y + radius), radius))
                }
                clipPath(covered, ClipOp.Difference) {
                    rings.forEach { drawCircle(line, radius * it, centre, style = stroke) }
                }
            }
            x += radius * 2
        }
        y += radius / 2
        row++
    }
}

/** Faint wave pattern over whatever background the element already has. */
fun Modifier.waves(color: Color, radius: Dp = 22.dp): Modifier = drawBehind {
    seigaiha(color, radius.toPx(), top = -radius.toPx(), strokeWidth = 1.dp.toPx())
}

/** The brand scene: a red sun over waves on night indigo. [rise] runs from 0 (sun below the waves) to 1. */
private fun DrawScope.sunrise(rise: Float, waveTop: Float, waveRadius: Float, sunRadius: Float) {
    drawRect(Kon)
    val rest = waveTop + waveRadius - sunRadius * 0.55f
    val hidden = waveTop + waveRadius + sunRadius
    drawCircle(Hinomaru, sunRadius, Offset(size.width / 2, hidden + (rest - hidden) * rise))
    seigaiha(Nami.copy(alpha = 0.85f), waveRadius, waveTop, fill = Kon)
}

/** The app's emblem, matching the launcher icon in res/drawable. */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(size * 0.24f)) {
    Canvas(modifier.size(size).clip(shape)) {
        val side = this.size.width
        sunrise(rise = 1f, waveTop = side * 0.50f, waveRadius = side * 0.153f, sunRadius = side * 0.25f)
    }
}

/** Name in Japanese over the name in Latin capitals. */
@Composable
fun Wordmark(color: Color, modifier: Modifier = Modifier, large: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.brand_japanese),
            color = color,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (large) 64.sp else 44.sp
        )
        Text(
            stringResource(R.string.app_name).uppercase(),
            color = color.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            fontSize = if (large) 16.sp else 13.sp,
            letterSpacing = if (large) 10.sp else 8.sp,
            // Letter spacing is added after every glyph, so nudge right to stay optically centred.
            modifier = Modifier.padding(start = if (large) 10.dp else 8.dp, top = 2.dp)
        )
    }
}

/** Indigo header with the sunrise scene and wordmark, used at the top of the sign-in screen. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.matchParentSize()) {
            sunrise(rise = 1f, waveTop = size.height * 0.66f, waveRadius = size.width / 9f, sunRadius = size.width * 0.14f)
        }
        Wordmark(Nami, Modifier.padding(top = 64.dp))
    }
}

/**
 * Sets whether the clock and status icons are drawn light (for a dark top area) or dark.
 * Call from one place only: two callers would race when screens cross-fade.
 */
@Composable
fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    LaunchedEffect(light) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !light
    }
}

/** Shown once at launch: the sun rises behind the waves while the name fades in. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        rise.animateTo(1f, tween(RISE_MILLIS, easing = FastOutSlowInEasing))
        delay(SPLASH_MILLIS - RISE_MILLIS)
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            sunrise(rise.value, waveTop = size.height * 0.60f, waveRadius = size.width / 8f, sunRadius = size.width * 0.20f)
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 168.dp).alpha(rise.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Wordmark(Nami, large = true)
            Text(
                stringResource(R.string.brand_tagline),
                color = Nami.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

/** A red name-seal stamp, as pressed on finished work. */
@Composable
fun Seal(text: String, size: Dp, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.secondary
    Box(
        modifier
            .size(size)
            .rotate(-8f)
            .border(4.dp, ink, RoundedCornerShape(size * 0.16f))
            .padding(7.dp)
            .border(1.5.dp, ink, RoundedCornerShape(size * 0.11f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.32f).sp
        )
    }
}
