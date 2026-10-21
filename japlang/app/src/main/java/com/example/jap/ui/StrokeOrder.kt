package com.example.jap.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jap.data.Strokes
import kotlinx.coroutines.delay

/** True when there is stroke-order data for [char]. */
fun hasStrokeOrder(char: String) = Strokes.of(char).isNotEmpty()

/**
 * Draws [char] one stroke at a time in writing order, over a faint copy of the whole character and a guide cross.
 * The stroke being drawn is in [accent]. Tapping replays it.
 */
@Composable
fun StrokeOrder(char: String, accent: Color, modifier: Modifier = Modifier, size: Dp = 180.dp) {
    val scheme = MaterialTheme.colorScheme
    val strokes = remember(char) { Strokes.of(char).map { PathParser().parsePathString(it).toPath() } }
    val lengths = remember(strokes) { strokes.map { PathMeasure().apply { setPath(it, false) }.length } }
    val progress = remember(char) { Animatable(0f) }
    var replays by remember { mutableIntStateOf(0) }

    LaunchedEffect(char, replays) {
        progress.snapTo(0f)
        delay(300)
        strokes.indices.forEach { i ->
            // Longer strokes take a little longer, as they would with a brush.
            val millis = (250 + lengths[i] * 6).toInt().coerceAtMost(900)
            progress.animateTo(i + 1f, tween(millis, easing = LinearEasing))
            delay(140)
        }
    }

    val ghost = scheme.outlineVariant.copy(alpha = 0.45f)
    val ink = scheme.onSurface
    val grid = scheme.outlineVariant.copy(alpha = 0.6f)
    Canvas(
        modifier
            .size(size)
            .clickable(onClickLabel = "Replay stroke order") { replays++ }
            .semantics { contentDescription = "$char, written in ${strokes.size} stroke${if (strokes.size == 1) "" else "s"}" }
    ) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        drawLine(grid, Offset(this.size.width / 2, 0f), Offset(this.size.width / 2, this.size.height), 1.5f, pathEffect = dash)
        drawLine(grid, Offset(0f, this.size.height / 2), Offset(this.size.width, this.size.height / 2), 1.5f, pathEffect = dash)

        val k = this.size.minDimension / Strokes.BOX
        translate((this.size.width - Strokes.BOX * k) / 2, (this.size.height - Strokes.BOX * k) / 2) {
            scale(k, pivot = Offset.Zero) {
                val pen = Stroke(width = 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                strokes.forEach { drawPath(it, ghost, style = pen) }
                val p = progress.value
                strokes.forEachIndexed { i, path ->
                    when {
                        i + 1 <= p -> drawPath(path, ink, style = pen)
                        i < p -> {
                            val part = Path()
                            PathMeasure().apply { setPath(path, false) }.getSegment(0f, lengths[i] * (p - i), part, true)
                            drawPath(part, accent, style = pen)
                        }
                    }
                }
            }
        }
    }
}
