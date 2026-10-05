package com.example.jap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jap.audio.LocalSpeaker

private val ButtonShape = RoundedCornerShape(14.dp)
private val CardShape = RoundedCornerShape(16.dp)

fun Color.darken(factor: Float = 0.78f) = Color(red * factor, green * factor, blue * factor, alpha)

/** Ink that reads on top of this colour. */
private fun Color.ink() = if (luminance() > 0.5f) Color(0xFF1B1B22) else Color.White

/** The app's main button: a solid bar that darkens while pressed. */
@Composable
fun PushButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = color.ink()
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scheme = MaterialTheme.colorScheme
    val fill = when {
        !enabled -> scheme.surfaceVariant
        pressed -> color.darken(0.86f)
        else -> color
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(ButtonShape)
            .background(fill)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) contentColor else scheme.onSurfaceVariant,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

enum class OptionState { IDLE, SELECTED, CORRECT, WRONG, DIM }

@Composable
fun OptionCard(
    text: String,
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    big: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val (fill, line, ink) = when (state) {
        OptionState.IDLE -> Triple(scheme.surface, scheme.outline, scheme.onSurface)
        OptionState.SELECTED -> Triple(scheme.primaryContainer, scheme.primary, scheme.onPrimaryContainer)
        OptionState.CORRECT -> Triple(scheme.tertiaryContainer, scheme.tertiary, scheme.onTertiaryContainer)
        OptionState.WRONG -> Triple(scheme.errorContainer, scheme.error, scheme.onErrorContainer)
        OptionState.DIM -> Triple(scheme.surfaceVariant, scheme.outlineVariant, scheme.onSurfaceVariant)
    }
    Box(
        modifier
            .clip(CardShape)
            .background(fill)
            .border(1.5.dp, line, CardShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = if (big) 18.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = ink,
            fontSize = if (big) 34.sp else 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SpeakButton(text: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val speaker = LocalSpeaker.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClickLabel = "Play audio", role = Role.Button) { speaker.speak(text) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Softly raised surface used for every card in the app. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surface,
    line: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .shadow(2.dp, CardShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(CardShape)
            .background(fill)
            .border(1.dp, line, CardShape)
    ) { content() }
}

@Composable
fun StatCard(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Meter(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Full-screen page opened from a tab, with a back arrow and title. */
@Composable
fun SubScreen(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    BackHandler(onBack = onBack)
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Back", role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = scheme.onBackground)
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onBackground, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            content()
        }
    }
}
