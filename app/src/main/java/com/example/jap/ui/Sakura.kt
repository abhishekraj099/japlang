package com.example.jap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

private val PetalLight = Color(0xFFFFE4EC)
private val PetalMid = Color(0xFFF8B3C8)
private val PetalDark = Color(0xFFE77E9E)
private val PetalEdge = Color(0xFFD9668A)
private val Stamen = Color(0xFFE9B23A)

/**
 * One sakura petal as a path: rounded, widest near the tip, with the small notch real cherry petals have.
 * [angle] is in degrees, 0 pointing straight up from [centre]; [length] is centre to tip.
 */
private fun petal(centre: Offset, length: Float, angle: Float): Path {
    val a = Math.toRadians(angle.toDouble())
    val ca = cos(a).toFloat()
    val sa = sin(a).toFloat()
    // Local coordinates: x across the petal, y outwards from the centre.
    fun p(x: Float, y: Float) = Offset(centre.x + (x * ca - (-y) * sa) * length, centre.y + (x * sa + (-y) * ca) * length)
    val start = p(0f, 0.04f)
    val l1 = p(-0.30f, 0.30f); val l2 = p(-0.46f, 0.80f); val lTip = p(-0.17f, 1.0f)
    val notch = p(0f, 0.86f)
    val rTip = p(0.17f, 1.0f); val r2 = p(0.46f, 0.80f); val r1 = p(0.30f, 0.30f)
    return Path().apply {
        moveTo(start.x, start.y)
        cubicTo(l1.x, l1.y, l2.x, l2.y, lTip.x, lTip.y)
        quadraticTo(p(-0.07f, 1.0f).x, p(-0.07f, 1.0f).y, notch.x, notch.y)
        quadraticTo(p(0.07f, 1.0f).x, p(0.07f, 1.0f).y, rTip.x, rTip.y)
        cubicTo(r2.x, r2.y, r1.x, r1.y, start.x, start.y)
        close()
    }
}

/** A full blossom: five shaded petals, a deep centre and a ring of stamens. */
private fun DrawScope.blossom(centre: Offset, radius: Float, light: Color, mid: Color, dark: Color, edge: Color, stamens: Boolean) {
    val shade = Brush.radialGradient(listOf(light, mid, dark), centre, radius)
    repeat(5) { i ->
        val path = petal(centre, radius, i * 72f - 90f + 90f)
        drawPath(path, shade)
        drawPath(path, edge.copy(alpha = 0.55f), style = Stroke(radius * 0.05f))
    }
    drawCircle(dark, radius * 0.22f, centre)
    if (stamens) {
        repeat(10) { i ->
            val a = Math.toRadians(i * 36.0 + 18.0)
            val tip = Offset(centre.x + (cos(a) * radius * 0.40f).toFloat(), centre.y + (sin(a) * radius * 0.40f).toFloat())
            drawLine(dark, centre, tip, radius * 0.035f, cap = StrokeCap.Round)
            drawCircle(Stamen, radius * 0.06f, tip)
        }
    }
}

/** A cherry blossom, shown next to the day streak. */
@Composable
fun SakuraIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        blossom(Offset(w / 2, w / 2), w * 0.48f, PetalLight, PetalMid, PetalDark, PetalEdge, stamens = w > 40f)
    }
}

/** A blossom-shaped marker for a calendar day: one outline around all five petals, so the date can sit inside. */
@Composable
fun BlossomOutline(size: Dp, color: Color, modifier: Modifier = Modifier, filled: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val c = Offset(w / 2, w / 2)
        var shape = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, w * 0.22f)) }
        repeat(5) { i ->
            val combined = Path()
            combined.op(shape, petal(c, w * 0.48f, i * 72f), PathOperation.Union)
            shape = combined
        }
        drawPath(shape, filled)
        drawPath(shape, color, style = Stroke(w * 0.055f))
    }
}

// ---------------- the streak tree ----------------

private class TreeColours(
    val grass: Color, val grassDark: Color, val stone: Color, val bark: Color, val barkDark: Color,
    val light: Color, val mid: Color, val dark: Color
)

private val Blooming = TreeColours(
    grass = Color(0xFF9ACD7E), grassDark = Color(0xFF6FA95A), stone = Color(0xFFF1EEE8),
    bark = Color(0xFF8A6448), barkDark = Color(0xFF654631),
    light = Color(0xFFFFE1EA), mid = Color(0xFFF7AFC4), dark = Color(0xFFE8869F)
)

