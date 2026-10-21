package com.example.jap.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import com.example.jap.data.Tree
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// The learner's tree is a sakura, painted the way anime paints cherry trees: dark, gnarled branches with the
// horizontal bark stripes of a real cherry, and a crown of soft pink masses covered in five-petal flowers.
// Missed days fade the flowers and break their petals off; study brings the blossom back.

/** One limb: [parent] is -1 for the trunk; [angle] is in degrees relative to the parent, 0 straight on. */
private class Limb(val parent: Int, val depth: Int, val angle: Float, val length: Float, val width: Float)

/**
 * A cluster of blossom at [at] (0 base to 1 tip) along limb [limb], [radius] wide as a fraction of the canvas.
 * [order] decides which clusters last longest as the tree dries; [flowers] are single blossoms on top of it.
 */
private class Cluster(
    val limb: Int, val at: Float, val radius: Float, val order: Float, val inner: Boolean, val flowers: List<Bloom>,
    val dx: Float = 0f, val dy: Float = 0f
)

/** One flower in a cluster: offset from its centre (in cluster radii), size (in canvas widths), turn and shade. */
private class Bloom(val dx: Float, val dy: Float, val size: Float, val turn: Float, val shade: Int)

private class Shape(val limbs: List<Limb>, val clusters: List<Cluster>, val sprout: Boolean)

/** Flat tones for cel shading, darkest first. */
private class Tones(val line: Color, val shadow: Color, val base: Color, val lit: Color)

private val Blossom = Tones(Color(0xFFA9476F), Color(0xFFE58AAE), Color(0xFFF5AEC6), Color(0xFFFFCCDB))
private val Faded = Tones(Color(0xFF8A5A50), Color(0xFFC9A08E), Color(0xFFDDBFAF), Color(0xFFEAD5C8))
private val Bark = Tones(Color(0xFF26160F), Color(0xFF452B22), Color(0xFF64443A), Color(0xFF8A6A5C))
private val DeadBark = Tones(Color(0xFF45413D), Color(0xFF6E6862), Color(0xFF8F8982), Color(0xFFA9A39B))
private val Seedling = Tones(Color(0xFF1F4A2C), Color(0xFF3C7E3E), Color(0xFF5FA64B), Color(0xFF8BCB5D))

/** Petal gradients for single flowers: light, middle and deep pink, each as (heart, tip). */
private val Petals = listOf(
    Color(0xFFFFFFFF) to Color(0xFFF9C3D3),
    Color(0xFFFFF1F5) to Color(0xFFF2A4BE),
    Color(0xFFFFE3EC) to Color(0xFFE88AAB)
)
private val FadedPetals = listOf(Color(0xFFF8EEE8) to Color(0xFFDCC2B4), Color(0xFFF1E3DA) to Color(0xFFCDAE9C), Color(0xFFEADBD0) to Color(0xFFBE9E8B))

/** Adds one sakura petal to [path]: base at the origin, tip at distance 1, turned by [degrees] (0 = up). */
private fun addPetal(path: Path, degrees: Double) {
    val a = degrees * PI / 180
    val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
    // (x across, y outwards) turned and flipped so +y points up the screen.
    fun q(x: Float, y: Float) = Offset(x * ca + y * sa, x * sa - y * ca)
    val s = q(0f, 0.04f)
    path.moveTo(s.x, s.y)
    q(-0.30f, 0.30f).let { c1 -> q(-0.48f, 0.80f).let { c2 -> q(-0.17f, 1f).let { e -> path.cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y) } } }
    q(-0.07f, 1f).let { c -> q(0f, 0.84f).let { e -> path.quadraticTo(c.x, c.y, e.x, e.y) } }
    q(0.07f, 1f).let { c -> q(0.17f, 1f).let { e -> path.quadraticTo(c.x, c.y, e.x, e.y) } }
    q(0.48f, 0.80f).let { c1 -> q(0.30f, 0.30f).let { c2 -> path.cubicTo(c1.x, c1.y, c2.x, c2.y, s.x, s.y) } }
    path.close()
}

/**
 * One petal, rounded and widest near the tip, with the small notch real cherry petals have. Built once and drawn
 * scaled, so hundreds of flowers stay cheap to draw every frame.
 */
private val PetalShape: Path by lazy { Path().also { addPetal(it, 0.0) } }

/** A whole five-petal flower around the origin, radius 1. */
private val FlowerShape: Path by lazy { Path().also { p -> repeat(5) { addPetal(p, it * 72.0) } } }

