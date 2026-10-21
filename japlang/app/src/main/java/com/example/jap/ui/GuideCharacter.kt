package com.example.jap.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.example.jap.data.Guide
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

private val Skin = Color(0xFFFFE6D8)
private val SkinShade = Color(0xFFF5CDBB)
private val Blush = Color(0xFFFF9EB0)

/**
 * How one guide looks: hair, eyes and clothes. [boy] gives short spiky hair, lighter lashes, a haori jacket and a
 * plain sash instead of long hair, a hair pin and an obi with a knot. Body, face and poses are shared.
 */
private class Look(
    val hair: Color, val hairShine: Color, val iris: Color, val irisDark: Color,
    val sleeve: Color, val sleeveShade: Color, val hakama: Color, val hakamaShade: Color,
    val collar: Color, val obi: Color, val obiShade: Color, val boy: Boolean
)

private val Hana = Look(
    hair = Color(0xFF2E2238), hairShine = Color(0xFF54426A), iris = Color(0xFF6B4FB3), irisDark = Color(0xFF2E1F57),
    sleeve = Color(0xFFF6BCCD), sleeveShade = Color(0xFFE79AB2), hakama = Color(0xFF2B3A6B), hakamaShade = Color(0xFF1F2B52),
    collar = Color(0xFFD9475C), obi = Color(0xFFE8708F), obiShade = Color(0xFFC9506F), boy = false
)

private val Ren = Look(
    hair = Color(0xFF3B2A24), hairShine = Color(0xFF6E5246), iris = Color(0xFF2F8A8F), irisDark = Color(0xFF15434A),
    sleeve = Color(0xFF34558A), sleeveShade = Color(0xFF263F68), hakama = Color(0xFF5B606C), hakamaShade = Color(0xFF454955),
    collar = Color(0xFF34558A), obi = Color(0xFF3E3F4A), obiShade = Color(0xFF2C2D35), boy = true
)

/** The drawn look for a guide id from guide.json; unknown ids use the first guide's look. */
private fun lookFor(id: String) = if (id == "ren") Ren else Hana

