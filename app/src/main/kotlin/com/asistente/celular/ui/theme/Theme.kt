package com.asistente.celular.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Constantes de diseño del sistema "OLED Void & Neon Purple Monolith"
val VoidBlack = Color(0xFF000000)
val VoidSurface = Color(0xFF090611)
val VoidSurfaceElevated = Color(0xFF130D22)
val VoidBorder = Color(0xFF26163D)
val VoidBorderHighlight = Color(0xFFA855F7)

val NeonPurple = Color(0xFFA855F7)
val NeonPurpleDim = Color(0xFF7C3AED)
val NeonPurpleGlow = Color(0x33A855F7)

val NeonViolet = Color(0xFF7C3AED)
val NeonLilac = Color(0xFFC084FC)
val NeonMagenta = Color(0xFFD946EF)

// Compatibilidad con invocadores heredados: reasignados a la paleta morada neón
val NeonCyan = Color(0xFFA855F7)
val NeonCyanDim = Color(0xFF7C3AED)
val NeonCyanGlow = Color(0x33A855F7)

val NeonAmber = Color(0xFFFFB800)
val NeonAmberGlow = Color(0x33FFB800)

val NeonGreen = Color(0xFF00FF66)
val NeonRed = Color(0xFFFF3366)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB4A5C7)
val TextMuted = Color(0xFF6B5880)

data class OledColors(
    val voidBlack: Color = VoidBlack,
    val voidSurface: Color = VoidSurface,
    val voidSurfaceElevated: Color = VoidSurfaceElevated,
    val voidBorder: Color = VoidBorder,
    val voidBorderHighlight: Color = VoidBorderHighlight,
    val neonPrimary: Color = NeonPurple,
    val neonPrimaryDim: Color = NeonViolet,
    val neonPrimaryGlow: Color = NeonPurpleGlow,
    val neonCyan: Color = NeonPurple,
    val neonCyanDim: Color = NeonViolet,
    val neonCyanGlow: Color = NeonPurpleGlow,
    val neonAmber: Color = NeonAmber,
    val neonAmberGlow: Color = NeonAmberGlow,
    val neonGreen: Color = NeonGreen,
    val neonRed: Color = NeonRed,
    val neonPurple: Color = NeonPurple,
    val neonPurpleGlow: Color = NeonPurpleGlow,
    val neonViolet: Color = NeonViolet,
    val neonLilac: Color = NeonLilac,
    val neonMagenta: Color = NeonMagenta,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted
)

val LocalOledColors = staticCompositionLocalOf { OledColors() }

object OledTheme {
    val colors: OledColors
        @Composable
        get() = LocalOledColors.current
}

private val OledVoidDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2E1065),
    onPrimaryContainer = NeonLilac,
    secondary = NeonViolet,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = NeonLilac,
    onTertiary = Color.Black,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = VoidSurface,
    onSurface = TextPrimary,
    surfaceVariant = VoidSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = VoidBorder,
    outlineVariant = Color(0x44A855F7),
    error = NeonRed,
    onError = Color.Black,
    errorContainer = Color(0xFF2A0810),
    onErrorContainer = NeonRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    secondary = Color(0xFFA855F7),
    tertiary = Color(0xFFC084FC),
    background = Color(0xFFFAF5FF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E1035),
    onSurface = Color(0xFF1E1035)
)

@Composable
fun AsistenteTheme(
    darkTheme: Boolean = true, // Por defecto estética OLED Void de alto contraste
    dynamicColor: Boolean = false, // Desactivado para preservar la identidad OLED Void
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OledVoidDarkColorScheme
        else -> LightColorScheme
    }

    val oledColors = rememberOledColors()

    CompositionLocalProvider(LocalOledColors provides oledColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
fun rememberOledColors(): OledColors = OledColors()