/** Trunk length and width (as fractions of the canvas) and how many times it branches, per stage. */
private fun plan(stage: Tree.Stage): Triple<Float, Float, Int> = when (stage) {
    Tree.Stage.SPROUT -> Triple(0.12f, 0.016f, 0)
    Tree.Stage.SAPLING -> Triple(0.15f, 0.026f, 2)
    Tree.Stage.YOUNG -> Triple(0.17f, 0.036f, 3)
    Tree.Stage.GROWN -> Triple(0.18f, 0.050f, 4)
    Tree.Stage.BLOSSOM -> Triple(0.19f, 0.056f, 4)
    Tree.Stage.GREAT -> Triple(0.20f, 0.074f, 4)
}

/** Grows the same tree every time for a stage, so it never jumps about between visits. */
private fun grow(stage: Tree.Stage): Shape {
    val random = Random(20_26)
    val limbs = mutableListOf<Limb>()
    val clusters = mutableListOf<Cluster>()
    val (trunk, width, depthMax) = plan(stage)
    // How full the crown is: a sapling has a few blossoms, full bloom is thick with them.
    val (perTip, flowersPer) = when (stage) {
        Tree.Stage.SAPLING -> 2 to 4
        Tree.Stage.YOUNG -> 2 to 5
        Tree.Stage.GROWN -> 2 to 6
        Tree.Stage.BLOSSOM -> 3 to 7
        Tree.Stage.GREAT -> 4 to 8
        Tree.Stage.SPROUT -> 0 to 0
    }
    val size = if (depthMax <= 2) 0.058f else 0.052f

    fun blooms(count: Int): List<Bloom> = List(count) {
        val a = random.nextFloat() * 2 * PI.toFloat()
        val d = sqrt(random.nextFloat()) * 0.85f
        Bloom(cos(a) * d, sin(a) * d * 0.85f - 0.1f, 0.016f + random.nextFloat() * 0.008f, random.nextFloat() * 72f, random.nextInt(3))
    }

    fun branch(parent: Int, depth: Int, angle: Float, length: Float, w: Float) {
        val index = limbs.size
        limbs += Limb(parent, depth, angle, length, w)
        if (depthMax > 0 && depth >= depthMax - 1) {
            val n = if (depth == depthMax) perTip else 1
            repeat(n) { k ->
                val at = if (n == 1) 0.75f else 0.45f + 0.55f * k / (n - 1)
                clusters += Cluster(
                    index, at, size * (0.85f + random.nextFloat() * 0.35f), random.nextFloat(),
                    inner = depth < depthMax, flowers = blooms(if (depth < depthMax) flowersPer / 2 else flowersPer)
                )
            }
        }
        if (depth == depthMax) return
        // Cherry trees spread wide rather than tall; a year-old great sakura spreads widest of all.
        val children = if (depth == 0) 2 else 2 + random.nextInt(2)
        val spread = 34f + depth * 8f + if (stage == Tree.Stage.GREAT) 8f else 0f
        repeat(children) { k ->
            val turn = if (children == 1) 0f else -spread + 2 * spread * k / (children - 1)
            branch(index, depth + 1, turn + random.nextFloat() * 14f - 7f, length * (0.74f + random.nextFloat() * 0.10f), w * 0.62f)
        }
    }
    branch(-1, 0, random.nextFloat() * 4f - 2f, trunk, width)
    if (depthMax >= 3) {
        // Blossom over the top of the trunk closes the gap where the two main limbs part.
        val reach = trunk * 1.2f
        listOf(0f to -0.75f, -0.3f to -0.55f, 0.3f to -0.55f, 0f to -1.0f).forEach { (fx, fy) ->
            clusters += Cluster(0, 1f, size * 1.1f, random.nextFloat() * 0.6f, inner = false, flowers = blooms(flowersPer), dx = fx * reach, dy = fy * reach)
        }
    }
    return Shape(limbs, clusters, sprout = depthMax == 0)
}

private val Water = Color(0xFF5AB3EA)

/**
 * The learner's sakura, alive: it sways, its petals drift on the wind, and as days are missed the flowers fade and
 * break off until the branches are bare. [watering] runs 0 to 1 while the can is pouring, and is 0 otherwise.
 */