private val Bare = TreeColours(
    grass = Color(0xFFCFCFCF), grassDark = Color(0xFFB2B2B2), stone = Color(0xFFEDEDED),
    bark = Color(0xFF9E9E9E), barkDark = Color(0xFF7E7E7E),
    light = Color(0xFFEFEFEF), mid = Color(0xFFD6D6D6), dark = Color(0xFFBDBDBD)
)

/** Blossom clouds on the crown, as (x, y, radius) fractions of the canvas, in the order they open. */
private val crown = listOf(
    floatArrayOf(0.50f, 0.24f, 0.17f), floatArrayOf(0.28f, 0.34f, 0.15f), floatArrayOf(0.72f, 0.33f, 0.15f),
    floatArrayOf(0.38f, 0.19f, 0.12f), floatArrayOf(0.63f, 0.18f, 0.12f), floatArrayOf(0.50f, 0.38f, 0.12f),
    floatArrayOf(0.16f, 0.45f, 0.10f), floatArrayOf(0.84f, 0.44f, 0.10f)
)

/** Petals drifting below the crown, as (x, y, angle). */
private val drift = listOf(
    floatArrayOf(0.14f, 0.62f, 30f), floatArrayOf(0.86f, 0.60f, -40f), floatArrayOf(0.24f, 0.74f, 70f),
    floatArrayOf(0.78f, 0.72f, 10f), floatArrayOf(0.66f, 0.80f, -20f)
)

/** A tapered limb from (x1, y1) to (x2, y2), [w1] wide at the start and [w2] at the end. */
private fun DrawScope.limb(color: Color, w: Float, x1: Float, y1: Float, x2: Float, y2: Float, w1: Float, w2: Float) {
    val dx = x2 - x1
    val dy = y2 - y1
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    val nx = -dy / len
    val ny = dx / len
    val path = Path().apply {
        moveTo(w * (x1 + nx * w1 / 2), w * (y1 + ny * w1 / 2))
        quadraticTo(w * ((x1 + x2) / 2 + nx * (w1 + w2) / 4 + ny * 0.02f), w * ((y1 + y2) / 2 + ny * (w1 + w2) / 4 - nx * 0.02f), w * (x2 + nx * w2 / 2), w * (y2 + ny * w2 / 2))
        lineTo(w * (x2 - nx * w2 / 2), w * (y2 - ny * w2 / 2))
        quadraticTo(w * ((x1 + x2) / 2 - nx * (w1 + w2) / 4 + ny * 0.02f), w * ((y1 + y2) / 2 - ny * (w1 + w2) / 4 - nx * 0.02f), w * (x1 - nx * w1 / 2), w * (y1 - ny * w1 / 2))
        close()
    }
    drawPath(path, color)
    drawCircle(color, w * w2 / 2, Offset(w * x2, w * y2))
}

/** A soft cloud of blossom: a shaded under-layer, the main puffs, highlights, then single flowers on top. */
private fun DrawScope.bloomCloud(c: TreeColours, w: Float, x: Float, y: Float, r: Float, flowers: Boolean, seed: Int) {
    val puffs = listOf(0f to 0f) + (0 until 6).map { k ->
        val a = Math.toRadians(k * 60.0 + seed * 17.0)
        (cos(a) * 0.55f).toFloat() to (sin(a) * 0.48f).toFloat()
    }
    puffs.forEach { (px, py) -> drawCircle(c.dark, w * r * 0.60f, Offset(w * (x + px * r), w * (y + py * r + r * 0.14f))) }
    puffs.forEach { (px, py) -> drawCircle(c.mid, w * r * 0.55f, Offset(w * (x + px * r), w * (y + py * r))) }
    puffs.filter { (px, py) -> px + py < 0.1f }.forEach { (px, py) ->
        drawCircle(c.light, w * r * 0.30f, Offset(w * (x + px * r - r * 0.14f), w * (y + py * r - r * 0.16f)))
    }
    if (flowers) {
        listOf(-0.45f to -0.20f, 0.30f to -0.35f, 0.05f to 0.25f, 0.55f to 0.15f, -0.20f to 0.40f).forEachIndexed { i, (fx, fy) ->
            if ((i + seed) % 5 != 4) {
                blossom(Offset(w * (x + fx * r), w * (y + fy * r)), w * r * 0.22f, Color.White, c.light, c.mid, c.dark, stamens = false)
            }
        }
    }
}

/**
 * A cherry tree on a grassy mound that comes into bloom with the streak: bare and grey at 0,
 * then more of the crown flowers at 1, 3, 7, 14 and 30 days.
 */
