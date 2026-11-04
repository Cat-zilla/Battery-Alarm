package com.im_atp.volthalt.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark theme — pitch-black OLED background with a periwinkle blue accent.
private val PitchBlackColorScheme = darkColorScheme(
    primary            = Color(0xFFE8E8E8),
    onPrimary          = Color(0xFF161616),
    primaryContainer   = Color(0xFF363636),
    onPrimaryContainer = Color(0xFFF0F0F0),
    secondary          = Color(0xFFBDBDBD),
    onSecondary        = Color(0xFFFFFFFF),
    tertiary           = Color(0xFFC7C7C7),
    onTertiary         = Color(0xFF000000),
    background         = Color(0xFF000000),
    surface            = Color(0xFF1C1C1C),
    surfaceVariant     = Color(0xFF292929),
    onBackground       = Color(0xFFFFFFFF),
    onSurface          = Color(0xFFFFFFFF),
    onSurfaceVariant   = Color(0xFFC7C7C7),
    outline            = Color(0xFF4A4A4A),
    outlineVariant     = Color(0xFF363636)
)

// Light theme — clean off-white background with a rich royal blue accent.
private val ProfessionalLightColorScheme = lightColorScheme(
    primary            = Color(0xFF262626),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF202020),
    secondary          = Color(0xFF555555),
    onSecondary        = Color(0xFFFFFFFF),
    tertiary           = Color(0xFF666666),
    onTertiary         = Color(0xFFFFFFFF),
    background         = Color(0xFFFAFAFA),
    surface            = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFFF0F0F0),
    onBackground       = Color(0xFF161616),
    onSurface          = Color(0xFF161616),
    onSurfaceVariant   = Color(0xFF626262),
    outline            = Color(0xFFD1D1D1),
    outlineVariant     = Color(0xFFD7D7D7)
)

@Composable
fun VoltHaltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PitchBlackColorScheme
        else      -> ProfessionalLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}