@Composable
fun LivingTree(state: Tree.State, size: Dp, modifier: Modifier = Modifier, watering: Float = 0f, still: Boolean = false) {
    val shape = remember(state.stage) { grow(state.stage) }
    // [still] trees (small previews) are drawn once and do not animate, so many can share a screen.
    val wind = if (still) 0.6f else {
        val sway by rememberInfiniteTransition(label = "wind").animateFloat(
            0f, (2 * PI).toFloat(), infiniteRepeatable(tween(7_000, easing = LinearEasing), RepeatMode.Restart), label = "sway"
        )
        sway
    }
    // On a dark page the glow behind the tree is kept faint, or it reads as a grey disc.
    val dim = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.4f
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val inBloom = state.stage >= Tree.Stage.BLOSSOM &&
            (state.condition == Tree.Condition.HEALTHY || state.condition == Tree.Condition.THIRSTY) && state.leaves >= 0.99f
        halo(w, state, dim)
        island(w, state, inBloom)
        if (state.stage == Tree.Stage.GREAT) lantern(w)
        if (shape.sprout) sprout(w, state, wind) else {
            // The whole tree grows with the stage, rooted where it stands, so a year of watering shows.
            val grown = when (state.stage) {
                Tree.Stage.SAPLING -> 0.60f
                Tree.Stage.YOUNG -> 0.72f
                Tree.Stage.GROWN -> 0.82f
                Tree.Stage.BLOSSOM -> 0.92f
                else -> 1f
            }
            withTransform({ scale(grown, grown, Offset(w * 0.5f, w * 0.845f)) }) { tree(w, shape, state, wind) }
        }
        when {
            // Petals break off and fall: a steady drift in full bloom, a flurry while the tree dries out.
            inBloom -> fallingPetals(w, wind, 9, faded = false)
            state.condition == Tree.Condition.DRYING -> fallingPetals(w, wind, 14, faded = true)
            state.condition == Tree.Condition.BARE && state.daysMissed < 5 -> fallingPetals(w, wind, 3, faded = true)
            state.condition == Tree.Condition.HEALTHY && !shape.sprout -> fallingPetals(w, wind, 3, faded = false)
        }
        if (state.condition == Tree.Condition.HEALTHY) sparkles(w, wind)
        if (watering > 0f) wateringCan(w, watering)
    }
}

/** A soft glow behind the tree: pink in good health, warm when it is drying, grey when it has withered. */
private fun DrawScope.halo(w: Float, state: Tree.State, dim: Boolean) {
    val glow = when (state.condition) {
        Tree.Condition.HEALTHY -> if (state.stage == Tree.Stage.GREAT) Color(0xFFFFE9B8) else Color(0xFFFFE6EE)
        Tree.Condition.THIRSTY -> Color(0xFFF8ECEE)
        Tree.Condition.DRYING -> Color(0xFFFFE6CF)
        Tree.Condition.BARE -> Color(0xFFEDE3D6)
        Tree.Condition.WITHERED -> Color(0xFFE1DEDA)
    }
    val c = Offset(w * 0.5f, w * 0.46f)
    val strength = if (dim) 0.12f else 1f
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = strength), glow.copy(alpha = 0.6f * strength), glow.copy(alpha = 0f)), c, w * 0.48f), w * 0.48f, c)
}

