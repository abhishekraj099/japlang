package com.example.jap.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.jap.data.Tree
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.geometry.Offset
import com.example.jap.ui.theme.isAppDark
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Progress
import java.text.DateFormatSymbols
import java.util.Calendar

private val Box14 = RoundedCornerShape(14.dp)

/**
 * Shown once before the main app: how the streak works, an optional streak goal,
 * and how an omamori protects a missed day.
 */
@Composable
fun StreakIntro(progress: Progress, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var page by rememberSaveable { mutableIntStateOf(0) }
    var choice by rememberSaveable { mutableIntStateOf(0) }

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            AnimatedContent(page, Modifier.weight(1f), label = "page") { current ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (current) {
                        0 -> {
                            LivingTree(Tree.State(Tree.Stage.BLOSSOM, Tree.Condition.HEALTHY, 1f, 30, 0, false, true, 40, 0), 240.dp)
                            IntroText(
                                "Study, water, grow",
                                "Each day you study fills your watering can. Water your tree and it grows from a sprout into a blossoming sakura. Skip days and it dries out."
                            )
                        }
                        1 -> {
                            LivingTree(Tree.State(Tree.Stage.SPROUT, Tree.Condition.THIRSTY, 1f, 0, 0, false, false, 0, 1), 170.dp)
                            IntroText("Set a streak goal", "How many days in a row will you study? Reach it to earn drops.")
                            Config.streakGoals.forEachIndexed { i, goal ->
                                val selected = i == choice
                                Row(
                                    Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(Box14)
                                        .background(if (selected) scheme.primaryContainer else scheme.surface)
                                        .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outline, Box14)
                                        .clickable(role = Role.RadioButton) { choice = i }
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${goal.days} days",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("+${goal.reward}", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                                    DropIcon(22.dp, Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                        else -> {
                            OmamoriIcon(150.dp)
                            IntroText(
                                "Miss a day without losing your streak",
                                "An omamori shields your streak for one missed day. Earn drops by studying and exchange " +
                                    "${Config.protectionCost} of them for an omamori. You can carry up to ${Config.maxProtection}."
                            )
                        }
                    }
                }
            }
            PushButton(
                when (page) {
                    0 -> "Continue"
                    1 -> "Commit to my goal"
                    else -> "Start learning"
                },
                onClick = {
                    when (page) {
                        1 -> {
                            progress.commitStreakGoal(Config.streakGoals[choice].days)
                            page++
                        }
                        2 -> {
                            progress.finishStreakIntro()
                            onDone()
                        }
                        else -> page++
                    }
                }
            )
            if (page == 1) {
                TextButton(onClick = { page++ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Not now")
                }
            }
        }
    }
}

@Composable
private fun IntroText(title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = scheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp)
    )
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = scheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
}

private val Sky = Color(0xFFCFE6FA)
private val SkyFade = Color(0xFFF2F8FD)
private val NightSky = Color(0xFF141B3D)
private val NightFade = Color(0xFF262C5A)
private val Moon = Color(0xFFFFF1C9)
private val Leaf = Color(0xFF6DB33F)
private val Pink = Color(0xFFE8829C)
private val Blue = Color(0xFF3D8FDB)

