package com.example.jap.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class StopState { DONE, CURRENT, OPEN, LOCKED }

private val RowHeight = 156.dp
private val TagSpace = 28.dp
private val Stop = 74.dp
private val Swing = 84.dp

/** Horizontal position of the n-th stop on a winding path, from -1 (left) to 1 (right). */
fun pathSwing(index: Int): Float = listOf(0f, 0.55f, 0.85f, 0.55f, 0f, -0.55f, -0.85f, -0.55f)[index % 8]

/**
 * One stop on a winding learning path. Draws the dashed trail down to the next stop, whose
 * swing is [nextSwing]; pass null for the last stop of a section.
 */
@Composable
fun PathStop(
    glyph: String,
    label: String,
    color: Color,
    state: StopState,
    swing: Float,
    nextSwing: Float?,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val trail = if (state == StopState.DONE) color.copy(alpha = 0.55f) else scheme.outline
    Box(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawBehind {
                if (nextSwing == null) return@drawBehind
                // From under this stop's label to the top of the next stop's badge.
                val from = Offset(size.width / 2 + swing * Swing.toPx(), size.height - 4.dp.toPx())
                val to = Offset(size.width / 2 + nextSwing * Swing.toPx(), size.height + TagSpace.toPx() + 6.dp.toPx())
                val bend = (to.y - from.y) * 0.6f
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    cubicTo(from.x, from.y + bend, to.x, to.y - bend, to.x, to.y)
                }
                drawPath(
                    path, trail,
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 12.dp.toPx()))
                    )
                )
            }
    ) {
        Column(
            Modifier.align(Alignment.TopCenter).offset(x = Swing * swing).width(140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.height(TagSpace), contentAlignment = Alignment.BottomCenter) {
                if (state == StopState.CURRENT) {
                    Text(
                        "START",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            StopBadge(glyph, color, state, shape, onClick)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (state == StopState.LOCKED) scheme.onSurfaceVariant else scheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun StopBadge(glyph: String, color: Color, state: StopState, shape: Shape, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "pulse")
    val grow by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "grow")
    Box(Modifier.size(Stop + 14.dp), contentAlignment = Alignment.Center) {
        if (state == StopState.CURRENT) {
            Box(Modifier.size(Stop + 12.dp).scale(grow).clip(shape).background(color.copy(alpha = 0.18f)))
        }
        val fill = when (state) {
            StopState.DONE, StopState.CURRENT -> color
            StopState.OPEN -> scheme.surface
            StopState.LOCKED -> scheme.surfaceVariant
        }
        Box(
            Modifier
                .size(Stop)
                .clip(shape)
                .background(fill)
                .border(if (state == StopState.OPEN) 2.dp else 0.dp, if (state == StopState.OPEN) color.copy(alpha = 0.6f) else Color.Transparent, shape)
                .clickable(enabled = state != StopState.LOCKED, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // An inner ring gives the stop the look of a stamped seal.
            Box(Modifier.size(Stop - 12.dp).border(1.5.dp, Color.White.copy(alpha = 0.35f), shape))
            if (state == StopState.LOCKED) {
                Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = scheme.onSurfaceVariant)
            } else {
                Text(
                    glyph,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (glyph.length > 1) 22.sp else 28.sp,
                    color = if (state == StopState.OPEN) color else Color.White
                )
            }
        }
        if (state == StopState.DONE) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary)
                    .border(2.dp, scheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Finished", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