/** A small floating island: a grassy top with a fringe of blades over a rocky underside, strewn with petals. */
private fun DrawScope.island(w: Float, state: Tree.State, inBloom: Boolean) {
    val dry = when (state.condition) {
        Tree.Condition.HEALTHY, Tree.Condition.THIRSTY -> 0f
        Tree.Condition.DRYING -> 0.35f
        Tree.Condition.BARE -> 0.7f
        Tree.Condition.WITHERED -> 1f
    }
    val line = lerp(Color(0xFF24502A), Color(0xFF5A4630), dry)
    val grassShadow = lerp(Color(0xFF4F9A45), Color(0xFFA48A5C), dry)
    val grass = lerp(Color(0xFF79C25A), Color(0xFFC8B07C), dry)
    val grassLit = lerp(Color(0xFFA6DD77), Color(0xFFDCCA98), dry)
    val stroke = w * 0.008f

    drawOval(Color.Black.copy(alpha = 0.06f), Offset(w * 0.30f, w * 0.965f), Size(w * 0.40f, w * 0.03f))
    val rock = Path().apply {
        moveTo(w * 0.15f, w * 0.86f)
        cubicTo(w * 0.20f, w * 0.93f, w * 0.38f, w * 0.95f, w * 0.47f, w * 0.985f)
        quadraticTo(w * 0.52f, w * 0.99f, w * 0.56f, w * 0.97f)
        cubicTo(w * 0.66f, w * 0.94f, w * 0.80f, w * 0.93f, w * 0.85f, w * 0.86f)
        close()
    }
    drawPath(rock, Brush.verticalGradient(listOf(Color(0xFF9A6A47), Color(0xFF5E3A27)), w * 0.86f, w * 0.99f))
    drawLine(Color(0xFF6E4630), Offset(w * 0.25f, w * 0.905f), Offset(w * 0.45f, w * 0.925f), stroke * 0.8f, cap = StrokeCap.Round)
    drawLine(Color(0xFF6E4630), Offset(w * 0.55f, w * 0.935f), Offset(w * 0.72f, w * 0.91f), stroke * 0.8f, cap = StrokeCap.Round)
    drawPath(rock, Color(0xFF3B2416), style = Stroke(stroke, join = StrokeJoin.Round))

    val topLeft = Offset(w * 0.13f, w * 0.80f)
    val topSize = Size(w * 0.74f, w * 0.12f)
    drawOval(grassShadow, topLeft, topSize)
    drawOval(grass, Offset(w * 0.15f, w * 0.795f), Size(w * 0.68f, w * 0.095f))
    drawOval(grassLit, Offset(w * 0.24f, w * 0.80f), Size(w * 0.36f, w * 0.045f))
    val edgeY = { x: Float -> 0.86f + 0.06f * sqrt((1f - ((x - 0.5f) / 0.37f).let { it * it }).coerceAtLeast(0f)) }
    val fringe = Path().apply {
        var x = 0.17f
        moveTo(w * x, w * edgeY(x))
        var down = true
        while (x < 0.82f) {
            x += 0.026f
            lineTo(w * x, w * (edgeY(x) + if (down) 0.014f else -0.002f))
            down = !down
        }
        lineTo(w * 0.83f, w * edgeY(0.83f))
        close()
    }
    drawPath(fringe, grassShadow)
    drawPath(fringe, line, style = Stroke(stroke * 0.7f, join = StrokeJoin.Round))
    drawOval(line, topLeft, topSize, style = Stroke(stroke))

    if (dry < 0.5f) {
        listOf(0.22f, 0.32f, 0.68f, 0.78f).forEach { gx ->
            listOf(-0.012f, 0f, 0.012f).forEach { o ->
                drawLine(grassShadow, Offset(w * (gx + o), w * 0.855f), Offset(w * (gx + o * 2.2f), w * 0.825f), w * 0.007f, cap = StrokeCap.Round)
            }
        }
    } else {
        listOf(floatArrayOf(0.30f, 0.84f, 0.35f, 0.87f), floatArrayOf(0.35f, 0.87f, 0.33f, 0.89f), floatArrayOf(0.66f, 0.835f, 0.62f, 0.865f))
            .forEach { (x1, y1, x2, y2) -> drawLine(line.copy(alpha = 0.7f), Offset(w * x1, w * y1), Offset(w * x2, w * y2), w * 0.005f, cap = StrokeCap.Round) }
    }
    // Fallen petals on the grass: a pink carpet in full bloom, brown ones once the tree has dried.
    val fallen = when {
        inBloom -> 26
        state.condition == Tree.Condition.HEALTHY || state.condition == Tree.Condition.THIRSTY -> 8
        state.condition == Tree.Condition.DRYING -> 18
        state.condition == Tree.Condition.BARE -> 14
        else -> 0
    }
    val r = Random(5)
    repeat(fallen) {
        val x = 0.20f + r.nextFloat() * 0.60f
        val y = 0.815f + r.nextFloat() * 0.07f
        val colours = if (dry > 0f) FadedPetals[r.nextInt(3)] else Petals[r.nextInt(3)]
        petal(Offset(w * x, w * y), w * 0.013f, r.nextFloat() * 360f, colours, flatten = 0.6f)
    }
}