/** Streak details: the tree, this month's numbers, the streak goal, a calendar and the wallet. */
@Composable
fun StreakScreen(progress: Progress, onBack: () -> Unit, onStudy: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    // Months back from the current one.
    var monthsBack by rememberSaveable { mutableIntStateOf(0) }
    val month = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        add(Calendar.MONTH, -monthsBack)
    }
    val firstDay = Progress.dayOf(month)
    val length = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthDays = (firstDay until firstDay + length)
    val studiedThisMonth = monthDays.count { it in progress.studiedDays }
    val savedThisMonth = monthDays.count { it in progress.protectedDays }

    BackHandler(onBack = onBack)
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box {
                // Sky behind the top of the page; it scrolls away with the header. A day sky in light mode and a
                // night sky in dark mode, so the theme's text always reads against it.
                val night = isAppDark()
                Canvas(Modifier.matchParentSize()) {
                    if (night) nightSky(scheme.background) else {
                        drawRect(Brush.verticalGradient(listOf(Sky, SkyFade, scheme.background)))
                        cloud(size.width * 0.06f, size.height * 0.26f, 1.1f)
                        // Kept below the title row so it never sits on top of "Streak".
                        cloud(size.width * 0.66f, size.height * 0.19f, 1.3f)
                        cloud(size.width * 0.70f, size.height * 0.48f, 1.0f)
                    }
                }
            Column(Modifier.statusBarsPadding().padding(bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                    Text(
                        "Streak",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    GuideHelpButton("streak", Modifier.padding(end = 4.dp), container = Color.White.copy(alpha = 0.9f), content = Color(0xFF1B1B22), visual = 48.dp)
                    RoundButton(Icons.Rounded.IosShare, "Share") {
                        val text = "I'm on a ${progress.streak}-day Japanese study streak!"
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    Text("${progress.streak}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Text(
                        "day streak",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground,
                        modifier = Modifier.padding(start = 10.dp, bottom = 12.dp)
                    )
                }
                TreeCard(progress, onStudy)
            }
            }
                Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatTile(Modifier.weight(1f).fillMaxHeight(), "$studiedThisMonth", "Days studied this month") { SakuraIcon(30.dp) }
                        StatTile(Modifier.weight(1f).fillMaxHeight(), "$savedThisMonth", "Days saved") { OmamoriIcon(30.dp) }
                    }
                    JourneyCard(progress)
                    GoalCard(progress)
                    CalendarCard(progress, month, firstDay, length, monthsBack, onMonth = { monthsBack = it })
                    WalletCard(progress)
                }
        }
        // Once the page scrolls, a strip under the status bar keeps the clock from sitting on the cards.
        val scrim by animateFloatAsState(if (scroll.value > 0) 1f else 0f, label = "statusScrim")
        Box(
            Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = scrim }.background(scheme.background)
        )
        }
    }
}

