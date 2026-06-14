package com.toffice.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BlueLight,
    secondary = TealSecondary,
    tertiary = AmberAccent,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    primaryContainer = BluePrimaryDark,
    secondary = TealSecondary,
    tertiary = AmberAccent,
    background = SurfaceDark,
)

@Composable
fun TOfficeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