/** A stone lantern (tōrō) beside the great sakura, its window lit. */
private fun DrawScope.lantern(w: Float) {
    val stone = Color(0xFFB9B3A8)
    val shade = Color(0xFF8F887C)
    val line = Color(0xFF4A443C)
    val x = w * 0.24f
    fun block(top: Float, bottom: Float, half: Float, fill: Color) {
        val r = androidx.compose.ui.geometry.Rect(x - w * half, w * top, x + w * half, w * bottom)
        drawRect(fill, r.topLeft, r.size)
        drawRect(line, r.topLeft, r.size, style = Stroke(w * 0.004f))
    }
    block(0.80f, 0.835f, 0.035f, shade)   // base
    block(0.72f, 0.80f, 0.012f, stone)    // post
    block(0.70f, 0.72f, 0.030f, shade)    // platform
    block(0.65f, 0.70f, 0.024f, stone)    // light box
    drawRect(Color(0xFFFFD27A), Offset(x - w * 0.012f, w * 0.660f), Size(w * 0.024f, w * 0.028f))
    drawCircle(Color(0xFFFFD27A).copy(alpha = 0.25f), w * 0.035f, Offset(x, w * 0.674f))
    val roof = Path().apply {
        moveTo(x - w * 0.045f, w * 0.655f); lineTo(x, w * 0.625f); lineTo(x + w * 0.045f, w * 0.655f); close()
    }
    drawPath(roof, shade)
    drawPath(roof, line, style = Stroke(w * 0.004f, join = StrokeJoin.Round))
    drawCircle(stone, w * 0.008f, Offset(x, w * 0.622f))
}

/** One petal drawn at [at], [length] long, turned by [angle]; [flatten] squashes it as if seen side-on. */
private fun DrawScope.petal(at: Offset, length: Float, angle: Float, colours: Pair<Color, Color>, flatten: Float = 1f) {
    withTransform({
        translate(at.x, at.y)
        rotate(angle, Offset.Zero)
        scale(length * flatten, length, Offset.Zero)
    }) {
        drawPath(PetalShape, Brush.verticalGradient(listOf(colours.first, colours.second), 0f, -1f))
        drawPath(PetalShape, Blossom.line.copy(alpha = 0.45f * colours.second.alpha), style = Stroke(0.07f))
    }
}

/** A five-petal sakura flower with a deep pink heart and a ring of stamens. */
private fun DrawScope.flower(at: Offset, radius: Float, turn: Float, colours: Pair<Color, Color>, faded: Boolean) {
    withTransform({
        translate(at.x, at.y)
        rotate(turn, Offset.Zero)
        scale(radius, radius, Offset.Zero)
    }) {
        drawPath(FlowerShape, Brush.radialGradient(listOf(colours.first, colours.second), Offset.Zero, 1f))
        drawPath(FlowerShape, (if (faded) Faded.line else Blossom.line).copy(alpha = 0.55f), style = Stroke(0.07f))
        drawCircle(if (faded) Color(0xFFB98A78) else Color(0xFFE2577A), 0.22f, Offset.Zero)
        if (!faded) repeat(5) { k ->
            val a = (k * 72 + 36) * PI / 180
            val tip = Offset((cos(a) * 0.38f).toFloat(), (sin(a) * 0.38f).toFloat())
            drawLine(Color(0xFFD6436C), Offset.Zero, tip, 0.04f)
            drawCircle(Color(0xFFF4C542), 0.07f, tip)
        }
    }
}

/** A seedling: a curved stem, two round leaves and a first pink bud. */
private fun DrawScope.sprout(w: Float, state: Tree.State, wind: Float) {
    val base = Offset(w * 0.5f, w * 0.845f)
    rotate(sin(wind) * 4f, base) {
        val top = Offset(w * 0.5f, w * 0.70f)
        val stem = Path().apply { moveTo(base.x, base.y); quadraticTo(w * 0.47f, w * 0.78f, top.x, top.y) }
        drawPath(stem, Color(0xFF24502A), style = Stroke(w * 0.022f, cap = StrokeCap.Round))
        drawPath(stem, Color(0xFF6DB04F), style = Stroke(w * 0.012f, cap = StrokeCap.Round))
        if (state.leaves > 0f) {
            leaf(top, w * 0.10f, -62f + sin(wind * 2) * 4f, Seedling, light = true)
            leaf(top, w * 0.09f, 58f + sin(wind * 2 + 1) * 4f, Seedling, light = false)
            if (state.condition == Tree.Condition.HEALTHY || state.condition == Tree.Condition.THIRSTY) {
                flower(Offset(top.x, top.y - w * 0.035f), w * 0.03f, 10f, Petals[1], faded = false)
            }
        }
    }
}

