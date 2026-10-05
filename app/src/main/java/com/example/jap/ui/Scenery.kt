package com.example.jap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private val SkyTop = Color(0xFFFCF8F1)
private val SkyBottom = Color(0xFFFBEDE3)
private val Vermilion = Color(0xFFD9573F)
private val VermilionDark = Color(0xFFA9402D)
private val Roof = Color(0xFF6C6A78)
private val RoofLight = Color(0xFF8D8B99)
private val Wall = Color(0xFFF7F2EA)
private val Wood = Color(0xFFB98B5E)
private val Pine = Color(0xFF7FA670)
private val Mountain = Color(0xFFF1DAD2)
private val MountainShade = Color(0xFFE8C6BC)
private val Snow = Color(0xFFFFFBF7)
private val Ground = Color(0xFFF3E4D2)

/**
 * The backdrop of the Alphabet tab: a warm sky with clouds, a torii gate, a temple hall,
 * a castle keep and Mt Fuji, drawn faintly so the learning path stays in front.
 */
@Composable
fun AlphabetScenery(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))

        cloud(w * 0.10f, h * 0.30f, 1.0f, Color.White.copy(alpha = 0.9f))
        cloud(w * 0.80f, h * 0.48f, 1.4f, Color.White.copy(alpha = 0.9f))
        cloud(w * 0.28f, h * 0.62f, 0.8f, Color(0xFFFFF4DC))
        cloud(w * 0.62f, h * 0.20f, 0.7f, Color(0xFFFFF4DC))

        fuji(Offset(w * 0.98f, h), w * 0.52f)
        torii(Offset(w * 0.11f, h * 0.44f), w * 0.22f)
        temple(Offset(w * 0.86f, h * 0.13f), w * 0.28f)
        castle(Offset(w * 0.10f, h * 0.92f), w * 0.22f)
        pine(Offset(w * 0.22f, h * 0.92f), w * 0.07f)
    }
}

/** A grassy island under a landmark. */
private fun DrawScope.island(base: Offset, width: Float) {
    drawOval(Ground, Offset(base.x - width * 0.6f, base.y - width * 0.10f), Size(width * 1.2f, width * 0.24f))
    drawArc(
        Color(0xFFF0C77B).copy(alpha = 0.6f), 0f, 180f, false,
        Offset(base.x - width * 0.6f, base.y - width * 0.10f), Size(width * 1.2f, width * 0.24f),
        style = Stroke(width * 0.025f)
    )
}

/** A torii gate: two pillars, the tie beam, and the curved top beam. [size] is its width. */
private fun DrawScope.torii(base: Offset, size: Float) {
    island(Offset(base.x, base.y + size * 0.05f), size * 0.9f)
    val left = base.x - size * 0.32f
    val right = base.x + size * 0.32f
    val top = base.y - size * 0.75f
    val pillar = size * 0.07f
    listOf(left, right).forEach { x ->
        drawRect(Vermilion, Offset(x - pillar / 2, top), Size(pillar, base.y - top))
        drawRect(Color(0xFF5A5A5A), Offset(x - pillar * 0.7f, base.y - size * 0.06f), Size(pillar * 1.4f, size * 0.06f))
    }
    drawRect(Vermilion, Offset(left - size * 0.08f, top + size * 0.14f), Size(right - left + size * 0.16f, size * 0.05f))
    val kasagi = Path().apply {
        moveTo(left - size * 0.20f, top - size * 0.06f)
        quadraticTo(base.x, top + size * 0.02f, right + size * 0.20f, top - size * 0.06f)
        lineTo(right + size * 0.17f, top + size * 0.03f)
        quadraticTo(base.x, top + size * 0.09f, left - size * 0.17f, top + size * 0.03f)
        close()
    }
    drawPath(kasagi, VermilionDark)
    drawRect(Vermilion, Offset(base.x - pillar * 0.4f, top + size * 0.03f), Size(pillar * 0.8f, size * 0.11f))
}

