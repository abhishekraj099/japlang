package com.example.jap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

// Headings are set in the system serif, which renders Japanese in a Mincho face;
// running text stays in the sans for legibility at small sizes.
private val Serif = FontFamily.Serif

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Serif),
    displayMedium = base.displayMedium.copy(fontFamily = Serif),
    displaySmall = base.displaySmall.copy(fontFamily = Serif),
    headlineLarge = base.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(letterSpacing = 0.2.sp),
    bodyMedium = base.bodyMedium.copy(letterSpacing = 0.1.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
    labelMedium = base.labelMedium.copy(letterSpacing = 0.8.sp)
)
