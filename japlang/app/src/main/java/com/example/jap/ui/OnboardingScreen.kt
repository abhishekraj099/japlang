package com.example.jap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.data.Config
import com.example.jap.data.Progress

private const val STEPS = 3

/** First-run setup: name, daily goal, then either start from zero or take the placement test. */
@Composable
fun OnboardingScreen(progress: Progress, onDone: (takeTest: Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(progress.name) }
    var goal by rememberSaveable { mutableIntStateOf(progress.goal) }
    var knowsSome by rememberSaveable { mutableStateOf<Boolean?>(null) }

    BackHandler(enabled = step > 0) { step-- }

    fun next() {
        when (step) {
            0 -> progress.updateName(name)
            1 -> progress.updateGoal(goal)
            else -> {
                onDone(knowsSome == true)
                return
            }
        }
        step++
    }

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(STEPS) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) scheme.primary else scheme.surfaceVariant)
                    )
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
                when (step) {
                    0 -> {
                        Heading("What should we call you?", "This is the name shown on your profile and the points table.")
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(24) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Your name") },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    1 -> {
                        Heading("Pick a daily goal", "Small and steady beats long and rare. You can change this later.")
                        Config.goals.forEach { option ->
                            OptionRow(
                                title = "${option.name} · ${option.xp} XP",
                                body = option.time,
                                selected = goal == option.xp,
                                onClick = { goal = option.xp }
                            )
                        }
                    }
                    else -> {
                        Heading("How much Japanese do you know?", "We'll start you in the right place.")
                        OptionRow(
                            title = "I'm new to Japanese",
                            body = "Start from the very first sounds.",
                            selected = knowsSome == false,
                            onClick = { knowsSome = false },
                            icon = Icons.Rounded.School
                        )
                        OptionRow(
                            title = "I know some already",
                            body = "Take a short test that gets harder as you answer correctly, and skip what you know.",
                            selected = knowsSome == true,
                            onClick = { knowsSome = true },
                            icon = Icons.Rounded.AutoAwesome
                        )
                    }
                }
            }
            PushButton(
                when {
                    step < STEPS - 1 -> "Continue"
                    knowsSome == true -> "Start the test"
                    else -> "Start learning"
                },
                ::next,
                enabled = when (step) {
                    0 -> name.isNotBlank()
                    1 -> true
                    else -> knowsSome != null
                }
            )
        }
    }
}

@Composable
private fun Heading(title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = scheme.onBackground)
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    )
}

@Composable
private fun OptionRow(title: String, body: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else scheme.surface)
            .border(2.dp, if (selected) scheme.primary else scheme.outline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp).size(32.dp)
            )
        }
        Column {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurface
            )
            Text(body, color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant)
        }
    }
}