/** An outlined leaf with its base at [base], pointing at [angle] degrees (0 = up). */
private fun DrawScope.leaf(base: Offset, length: Float, angle: Float, tones: Tones, light: Boolean) {
    val a = Math.toRadians(angle.toDouble())
    val ux = sin(a).toFloat(); val uy = -cos(a).toFloat()
    fun p(along: Float, across: Float) = Offset(base.x + ux * along * length - uy * across * length, base.y + uy * along * length + ux * across * length)
    val path = Path().apply {
        moveTo(base.x, base.y)
        val l = p(0.45f, 0.45f); val r = p(0.45f, -0.45f); val tip = p(1f, 0f)
        quadraticTo(l.x, l.y, tip.x, tip.y)
        quadraticTo(r.x, r.y, base.x, base.y)
        close()
    }
    drawPath(path, if (light) tones.lit else tones.base)
    drawPath(path, tones.line, style = Stroke(length * 0.08f, join = StrokeJoin.Round))
    drawLine(tones.line.copy(alpha = 0.6f), base, p(0.8f, 0f), length * 0.05f)
}

private fun DrawScope.tree(w: Float, shape: Shape, state: Tree.State, wind: Float) {
    val withered = state.condition == Tree.Condition.WITHERED
    val bark = if (withered) DeadBark else Bark
    val sway = when (state.condition) {
        Tree.Condition.WITHERED -> 0.3f
        Tree.Condition.BARE -> 0.6f
        else -> 1.0f
    }
    val n = shape.limbs.size
    val start = Array(n) { Offset.Zero }
    val end = Array(n) { Offset.Zero }
    val heading = FloatArray(n)
    shape.limbs.forEachIndexed { i, limb ->
        val from = if (limb.parent < 0) Offset(w * 0.5f, w * 0.845f) else end[limb.parent]
        val base = if (limb.parent < 0) 0f else heading[limb.parent]
        val angle = base + limb.angle + sin(wind + limb.depth * 0.8f + i * 0.13f) * sway * limb.depth
        val a = Math.toRadians(angle.toDouble())
        heading[i] = angle
        start[i] = from
        end[i] = Offset(from.x + sin(a).toFloat() * w * limb.length, from.y - cos(a).toFloat() * w * limb.length)
    }

    fun limbPath(i: Int, extra: Float): Path {
        val limb = shape.limbs[i]
        val dx = end[i].x - start[i].x; val dy = end[i].y - start[i].y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val nx = -dy / len; val ny = dx / len
        val w1 = w * limb.width / 2 + extra; val w2 = w * limb.width * 0.62f / 2 + extra
        // Cherry limbs bend and twist; alternate the bow so they look gnarled rather than ruled.
        val bow = (if (i % 2 == 0) 1f else -1f) * w * 0.010f
        val mid = Offset((start[i].x + end[i].x) / 2 + nx * bow, (start[i].y + end[i].y) / 2 + ny * bow)
        return Path().apply {
            moveTo(start[i].x + nx * w1, start[i].y + ny * w1)
            quadraticTo(mid.x + nx * (w1 + w2) / 2, mid.y + ny * (w1 + w2) / 2, end[i].x + nx * w2, end[i].y + ny * w2)
            lineTo(end[i].x - nx * w2, end[i].y - ny * w2)
            quadraticTo(mid.x - nx * (w1 + w2) / 2, mid.y - ny * (w1 + w2) / 2, start[i].x - nx * w1, start[i].y - ny * w1)
            close()
        }
    }
    val at = { c: Cluster ->
        Offset(
            start[c.limb].x + (end[c.limb].x - start[c.limb].x) * c.at + w * c.dx,
            start[c.limb].y + (end[c.limb].y - start[c.limb].y) * c.at + w * c.dy
        )
    }
    val shown = shape.clusters.filter { it.order < state.leaves }
    val fading = { c: Cluster -> state.condition == Tree.Condition.DRYING && c.order > state.leaves * 0.4f }

    // Inner clusters sit behind the branches, so limbs read in front of the crown's depth.
    shown.filter { it.inner }.forEach { c -> mass(w, at(c), c.radius * 1.25f, if (fading(c)) Faded else Blossom, behind = true) }

    // Roots flaring into the grass.
    val foot = Offset(w * 0.5f, w * 0.845f)
    val trunkWidth = w * shape.limbs[0].width
    listOf(-1f, 1f).forEach { side ->
        val root = Path().apply {
            moveTo(foot.x + side * trunkWidth * 0.3f, foot.y - trunkWidth * 0.8f)
            quadraticTo(foot.x + side * trunkWidth * 0.7f, foot.y - trunkWidth * 0.1f, foot.x + side * trunkWidth * 1.4f, foot.y + trunkWidth * 0.15f)
            lineTo(foot.x, foot.y + trunkWidth * 0.15f)
            close()
        }
        drawPath(root, bark.line, style = Stroke(w * 0.012f, join = StrokeJoin.Round))
        drawPath(root, if (side < 0) bark.base else bark.shadow)
    }
    // The outline thins with the limb, so fine twigs stay delicate instead of tangling.
    shape.limbs.indices.forEach { i ->
        val outline = minOf(w * 0.007f, w * shape.limbs[i].width * 0.25f)
        drawPath(limbPath(i, outline), bark.line)
        drawCircle(bark.line, w * shape.limbs[i].width * 0.31f + outline, end[i])
    }
    shape.limbs.indices.forEach { i ->
        val limb = shape.limbs[i]
        drawPath(limbPath(i, 0f), bark.base)
        drawCircle(bark.base, w * limb.width * 0.31f, end[i])
        val dx = end[i].x - start[i].x; val dy = end[i].y - start[i].y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val nx = -dy / len; val ny = dx / len
        val shade = w * limb.width * 0.22f
        drawLine(bark.shadow, start[i] + Offset(nx * shade, ny * shade), end[i] + Offset(nx * shade * 0.6f, ny * shade * 0.6f), w * limb.width * 0.34f, cap = StrokeCap.Round)
        if (limb.depth <= 1) {
            drawLine(bark.lit, start[i] - Offset(nx * shade, ny * shade), end[i] - Offset(nx * shade * 0.7f, ny * shade * 0.7f), w * limb.width * 0.10f, cap = StrokeCap.Round)
            // The horizontal bands (lenticels) that make cherry bark recognisable.
            listOf(0.2f, 0.38f, 0.55f, 0.72f).forEach { t ->
                val c = Offset(start[i].x + dx * t, start[i].y + dy * t)
                val half = w * limb.width * (0.36f - 0.1f * t)
                drawLine(bark.line.copy(alpha = 0.55f), c - Offset(nx * half, ny * half), c + Offset(nx * half * 0.6f, ny * half * 0.6f), w * 0.004f, cap = StrokeCap.Round)
            }
        }
    }

    // Outer clusters: a soft pink mass, then single flowers over it, brightest on top.
    val outer = shown.filter { !it.inner }.sortedByDescending { at(it).y }
    outer.forEach { c -> mass(w, at(c), c.radius, if (fading(c)) Faded else Blossom, behind = false) }
    (shown.filter { it.inner } + outer).forEach { c ->
        val centre = at(c)
        val faded = fading(c)
        val r = w * c.radius
        c.flowers.forEach { b ->
            val colours = if (faded) FadedPetals[b.shade] else Petals[b.shade]
            flower(Offset(centre.x + b.dx * r, centre.y + b.dy * r), w * b.size, b.turn + sin(wind * 2 + b.turn) * 6f, colours, faded)
        }
    }
}