/** The learner's tree with what it needs today, a button to water it, and how far it is from growing. */
@Composable
private fun TreeCard(progress: Progress, onStudy: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tree = progress.tree
    val scope = rememberCoroutineScope()
    val pour = remember { Animatable(0f) }
    var justWatered by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GuideTour("streak")
        LivingTree(tree, 260.dp, Modifier.guideTarget("tree"), watering = pour.value)
        Text(
            "${stageName(tree.stage)} · ${tree.waterings} ${if (tree.waterings == 1) "watering" else "waterings"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onBackground
        )
        if (tree.toNextStage > 0) {
            val next = Tree.Stage.entries[tree.stage.ordinal + 1]
            Text(
                "${tree.toNextStage} more to grow into a ${stageName(next).lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Text(
            when {
                justWatered -> "Your tree drank it all up. See you tomorrow!"
                tree.canWater -> "Your watering can is full. Water your tree to help it grow."
                tree.wateredToday -> "Watered today. Come back tomorrow to keep it growing."
                else -> when (tree.condition) {
                    Tree.Condition.HEALTHY -> "Your tree is doing well today."
                    Tree.Condition.THIRSTY -> "Your tree is thirsty. Finish a lesson to fill your watering can."
                    Tree.Condition.DRYING -> "Your tree is drying out and its leaves are falling. Study today to save it."
                    Tree.Condition.BARE -> "Your tree has lost its leaves after ${tree.daysMissed} days without study. Study to bring them back."
                    Tree.Condition.WITHERED -> "Your tree has withered, but it isn't gone. Study and water it to bring it back."
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
        )
        when {
            tree.canWater -> GradientButton("Water your tree", Icons.Rounded.WaterDrop, enabled = !pour.isRunning) {
                scope.launch {
                    pour.snapTo(0f)
                    pour.animateTo(1f, tween(2_400, easing = LinearEasing))
                    progress.waterTree()
                    justWatered = true
                    pour.snapTo(0f)
                }
            }
            !progress.studiedToday -> GradientButton("Study now", onClick = onStudy)
        }
    }
}

private fun stageName(stage: Tree.Stage) = when (stage) {
    Tree.Stage.SPROUT -> "Sprout"
    Tree.Stage.SAPLING -> "Sapling"
    Tree.Stage.YOUNG -> "Young tree"
    Tree.Stage.GROWN -> "Full tree"
    Tree.Stage.BLOSSOM -> "Blossoming sakura"
    Tree.Stage.GREAT -> "Great sakura"
}

/**
 * How the tree changes over time: one row with the tree at each milestone of a year of watering, the learner's
 * current stage marked, and one row with what a few missed days do to it. The small trees are still pictures.
 */
@Composable
private fun JourneyCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val tree = progress.tree
    fun still(stage: Tree.Stage, condition: Tree.Condition = Tree.Condition.HEALTHY, leaves: Float = 1f, missed: Int = 0) =
        Tree.State(stage, condition, leaves, 0, missed, false, true, 0, 0)
    Card {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text("Your tree's journey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Text("Water it each day you study. Here's how it grows over a year.", style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            Row(
                Modifier.fillMaxWidth()
                    // The right edge fades out, so stages beyond it read as "scroll for more" rather than cut off.
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.horizontalGradient(0.8f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                    }
                    .horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Tree.Stage.entries.forEachIndexed { i, stage ->
                    val days = Config.treeStages.getOrElse(i) { 0 }
                    val here = stage == tree.stage
                    Column(
                        Modifier.width(92.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (here) scheme.primaryContainer else Color.Transparent)
                            .border(if (here) 2.dp else 0.dp, if (here) scheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LivingTree(still(stage), 80.dp, still = true)
                        Text(if (days == 0) "Start" else "Day $days", fontWeight = FontWeight.Bold,
                            color = if (here) scheme.onPrimaryContainer else scheme.onSurface, fontSize = 14.sp)
                        Text(if (here) "You're here" else stageName(stage), fontSize = 11.sp, textAlign = TextAlign.Center,
                            color = if (here) scheme.onPrimaryContainer else scheme.onSurfaceVariant, maxLines = 2, lineHeight = 13.sp)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = scheme.outlineVariant)
            Text("If you skip days", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            // Shown on the learner's own tree, at least a young one so the change is visible.
            val shown = maxOf(tree.stage, Tree.Stage.YOUNG)
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    Triple(still(shown, Tree.Condition.THIRSTY), "1 day", "Thirsty"),
                    Triple(still(shown, Tree.Condition.DRYING, 0.5f, 1), "2 days", "Petals fall"),
                    Triple(still(shown, Tree.Condition.BARE, 0f, 3), "4 days", "Bare"),
                    Triple(still(shown, Tree.Condition.WITHERED, 0f, 8), "8 days", "Withered")
                ).forEach { (state, days, what) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                        LivingTree(state, 72.dp, still = true)
                        Text(days, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.onSurface)
                        Text(what, fontSize = 11.sp, color = scheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Studying again brings it back to life within 3 days.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

/** A night sky fading into the page: deep indigo, a scatter of stars, a crescent moon and faint clouds. */
private fun DrawScope.nightSky(page: Color) {
    drawRect(Brush.verticalGradient(listOf(NightSky, NightFade, page), 0f, size.height))
    val stars = listOf(
        0.08f to 0.20f, 0.18f to 0.36f, 0.30f to 0.14f, 0.42f to 0.30f, 0.12f to 0.52f, 0.86f to 0.40f,
        0.62f to 0.20f, 0.74f to 0.34f, 0.92f to 0.18f, 0.55f to 0.44f, 0.24f to 0.62f, 0.80f to 0.58f
    )
    stars.forEachIndexed { i, (x, y) ->
        val r = size.width * (if (i % 3 == 0) 0.0045f else 0.003f)
        drawCircle(Color.White.copy(alpha = if (i % 2 == 0) 0.75f else 0.4f), r, Offset(size.width * x, size.height * y))
    }
    // A crescent moon on the left, between the back button and the streak count: a soft halo, then a crescent cut as one shape, so
    // nothing dark is painted over the sky.
    val moon = Offset(size.width * 0.14f, size.height * 0.22f)
    val r = size.width * 0.035f
    drawCircle(Brush.radialGradient(listOf(Moon.copy(alpha = 0.22f), Moon.copy(alpha = 0f)), moon, r * 3f), r * 3f, moon)
    val crescent = Path().apply {
        op(
            Path().apply { addOval(androidx.compose.ui.geometry.Rect(moon, r)) },
            Path().apply { addOval(androidx.compose.ui.geometry.Rect(moon + Offset(r * 0.45f, -r * 0.25f), r * 0.85f)) },
            PathOperation.Difference
        )
    }
    drawPath(crescent, Moon)
    cloud(size.width * 0.70f, size.height * 0.30f, 1.0f, Color(0xFF3B4378).copy(alpha = 0.55f))
    cloud(size.width * 0.58f, size.height * 0.50f, 0.9f, Color(0xFF3B4378).copy(alpha = 0.45f))
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1B1B22))
    }
}

/** White card with a soft bottom edge, as used across the streak page. */
@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.outlineVariant)
            .padding(bottom = 3.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
    ) { content() }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String, icon: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(start = 8.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun GoalCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    val goal = Config.streakGoals.firstOrNull { it.days == progress.streakGoal }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (goal == null || progress.goalClaimed) {
                Text(
                    if (goal != null) "Goal of ${goal.days} days reached. Pick a new one:" else "Set a streak goal",
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Config.streakGoals.forEach { option ->
                        OptionCard("${option.days}d", OptionState.IDLE, { progress.commitStreakGoal(option.days) }, Modifier.weight(1f))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Goal: ${goal.days} days in a row", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.weight(1f))
                    Text("+${goal.reward}", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    DropIcon(20.dp, Modifier.padding(start = 4.dp))
                }
                Meter(progress.streak / goal.days.toFloat(), scheme.secondary, Modifier.padding(top = 10.dp), height = 8.dp)
                Text(
                    "${progress.streak.coerceAtMost(goal.days)} of ${goal.days} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(progress: Progress, month: Calendar, firstDay: Long, length: Int, monthsBack: Int, onMonth: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val today = progress.day
    val monthName = DateFormatSymbols.getInstance().months[month.get(Calendar.MONTH)]
    // Monday-first column of the 1st.
    val lead = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$monthName ${month.get(Calendar.YEAR)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MonthButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", true) { onMonth(monthsBack + 1) }
                MonthButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", monthsBack > 0) { onMonth(monthsBack - 1) }
            }
            Row(Modifier.padding(top = 14.dp)) {
                weekdays.forEach {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            (0 until (lead + length + 6) / 7).forEach { week ->
                Row(Modifier.padding(top = 8.dp)) {
                    (0 until 7).forEach { column ->
                        val date = week * 7 + column - lead + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date in 1..length) {
                                val day = firstDay + date - 1
                                val studied = day in progress.studiedDays
                                val saved = day in progress.protectedDays
                                when {
                                    studied -> BlossomOutline(44.dp, Pink, filled = Pink.copy(alpha = 0.18f))
                                    saved -> BlossomOutline(44.dp, Blue, filled = Blue.copy(alpha = 0.12f))
                                    day == today -> Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.surfaceVariant))
                                }
                                Text(
                                    "$date",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (studied || saved || day == today) FontWeight.Bold else FontWeight.Normal,
                                    // The blossom carries the colour; the number stays in the text colour so it reads on top of it.
                                    color = when {
                                        studied || saved -> scheme.onSurface
                                        day > today -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
                                        else -> scheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BlossomOutline(16.dp, Pink)
                Text("Studied", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                BlossomOutline(16.dp, Blue, Modifier.padding(start = 18.dp))
                Text("Saved by omamori", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun MonthButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .padding(start = 8.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(scheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) scheme.onSurface else scheme.outline)
    }
}

@Composable
private fun WalletCard(progress: Progress) {
    val scheme = MaterialTheme.colorScheme
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DropIcon(40.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("${progress.coins}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue)
                    Text("Water drops", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                OmamoriIcon(44.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("${progress.omamori} / ${Config.maxProtection}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue)
                    Text("Omamori", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = scheme.outlineVariant, modifier = Modifier.padding(vertical = 14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append("Exchange ")
                        withStyle(SpanStyle(color = Blue, fontWeight = FontWeight.Bold)) { append("${Config.protectionCost}") }
                        append(" drops for 1 omamori")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val can = progress.canBuyOmamori()
                Text(
                    "Exchange",
                    fontWeight = FontWeight.Bold,
                    color = if (can) Color.White else scheme.outline,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (can) Blue else scheme.surfaceVariant)
                        .clickable(enabled = can, role = Role.Button) { progress.buyOmamori() }
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                )
            }
            Text(
                "An omamori saves your streak on a day you miss.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