@Composable
fun SakuraTree(streak: Int, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val alive = streak > 0
        val c = if (alive) Blooming else Bare
        val open = when {
            !alive -> crown.size
            streak < 3 -> 3
            streak < 7 -> 4
            streak < 14 -> 5
            streak < 30 -> 6
            else -> crown.size
        }

        // Ground: shadow, mound, tufts and stones.
        drawOval(Color.Black.copy(alpha = 0.08f), Offset(w * 0.08f, w * 0.86f), Size(w * 0.84f, w * 0.10f))
        val mound = Path().apply {
            moveTo(w * 0.10f, w * 0.90f)
            cubicTo(w * 0.18f, w * 0.78f, w * 0.82f, w * 0.78f, w * 0.90f, w * 0.90f)
            quadraticTo(w * 0.50f, w * 0.97f, w * 0.10f, w * 0.90f)
            close()
        }
        drawPath(mound, Brush.verticalGradient(listOf(c.grass, c.grassDark), w * 0.80f, w * 0.95f))
        listOf(0.20f, 0.30f, 0.70f, 0.80f).forEach { gx ->
            listOf(-0.015f, 0f, 0.015f).forEach { o ->
                drawLine(c.grassDark, Offset(w * (gx + o), w * 0.875f), Offset(w * (gx + o * 2f), w * 0.85f), w * 0.008f, cap = StrokeCap.Round)
            }
        }
        listOf(floatArrayOf(0.26f, 0.90f, 0.07f), floatArrayOf(0.42f, 0.925f, 0.06f), floatArrayOf(0.62f, 0.92f, 0.07f), floatArrayOf(0.76f, 0.895f, 0.05f))
            .forEach { (sx, sy, sw) -> drawOval(c.stone, Offset(w * (sx - sw / 2), w * (sy - sw / 4)), Size(w * sw, w * sw / 2)) }

        // Trunk with a flared base and a darker shaded side.
        val trunk = Path().apply {
            moveTo(w * 0.38f, w * 0.87f)
            cubicTo(w * 0.45f, w * 0.84f, w * 0.46f, w * 0.72f, w * 0.45f, w * 0.60f)
            cubicTo(w * 0.44f, w * 0.52f, w * 0.47f, w * 0.46f, w * 0.48f, w * 0.42f)
            lineTo(w * 0.53f, w * 0.42f)
            cubicTo(w * 0.54f, w * 0.48f, w * 0.56f, w * 0.54f, w * 0.55f, w * 0.62f)
            cubicTo(w * 0.54f, w * 0.74f, w * 0.56f, w * 0.84f, w * 0.63f, w * 0.87f)
            close()
        }
        drawPath(trunk, c.bark)
        val shadeSide = Path().apply {
            moveTo(w * 0.52f, w * 0.44f)
            cubicTo(w * 0.53f, w * 0.52f, w * 0.545f, w * 0.60f, w * 0.535f, w * 0.70f)
            cubicTo(w * 0.53f, w * 0.80f, w * 0.56f, w * 0.85f, w * 0.63f, w * 0.87f)
            lineTo(w * 0.55f, w * 0.87f)
            cubicTo(w * 0.51f, w * 0.80f, w * 0.515f, w * 0.60f, w * 0.50f, w * 0.44f)
            close()
        }
        drawPath(shadeSide, c.barkDark.copy(alpha = 0.55f))

        // Limbs out to each part of the crown.
        limb(c.bark, w, 0.49f, 0.50f, 0.27f, 0.36f, 0.045f, 0.018f)
        limb(c.bark, w, 0.52f, 0.48f, 0.73f, 0.34f, 0.045f, 0.018f)
        limb(c.bark, w, 0.505f, 0.45f, 0.50f, 0.24f, 0.04f, 0.016f)
        limb(c.bark, w, 0.36f, 0.42f, 0.17f, 0.46f, 0.02f, 0.010f)
        limb(c.bark, w, 0.64f, 0.40f, 0.84f, 0.45f, 0.02f, 0.010f)
        limb(c.bark, w, 0.50f, 0.32f, 0.38f, 0.20f, 0.018f, 0.010f)
        limb(c.bark, w, 0.50f, 0.30f, 0.63f, 0.19f, 0.018f, 0.010f)

        // Crown, back clouds first so the front ones overlap them.
        crown.take(open).withIndex().sortedBy { it.value[1] }.forEach { (i, cl) ->
            bloomCloud(c, w, cl[0], cl[1], cl[2], flowers = alive, seed = i)
        }

        // A few petals on the wind.
        if (alive) {
            drift.take(if (streak >= 7) drift.size else 2).forEach { (px, py, a) ->
                val path = petal(Offset(w * px, w * py), w * 0.035f, a)
                drawPath(path, Brush.radialGradient(listOf(c.light, c.mid), Offset(w * px, w * py), w * 0.035f))
            }
        }
    }
}