/** A soft cel-shaded mass of blossom: outline, shadow, base and a lit top; masses [behind] the branches stay dark. */
private fun DrawScope.mass(w: Float, c: Offset, radius: Float, tones: Tones, behind: Boolean) {
    val r = w * radius
    val puffs = listOf(Offset.Zero, Offset(-0.55f, 0.15f), Offset(0.55f, 0.12f), Offset(-0.25f, -0.4f), Offset(0.3f, -0.38f))
    puffs.forEach { p -> drawCircle(tones.line, r * 0.62f + w * 0.006f, c + Offset(p.x * r, p.y * r)) }
    puffs.forEach { p -> drawCircle(tones.shadow, r * 0.62f, c + Offset(p.x * r, p.y * r)) }
    if (behind) return
    puffs.forEach { p -> drawCircle(tones.base, r * 0.52f, c + Offset(p.x * r - r * 0.06f, p.y * r - r * 0.08f)) }
    puffs.take(4).forEach { p -> drawCircle(tones.lit, r * 0.30f, c + Offset(p.x * r - r * 0.14f, p.y * r - r * 0.18f)) }
}

/** Four-pointed glints that twinkle around a healthy tree. */
private fun DrawScope.sparkles(w: Float, wind: Float) {
    listOf(floatArrayOf(0.18f, 0.30f, 0.020f), floatArrayOf(0.82f, 0.24f, 0.016f), floatArrayOf(0.88f, 0.55f, 0.013f), floatArrayOf(0.12f, 0.60f, 0.015f), floatArrayOf(0.62f, 0.08f, 0.012f))
        .forEachIndexed { k, (x, y, s) ->
            val alpha = ((sin(wind * 3 + k * 1.7f) + 1f) / 2f).coerceIn(0f, 1f)
            if (alpha < 0.05f) return@forEachIndexed
            val c = Offset(w * x, w * y)
            val r = w * s * (0.7f + 0.3f * alpha)
            val star = Path().apply {
                moveTo(c.x, c.y - r)
                quadraticTo(c.x, c.y, c.x + r, c.y)
                quadraticTo(c.x, c.y, c.x, c.y + r)
                quadraticTo(c.x, c.y, c.x - r, c.y)
                quadraticTo(c.x, c.y, c.x, c.y - r)
                close()
            }
            drawPath(star, Color(0xFFFFF3F7).copy(alpha = alpha))
            drawCircle(Color.White.copy(alpha = alpha), r * 0.25f, c)
        }
}

