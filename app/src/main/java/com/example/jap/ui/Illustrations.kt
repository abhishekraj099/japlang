package com.example.jap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.ui.theme.Kon

private val Bark = Color(0xFF6B4A2F)
private val Needles = Color(0xFF3F7D58)
private val NeedlesLight = Color(0xFF6FA57C)
private val Bud = Color(0xFFE8899B)
private val Dormant = Color(0xFFB9B4AB)
private val DormantLight = Color(0xFFD4D0C8)
private val Gold = Color(0xFFD4A72C)
private val GoldDeep = Color(0xFF9C7413)
private val Charm = Color(0xFFC8372D)

/** Pads of foliage, as (centre x, centre y, width, height) fractions of the canvas, in the order they appear. */
private val pads = listOf(
    floatArrayOf(0.50f, 0.34f, 0.34f, 0.15f),
    floatArrayOf(0.31f, 0.47f, 0.28f, 0.12f),
    floatArrayOf(0.69f, 0.43f, 0.28f, 0.12f),
    floatArrayOf(0.27f, 0.60f, 0.22f, 0.10f),
    floatArrayOf(0.56f, 0.22f, 0.22f, 0.10f)
)

/** How many pads a streak of this length grows. */
private fun padsFor(streak: Int) = when {
    streak <= 0 -> pads.size
    streak < 3 -> 1
    streak < 7 -> 2
    streak < 14 -> 3
    streak < 30 -> 4
    else -> 5
}

/**
 * A bonsai pine that grows with the streak. With no streak it is drawn whole but grey,
 * to show what the learner is working towards.
 */
@Composable
fun Bonsai(streak: Int, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val alive = streak > 0
        val sapling = alive && streak < 3

        // Pot.
        val potTop = w * 0.80f
        drawRoundRect(Kon, Offset(w * 0.20f, potTop), Size(w * 0.60f, w * 0.05f), CornerRadius(w * 0.015f))
        val pot = Path().apply {
            moveTo(w * 0.24f, potTop + w * 0.05f)
            lineTo(w * 0.76f, potTop + w * 0.05f)
            lineTo(w * 0.70f, w * 0.96f)
            lineTo(w * 0.30f, w * 0.96f)
            close()
        }
        drawPath(pot, Kon.copy(alpha = 0.85f))

        // Trunk.
        val bark = if (alive) Bark else Dormant
        val trunk = Path().apply {
            moveTo(w * 0.50f, potTop)
            if (sapling) {
                quadraticTo(w * 0.46f, w * 0.68f, w * 0.50f, w * 0.58f)
            } else {
                cubicTo(w * 0.40f, w * 0.66f, w * 0.64f, w * 0.52f, w * 0.50f, w * 0.36f)
            }
        }
        drawPath(trunk, bark, style = Stroke(w * 0.065f, cap = StrokeCap.Round))
        if (!sapling) {
            branch(bark, w, 0.47f, 0.55f, 0.31f, 0.48f)
            branch(bark, w, 0.53f, 0.49f, 0.69f, 0.44f)
        }

        // Foliage.
        val dark = if (alive) Needles else Dormant
        val light = if (alive) NeedlesLight else DormantLight
        if (sapling) {
            pad(floatArrayOf(0.50f, 0.55f, 0.22f, 0.10f), w, dark, light)
        } else {
            pads.take(padsFor(streak)).forEach { pad(it, w, dark, light) }
        }
        // A long streak earns the tree its first blossoms.
        if (streak >= 30) {
            listOf(0.42f to 0.31f, 0.60f to 0.36f, 0.26f to 0.45f, 0.74f to 0.41f, 0.52f to 0.20f).forEach { (x, y) ->
                drawCircle(Bud, w * 0.022f, Offset(w * x, w * y))
            }
        }
    }
}

private fun DrawScope.branch(color: Color, w: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
    drawLine(color, Offset(w * x1, w * y1), Offset(w * x2, w * y2), w * 0.035f, cap = StrokeCap.Round)
}

private fun DrawScope.pad(spec: FloatArray, w: Float, dark: Color, light: Color) {
    val (x, y, pw, ph) = spec.toList()
    drawOval(dark, Offset(w * (x - pw / 2), w * (y - ph / 2)), Size(w * pw, w * ph))
    drawOval(light, Offset(w * (x - pw * 0.38f), w * (y - ph * 0.48f)), Size(w * pw * 0.6f, w * ph * 0.5f))
}

private val Petal = Color(0xFFF4A6BD)
private val PetalDeep = Color(0xFFE0708F)
private val Water = Color(0xFF4FA3E8)
private val WaterDeep = Color(0xFF2B7CC4)

/** A water drop: the currency earned by studying and spent on omamori. */
@Composable
fun DropIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val drop = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            cubicTo(w * 0.62f, h * 0.24f, w * 0.86f, h * 0.44f, w * 0.86f, h * 0.62f)
            cubicTo(w * 0.86f, h * 0.84f, w * 0.70f, h * 0.97f, w * 0.5f, h * 0.97f)
            cubicTo(w * 0.30f, h * 0.97f, w * 0.14f, h * 0.84f, w * 0.14f, h * 0.62f)
            cubicTo(w * 0.14f, h * 0.44f, w * 0.38f, h * 0.24f, w * 0.5f, h * 0.04f)
            close()
        }
        drawPath(drop, Water)
        drawPath(drop, WaterDeep, style = Stroke(w * 0.05f))
        // Highlight.
        drawOval(Color.White.copy(alpha = 0.75f), Offset(w * 0.30f, h * 0.52f), Size(w * 0.14f, h * 0.24f))
    }
}

/** Omamori, a shrine charm. Each one shields the streak for one missed day. */
@Composable
fun OmamoriIcon(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val body = Path().apply {
                moveTo(w * 0.36f, w * 0.18f)
                lineTo(w * 0.64f, w * 0.18f)
                lineTo(w * 0.78f, w * 0.32f)
                lineTo(w * 0.78f, w * 0.94f)
                lineTo(w * 0.22f, w * 0.94f)
                lineTo(w * 0.22f, w * 0.32f)
                close()
            }
            drawPath(body, Charm)
            drawRoundRect(Color.White, Offset(w * 0.34f, w * 0.40f), Size(w * 0.32f, w * 0.42f), CornerRadius(w * 0.03f))
            // Cord and knot.
            drawCircle(Gold, w * 0.07f, Offset(w * 0.5f, w * 0.16f))
            drawLine(Gold, Offset(w * 0.5f, w * 0.10f), Offset(w * 0.40f, w * 0.02f), w * 0.04f, cap = StrokeCap.Round)
            drawLine(Gold, Offset(w * 0.5f, w * 0.10f), Offset(w * 0.60f, w * 0.02f), w * 0.04f, cap = StrokeCap.Round)
        }
        Text(
            "守",
            color = Charm,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.24f).sp,
            modifier = Modifier.offset(y = size * 0.11f)
        )
    }
}

/** A soft cloud for sky backgrounds. */
fun DrawScope.cloud(x: Float, y: Float, scale: Float, color: Color = Color.White.copy(alpha = 0.85f)) {
    drawCircle(color, 22f * scale, Offset(x, y))
    drawCircle(color, 30f * scale, Offset(x + 30f * scale, y - 10f * scale))
    drawCircle(color, 20f * scale, Offset(x + 60f * scale, y))
    drawRoundRect(color, Offset(x - 10f * scale, y - 4f * scale), Size(80f * scale, 24f * scale), CornerRadius(12f * scale))
}
