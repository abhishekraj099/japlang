package com.example.jap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Ai,
    onPrimary = Color.White,
    primaryContainer = AiPale,
    onPrimaryContainer = AiDeep,
    secondary = Shu,
    onSecondary = Color.White,
    secondaryContainer = ShuPale,
    onSecondaryContainer = ShuDeep,
    tertiary = Matsuba,
    onTertiary = Color.White,
    tertiaryContainer = MatsubaPale,
    onTertiaryContainer = MatsubaDeep,
    error = Enji,
    onError = Color.White,
    errorContainer = EnjiPale,
    onErrorContainer = EnjiDeep,
    background = Washi,
    onBackground = Sumi,
    surface = WashiBright,
    onSurface = Sumi,
    surfaceVariant = WashiDim,
    onSurfaceVariant = SumiSoft,
    surfaceContainer = WashiBright,
    surfaceContainerHigh = WashiBright,
    outline = Kinari,
    outlineVariant = KinariSoft
)

private val DarkColorScheme = darkColorScheme(
    primary = AiLit,
    onPrimary = AiDeep,
    primaryContainer = AiNight,
    onPrimaryContainer = AiPale,
    secondary = ShuLit,
    onSecondary = Color(0xFF4A0D08),
    secondaryContainer = ShuNight,
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = MatsubaLit,
    onTertiary = Color(0xFF06331B),
    tertiaryContainer = MatsubaNight,
    onTertiaryContainer = Color(0xFFC7F0D3),
    error = EnjiLit,
    onError = Color(0xFF4A0D08),
    errorContainer = EnjiNight,
    onErrorContainer = Color(0xFFFFDAD6),
    background = Yoru,
    onBackground = Tsuki,
    surface = YoruRaised,
    onSurface = Tsuki,
    surfaceVariant = YoruDim,
    onSurfaceVariant = TsukiSoft,
    surfaceContainer = YoruRaised,
    surfaceContainerHigh = YoruDim,
    outline = YoruRule,
    outlineVariant = YoruRuleSoft
)

@Composable
fun JapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