/**
 * Petals that have broken off, fluttering down: each spins and flips (squashed side-on and back) as it falls,
 * drifting with the wind. [faded] petals are the brownish ones of a drying tree.
 */
private fun DrawScope.fallingPetals(w: Float, wind: Float, count: Int, faded: Boolean) {
    repeat(count) { k ->
        val phase = ((wind / (2 * PI)).toFloat() * 1.6f + k / count.toFloat()) % 1f
        val x = 0.18f + 0.64f * ((k * 0.37f) % 1f) + sin(phase * 2 * PI.toFloat() * 2 + k) * 0.05f + phase * 0.08f
        val y = 0.18f + 0.66f * phase
        val flip = 0.25f + 0.75f * abs(cos(phase * 2 * PI.toFloat() * 3 + k))
        val colours = (if (faded) FadedPetals else Petals)[k % 3]
        val fade = 1f - phase * 0.45f
        petal(Offset(w * x, w * y), w * 0.022f, phase * 400f + k * 47f, colours.first.copy(alpha = fade) to colours.second.copy(alpha = fade), flatten = flip)
    }
}

/** A watering can that tips in, pours a stream of drops on the roots, then lifts away. */
private fun DrawScope.wateringCan(w: Float, t: Float) {
    val tilt = when {
        t < 0.15f -> t / 0.15f
        t > 0.85f -> (1f - t) / 0.15f
        else -> 1f
    } * 38f
    val pivot = Offset(w * 0.80f, w * 0.30f)
    val line = Color(0xFF23406A)
    rotate(-tilt, pivot) {
        val body = Path().apply {
            moveTo(w * 0.72f, w * 0.24f); lineTo(w * 0.88f, w * 0.24f); lineTo(w * 0.86f, w * 0.36f); lineTo(w * 0.74f, w * 0.36f); close()
        }
        drawLine(line, Offset(w * 0.74f, w * 0.30f), Offset(w * 0.62f, w * 0.22f), w * 0.026f, cap = StrokeCap.Round)
        drawLine(Color(0xFF6AA3DE), Offset(w * 0.74f, w * 0.30f), Offset(w * 0.62f, w * 0.22f), w * 0.014f, cap = StrokeCap.Round)
        drawPath(body, Color(0xFF5B95D6))
        drawRect(Color(0xFF8DBDEB), Offset(w * 0.745f, w * 0.25f), Size(w * 0.03f, w * 0.10f))
        drawPath(body, line, style = Stroke(w * 0.008f, join = StrokeJoin.Round))
        drawArc(line, 180f, 180f, false, Offset(w * 0.76f, w * 0.18f), Size(w * 0.08f, w * 0.10f), style = Stroke(w * 0.014f))
        drawCircle(line, w * 0.02f, Offset(w * 0.62f, w * 0.22f))
    }
    if (t in 0.15f..0.85f) {
        val spout = Offset(w * 0.58f, w * 0.30f)
        repeat(10) { k ->
            val f = ((t - 0.15f) / 0.7f * 4f + k / 10f) % 1f
            val x = spout.x + (w * 0.50f - spout.x) * f
            val y = spout.y + (w * 0.84f - spout.y) * f * f
            drawCircle(Water, w * 0.010f, Offset(x, y))
            drawCircle(Color.White.copy(alpha = 0.7f), w * 0.004f, Offset(x - w * 0.003f, y - w * 0.003f))
        }
    }
}