/** A low temple hall with a sweeping tiled roof. [size] is its width. */
private fun DrawScope.temple(base: Offset, size: Float) {
    island(Offset(base.x, base.y + size * 0.04f), size)
    drawRect(Wood, Offset(base.x - size * 0.42f, base.y - size * 0.06f), Size(size * 0.84f, size * 0.06f))
    drawRect(Wall, Offset(base.x - size * 0.36f, base.y - size * 0.30f), Size(size * 0.72f, size * 0.24f))
    for (i in 0..4) {
        val x = base.x - size * 0.36f + i * size * 0.18f
        drawRect(Vermilion, Offset(x - size * 0.012f, base.y - size * 0.30f), Size(size * 0.024f, size * 0.24f))
    }
    val roof = Path().apply {
        moveTo(base.x - size * 0.56f, base.y - size * 0.28f)
        quadraticTo(base.x - size * 0.40f, base.y - size * 0.34f, base.x - size * 0.30f, base.y - size * 0.50f)
        lineTo(base.x + size * 0.30f, base.y - size * 0.50f)
        quadraticTo(base.x + size * 0.40f, base.y - size * 0.34f, base.x + size * 0.56f, base.y - size * 0.28f)
        close()
    }
    drawPath(roof, Roof)
    drawRect(RoofLight, Offset(base.x - size * 0.30f, base.y - size * 0.53f), Size(size * 0.60f, size * 0.04f))
}

/** A three-storey castle keep with white walls and curved roofs. [size] is its width. */
private fun DrawScope.castle(base: Offset, size: Float) {
    island(Offset(base.x, base.y + size * 0.04f), size)
    drawRect(Color(0xFFBDB6AA), Offset(base.x - size * 0.40f, base.y - size * 0.16f), Size(size * 0.80f, size * 0.16f))
    var y = base.y - size * 0.16f
    listOf(0.62f, 0.48f, 0.34f).forEach { tier ->
        val half = size * tier / 2
        drawRect(Wall, Offset(base.x - half * 0.82f, y - size * 0.16f), Size(half * 1.64f, size * 0.16f))
        val roof = Path().apply {
            moveTo(base.x - half * 1.15f, y - size * 0.15f)
            quadraticTo(base.x - half * 0.9f, y - size * 0.20f, base.x - half * 0.75f, y - size * 0.25f)
            lineTo(base.x + half * 0.75f, y - size * 0.25f)
            quadraticTo(base.x + half * 0.9f, y - size * 0.20f, base.x + half * 1.15f, y - size * 0.15f)
            close()
        }
        drawPath(roof, Vermilion.copy(alpha = 0.85f))
        y -= size * 0.25f
    }
    drawCircle(Color(0xFFE2B33C), size * 0.03f, Offset(base.x, y + size * 0.01f))
}

/** Mt Fuji with its snow cap, rising from the bottom edge. [width] is the width of its base. */
private fun DrawScope.fuji(base: Offset, width: Float) {
    val peak = Offset(base.x - width * 0.35f, base.y - width * 0.42f)
    val mountain = Path().apply {
        moveTo(base.x - width * 0.85f, base.y)
        lineTo(peak.x - width * 0.08f, peak.y)
        lineTo(peak.x + width * 0.08f, peak.y)
        lineTo(base.x + width * 0.15f, base.y)
        close()
    }
    drawPath(mountain, Brush.horizontalGradient(listOf(Mountain, MountainShade), base.x - width * 0.85f, base.x + width * 0.15f))
    val snow = Path().apply {
        moveTo(peak.x - width * 0.08f, peak.y)
        lineTo(peak.x + width * 0.08f, peak.y)
        lineTo(peak.x + width * 0.17f, peak.y + width * 0.10f)
        lineTo(peak.x + width * 0.09f, peak.y + width * 0.08f)
        lineTo(peak.x + width * 0.03f, peak.y + width * 0.12f)
        lineTo(peak.x - width * 0.04f, peak.y + width * 0.08f)
        lineTo(peak.x - width * 0.11f, peak.y + width * 0.12f)
        lineTo(peak.x - width * 0.17f, peak.y + width * 0.10f)
        close()
    }
    drawPath(snow, Snow)
}

/** A small pine with layered foliage. [size] is its height. */
private fun DrawScope.pine(base: Offset, size: Float) {
    drawLine(Wood, base, Offset(base.x, base.y - size), size * 0.12f, cap = StrokeCap.Round)
    listOf(0.35f to 0.55f, 0.62f to 0.42f, 0.86f to 0.28f).forEach { (at, wide) ->
        drawRoundRect(
            Pine, Offset(base.x - size * wide, base.y - size * at - size * 0.10f),
            Size(size * wide * 2, size * 0.18f), CornerRadius(size * 0.09f)
        )
    }
}
