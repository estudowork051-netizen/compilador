package com.lifeorganizer.app.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// PALETA OFICIAL SAGE GREEN & NATURE (FASE B)
// ==========================================
val BgApp = Color(0xFFFDFBF7)
val PrimarySage = Color(0xFF75937B)
val PrimaryDark = Color(0xFF465B4B)
val SageLight = Color(0xFFE8EFE9)
val SageAccent = Color(0xFFA3B899)
val CreamCard = Color(0xFFF7F4EC)
val TextMain = Color(0xFF2C352E)
val TextMuted = Color(0xFF6B7A6E)

// Aliases for system compatibility
val SageGreenPrimary = PrimarySage
val SageGreenLight = SageAccent
val SoftOlive = PrimaryDark
val WarmCream = BgApp
val SoftWhite = Color(0xFFFFFFFF)
val ForestSlateDark = Color(0xFF1C221D)
val DeepOrganicDark = Color(0xFF141915)
val DarkSurfaceVariantNature = Color(0xFF263028)

// Dark Theme Colors
val DarkBackground = DeepOrganicDark
val DarkSurface = ForestSlateDark
val DarkSurfaceVariant = DarkSurfaceVariantNature
val DarkPrimary = SageAccent
val DarkPrimaryVariant = PrimarySage
val DarkSecondary = PrimaryDark
val DarkOnBackground = Color(0xFFF1F5F2)
val DarkOnSurface = Color(0xFFF1F5F2)
val DarkOnSurfaceVariant = Color(0xFFA8B4AB)

// Light Theme Colors
val LightBackground = BgApp
val LightSurface = CreamCard
val LightSurfaceVariant = SageLight
val LightPrimary = PrimarySage
val LightPrimaryVariant = PrimaryDark
val LightSecondary = SageAccent
val LightOnBackground = TextMain
val LightOnSurface = TextMain
val LightOnSurfaceVariant = TextMuted

// Functional Accent Colors
val AccentSuccess = Color(0xFF48A268)
val AccentWarning = Color(0xFFE08A28)
val AccentError = Color(0xFFD34543)
val CardBorderDark = Color(0xFF354438)
val CardBorderLight = Color(0xFFE2EBE3)

// Environment Pill Gradients (B4.2)
val MorningGradientLight = Brush.linearGradient(listOf(Color(0xFFE8EFE9), Color(0xFFFDFBF7)))
val AfternoonGradientLight = Brush.linearGradient(listOf(Color(0xFFDDE9D9), Color(0xFFF7F4EC)))
val NightGradientLight = Brush.linearGradient(listOf(Color(0xFF465B4B), Color(0xFF75937B)))

val MorningGradientDark = Brush.linearGradient(listOf(Color(0xFF1E2B21), Color(0xFF232E20)))
val AfternoonGradientDark = Brush.linearGradient(listOf(Color(0xFF232E20), Color(0xFF1B282C)))
val NightGradientDark = Brush.linearGradient(listOf(Color(0xFF151D18), Color(0xFF11171E)))