/** Artwork that replaces the drawn guide, read from assets/guide/<pose>.webp or .png; null when there is none. */
@Composable
private fun rememberGuideArt(id: String, pose: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(id, pose) {
        val names = listOf("guide/$id/$pose") + if (id == Guide.characters.firstOrNull()?.id) listOf("guide/$pose") else emptyList()
        names.firstNotNullOfOrNull { name ->
            listOf("webp", "png").firstNotNullOfOrNull { ext ->
                runCatching { context.assets.open("$name.$ext").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
}

/**
 * A guide in one of their poses: wave, point, cheer or think. They bob gently and wave when waving. [character] is
 * a guide id from guide.json and defaults to the learner's chosen guide. Drawn in code so the app ships without art;
 * artwork in assets/guide/<id>/ takes their place.
 */
@Composable
fun GuideCharacter(pose: String, size: Dp, modifier: Modifier = Modifier, flip: Boolean = false, character: String = Guide.current.id) {
    val art = rememberGuideArt(character, pose)
    // Mirrored when she stands on the right, so she still faces what she is talking about.
    val facing = if (flip) Modifier.graphicsLayer { scaleX = -1f } else Modifier
    val motion = rememberInfiniteTransition(label = "guide")
    val bob by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    if (art != null) {
        Image(art, contentDescription = "Your guide", contentScale = ContentScale.Fit, modifier = modifier.then(facing).size(size, size * 1.3f))
        return
    }
    Canvas(modifier.then(facing).size(size, size * 1.3f)) {
        val w = this.size.width
        translate(top = -bob * w * 0.015f) { guide(w, pose, bob, lookFor(character)) }
    }
}

private val Line = Color(0xFF2A1F35)
private val Kimono = Color(0xFFFFFFFF)
private val KimonoShade = Color(0xFFE9E2F0)

/** Fills [path] with [fill] and draws the anime line art around it. */
private fun DrawScope.inked(path: Path, fill: Color, w: Float, weight: Float = 0.011f) {
    drawPath(path, fill)
    drawPath(path, Line, style = Stroke(w * weight, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.guide(w: Float, pose: String, bob: Float, look: Look) {
    fun o(x: Float, y: Float) = Offset(w * x, w * y)
    fun path(vararg points: Pair<Float, Float>, block: Path.() -> Unit = {}) = Path().apply {
        moveTo(w * points[0].first, w * points[0].second)
        points.drop(1).forEach { (x, y) -> lineTo(w * x, w * y) }
        block()
        close()
    }

    // Behind the head: long hair to below the shoulders for her, a short crop for him.
    if (look.boy) {
        inked(Path().apply { addOval(androidx.compose.ui.geometry.Rect(o(0.19f, 0.08f), o(0.81f, 0.56f))) }, look.hair, w)
    } else {
    val back = Path().apply {
        moveTo(w * 0.17f, w * 0.30f)
        cubicTo(w * 0.12f, w * 0.55f, w * 0.14f, w * 0.80f, w * 0.20f, w * 0.92f)
        lineTo(w * 0.27f, w * 0.86f); lineTo(w * 0.33f, w * 0.93f); lineTo(w * 0.38f, w * 0.80f)
        lineTo(w * 0.62f, w * 0.80f); lineTo(w * 0.67f, w * 0.93f); lineTo(w * 0.73f, w * 0.86f); lineTo(w * 0.80f, w * 0.92f)
        cubicTo(w * 0.86f, w * 0.80f, w * 0.88f, w * 0.55f, w * 0.83f, w * 0.30f)
        close()
    }
    inked(back, look.hair, w)
    }

    // Hakama: a pleated skirt widening to the hem.
    val skirt = path(0.31f to 0.90f, 0.69f to 0.90f, 0.80f to 1.27f, 0.20f to 1.27f)
    drawPath(skirt, Brush.verticalGradient(listOf(look.hakama, look.hakamaShade), w * 0.90f, w * 1.27f))
    drawPath(skirt, Line, style = Stroke(w * 0.011f, join = StrokeJoin.Round))
    listOf(0.40f, 0.50f, 0.60f).forEach { x -> drawLine(look.hakamaShade, o(x, 0.93f), o(x + (x - 0.5f) * 0.45f, 1.26f), w * 0.008f) }

    // Kimono top, wrapped left over right, with a pink collar.
    val top = Path().apply {
        moveTo(w * 0.32f, w * 0.71f)
        cubicTo(w * 0.38f, w * 0.67f, w * 0.62f, w * 0.67f, w * 0.68f, w * 0.71f)
        lineTo(w * 0.71f, w * 0.91f); lineTo(w * 0.29f, w * 0.91f); close()
    }
    inked(top, Kimono, w)
    drawPath(path(0.50f to 0.80f, 0.68f to 0.72f, 0.70f to 0.91f, 0.55f to 0.91f), KimonoShade)
    val collar = Path().apply { moveTo(w * 0.40f, w * 0.68f); lineTo(w * 0.53f, w * 0.83f); moveTo(w * 0.60f, w * 0.68f); lineTo(w * 0.47f, w * 0.83f) }
    drawPath(collar, Line, style = Stroke(w * 0.034f, cap = StrokeCap.Round))
    drawPath(collar, look.collar, style = Stroke(w * 0.020f, cap = StrokeCap.Round))
    // The sash: an obi with a little knot for her, a plain narrow band for him.
    inked(path(0.29f to 0.84f, 0.71f to 0.84f, 0.71f to 0.92f, 0.29f to 0.92f), look.obi, w)
    drawLine(look.obiShade, o(0.30f, 0.905f), o(0.70f, 0.905f), w * 0.012f)
    if (!look.boy) inked(Path().apply { addOval(androidx.compose.ui.geometry.Rect(o(0.57f, 0.855f), o(0.64f, 0.91f))) }, look.obiShade, w, 0.008f)
    if (look.boy) {
        // An open haori jacket over the kimono, tied with a white cord.
        inked(path(0.30f to 0.715f, 0.41f to 0.685f, 0.46f to 0.92f, 0.29f to 0.92f), look.sleeve, w)
        inked(path(0.70f to 0.715f, 0.59f to 0.685f, 0.54f to 0.92f, 0.71f to 0.92f), look.sleeve, w)
        drawLine(Line, o(0.455f, 0.80f), o(0.545f, 0.80f), w * 0.016f, cap = StrokeCap.Round)
        drawLine(Color.White, o(0.455f, 0.80f), o(0.545f, 0.80f), w * 0.009f, cap = StrokeCap.Round)
        inked(Path().apply { addOval(androidx.compose.ui.geometry.Rect(o(0.485f, 0.785f), o(0.515f, 0.815f))) }, Color.White, w, 0.006f)
    }

    // Arms: wide kimono sleeves from the shoulder, a hand peeking out of each.
    val swing = (bob - 0.5f) * 18f
    val arms: List<Pair<Offset, Offset>> = when (pose) {
        "point" -> listOf(o(0.33f, 0.75f) to o(0.27f, 0.98f), o(0.67f, 0.75f) to o(0.96f, 0.70f))
        "cheer" -> listOf(o(0.33f, 0.74f) to o(0.13f, 0.50f), o(0.67f, 0.74f) to o(0.87f, 0.50f))
        "think" -> listOf(o(0.33f, 0.75f) to o(0.27f, 0.98f), o(0.67f, 0.75f) to o(0.58f, 0.60f))
        else -> listOf(o(0.33f, 0.75f) to o(0.27f, 0.98f), o(0.67f, 0.75f) to o(0.88f, 0.47f))
    }
    arms.forEachIndexed { i, (shoulder, hand) ->
        val waving = pose == "wave" && i == 1
        rotate(if (waving) swing else 0f, shoulder) {
            val dx = hand.x - shoulder.x; val dy = hand.y - shoulder.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy)
            val ux = dx / len; val uy = dy / len
            val nx = -uy; val ny = ux
            val cuff = Offset(shoulder.x + dx * 0.80f, shoulder.y + dy * 0.80f)
            val sleeve = Path().apply {
                moveTo(shoulder.x + nx * w * 0.045f, shoulder.y + ny * w * 0.045f)
                lineTo(cuff.x + nx * w * 0.075f, cuff.y + ny * w * 0.075f)
                quadraticTo(cuff.x + ux * w * 0.03f, cuff.y + uy * w * 0.03f, cuff.x - nx * w * 0.075f, cuff.y - ny * w * 0.075f)
                lineTo(shoulder.x - nx * w * 0.045f, shoulder.y - ny * w * 0.045f)
                close()
            }
            inked(Path().apply { addOval(androidx.compose.ui.geometry.Rect(hand, w * 0.042f)) }, Skin, w, 0.009f)
            if (pose == "point" && i == 1) {
                drawLine(Line, hand, hand + Offset(ux * w * 0.07f, uy * w * 0.07f), w * 0.030f, cap = StrokeCap.Round)
                drawLine(Skin, hand, hand + Offset(ux * w * 0.07f, uy * w * 0.07f), w * 0.016f, cap = StrokeCap.Round)
            }
            inked(sleeve, look.sleeve, w)
            drawLine(look.sleeveShade, cuff + Offset(nx * w * 0.06f, ny * w * 0.06f), cuff - Offset(nx * w * 0.06f, ny * w * 0.06f), w * 0.014f, cap = StrokeCap.Round)
        }
    }

    // Neck, then the face: round cheeks narrowing to a soft chin.
    inked(path(0.45f to 0.58f, 0.55f to 0.58f, 0.55f to 0.70f, 0.45f to 0.70f), SkinShade, w, 0.009f)
    val face = Path().apply {
        moveTo(w * 0.25f, w * 0.30f)
        cubicTo(w * 0.24f, w * 0.48f, w * 0.38f, w * 0.62f, w * 0.50f, w * 0.635f)
        cubicTo(w * 0.62f, w * 0.62f, w * 0.76f, w * 0.48f, w * 0.75f, w * 0.30f)
        cubicTo(w * 0.72f, w * 0.12f, w * 0.28f, w * 0.12f, w * 0.25f, w * 0.30f)
        close()
    }
    inked(face, Skin, w)

    // Eyes: tall and shining, with a heavy upper lash and a flick at the outer corner; closed arcs when cheering.
    listOf(0.385f to -1f, 0.615f to 1f).forEach { (x, side) ->
        if (pose == "cheer") {
            drawArc(Line, 200f, 140f, false, o(x - 0.055f, 0.41f), Size(w * 0.11f, w * 0.08f), style = Stroke(w * 0.018f, cap = StrokeCap.Round))
        } else {
            drawOval(Color.White, o(x - 0.052f, 0.365f), Size(w * 0.104f, w * 0.125f))
            drawOval(Brush.verticalGradient(listOf(look.irisDark, look.iris, lerp(look.iris, Color.White, 0.45f)), w * 0.37f, w * 0.49f), o(x - 0.043f, 0.372f), Size(w * 0.086f, w * 0.115f))
            drawOval(look.irisDark, o(x - 0.02f, 0.405f), Size(w * 0.04f, w * 0.05f))
            drawCircle(Color.White, w * 0.018f, o(x - 0.012f * side + 0.012f, 0.398f))
            drawCircle(Color.White.copy(alpha = 0.85f), w * 0.008f, o(x - 0.018f, 0.46f))
            // Her lashes are heavy with a flick at the corner; his are a plain, lighter line.
            drawArc(Line, 190f, 160f, false, o(x - 0.062f, 0.358f), Size(w * 0.124f, w * 0.07f), style = Stroke(w * (if (look.boy) 0.014f else 0.020f), cap = StrokeCap.Round))
            if (!look.boy) drawLine(Line, o(x + side * 0.058f, 0.378f), o(x + side * 0.078f, 0.360f), w * 0.012f, cap = StrokeCap.Round)
            drawLine(Line.copy(alpha = 0.5f), o(x - 0.03f, 0.493f), o(x + 0.03f, 0.493f), w * 0.006f, cap = StrokeCap.Round)
        }
        drawArc(look.hair, 210f, 120f, false, o(x - 0.05f, 0.315f), Size(w * 0.10f, w * 0.04f), style = Stroke(w * (if (look.boy) 0.016f else 0.010f), cap = StrokeCap.Round))
    }
    // Nose, blush with little hatch marks, mouth.
    drawLine(SkinShade, o(0.50f, 0.505f), o(0.505f, 0.515f), w * 0.010f, cap = StrokeCap.Round)
    listOf(0.32f, 0.68f).forEach { x ->
        drawOval(Blush.copy(alpha = if (look.boy) 0.25f else 0.45f), o(x - 0.045f, 0.50f), Size(w * 0.09f, w * 0.04f))
        if (!look.boy) listOf(-0.02f, 0f, 0.02f).forEach { d -> drawLine(Color(0xFFF07C98), o(x + d, 0.512f), o(x + d + 0.01f, 0.502f), w * 0.005f, cap = StrokeCap.Round) }
    }
    when (pose) {
        "cheer" -> {
            val mouth = Path().apply { moveTo(w * 0.455f, w * 0.545f); quadraticTo(w * 0.50f, w * 0.615f, w * 0.545f, w * 0.545f); close() }
            inked(mouth, Color(0xFFC2415B), w, 0.008f)
            drawOval(Color(0xFFFF9EB0), o(0.48f, 0.57f), Size(w * 0.04f, w * 0.02f))
        }
        "think" -> drawCircle(Color(0xFFC2415B), w * 0.012f, o(0.53f, 0.56f))
        else -> drawArc(Line, 25f, 130f, false, o(0.465f, 0.525f), Size(w * 0.07f, w * 0.04f), style = Stroke(w * 0.010f, cap = StrokeCap.Round))
    }

    // Fringe: pointed locks over the forehead, a ring of shine, and locks framing the face. His hair stands up in
    // spikes over the crown and stops short at the sides.
    val fringe = if (look.boy) Path().apply {
        moveTo(w * 0.21f, w * 0.40f)
        lineTo(w * 0.16f, w * 0.22f); lineTo(w * 0.25f, w * 0.18f); lineTo(w * 0.24f, w * 0.06f); lineTo(w * 0.36f, w * 0.11f)
        lineTo(w * 0.42f, w * 0.01f); lineTo(w * 0.51f, w * 0.09f); lineTo(w * 0.61f, w * 0.0f); lineTo(w * 0.65f, w * 0.11f)
        lineTo(w * 0.78f, w * 0.07f); lineTo(w * 0.75f, w * 0.19f); lineTo(w * 0.85f, w * 0.24f); lineTo(w * 0.79f, w * 0.40f)
        lineTo(w * 0.73f, w * 0.27f); lineTo(w * 0.66f, w * 0.34f); lineTo(w * 0.60f, w * 0.25f); lineTo(w * 0.52f, w * 0.33f)
        lineTo(w * 0.46f, w * 0.24f); lineTo(w * 0.38f, w * 0.33f); lineTo(w * 0.32f, w * 0.25f); lineTo(w * 0.26f, w * 0.33f)
        close()
    } else Path().apply {
        moveTo(w * 0.20f, w * 0.40f)
        cubicTo(w * 0.15f, w * 0.06f, w * 0.85f, w * 0.06f, w * 0.80f, w * 0.40f)
        lineTo(w * 0.74f, w * 0.26f); lineTo(w * 0.69f, w * 0.35f); lineTo(w * 0.62f, w * 0.24f); lineTo(w * 0.56f, w * 0.33f)
        lineTo(w * 0.50f, w * 0.23f); lineTo(w * 0.44f, w * 0.33f); lineTo(w * 0.38f, w * 0.24f); lineTo(w * 0.31f, w * 0.35f)
        lineTo(w * 0.26f, w * 0.26f)
        close()
    }
    inked(fringe, look.hair, w)
    drawArc(look.hairShine, 200f, 140f, false, o(0.28f, 0.12f), Size(w * 0.44f, w * 0.20f), style = Stroke(w * 0.022f, cap = StrokeCap.Round))
    val lockEnd = if (look.boy) 0.50f else 0.70f
    listOf(0.21f to 1f, 0.79f to -1f).forEach { (x, dir) ->
        val lock = Path().apply {
            moveTo(w * x, w * 0.30f)
            quadraticTo(w * (x - dir * 0.05f), w * (0.30f + (lockEnd - 0.30f) * 0.55f), w * (x + dir * 0.03f), w * lockEnd)
            lineTo(w * (x + dir * 0.07f), w * (lockEnd - 0.08f))
            quadraticTo(w * (x + dir * 0.03f), w * 0.46f, w * (x + dir * 0.07f), w * 0.33f)
            close()
        }
        inked(lock, look.hair, w, 0.010f)
    }

    // Sakura hair pin, hers only.
    if (!look.boy) drawHairPin(o(0.72f, 0.19f), w)

    if (pose == "think") {
        drawCircle(Color.White, w * 0.02f, o(0.86f, 0.30f))
        drawCircle(Line, w * 0.02f, o(0.86f, 0.30f), style = Stroke(w * 0.006f))
        drawCircle(Color.White, w * 0.03f, o(0.91f, 0.22f))
        drawCircle(Line, w * 0.03f, o(0.91f, 0.22f), style = Stroke(w * 0.006f))
    }
}

private fun DrawScope.drawHairPin(pin: Offset, w: Float) {
    repeat(5) { k ->
        val a = Math.toRadians(k * 72.0 - 90)
        val c = Offset(pin.x + (cos(a) * w * 0.03f).toFloat(), pin.y + (sin(a) * w * 0.03f).toFloat())
        drawCircle(Line, w * 0.031f, c)
    }
    repeat(5) { k ->
        val a = Math.toRadians(k * 72.0 - 90)
        drawCircle(Color(0xFFF7A8C0), w * 0.025f, Offset(pin.x + (cos(a) * w * 0.03f).toFloat(), pin.y + (sin(a) * w * 0.03f).toFloat()))
    }
    drawCircle(Color(0xFFE2577A), w * 0.015f, pin)
}
