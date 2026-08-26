package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NaturalDarkPrimary,
    onPrimary = NaturalDarkOnPrimary,
    primaryContainer = NaturalDarkPrimaryContainer,
    onPrimaryContainer = NaturalDarkOnPrimaryContainer,
    secondary = NaturalDarkSecondary,
    onSecondary = NaturalDarkOnSecondary,
    secondaryContainer = NaturalDarkSecondaryContainer,
    onSecondaryContainer = NaturalDarkOnSecondaryContainer,
    tertiary = NaturalDarkTertiary,
    onTertiary = NaturalDarkOnTertiary,
    tertiaryContainer = NaturalDarkTertiaryContainer,
    onTertiaryContainer = NaturalDarkOnTertiaryContainer,
    background = NaturalDarkBackground,
    onBackground = NaturalDarkOnBackground,
    surface = NaturalDarkSurface,
    onSurface = NaturalDarkOnSurface,
    surfaceVariant = NaturalDarkSurfaceVariant,
    onSurfaceVariant = NaturalDarkOnSurfaceVariant,
    outline = NaturalDarkOutline,
    outlineVariant = NaturalDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalLightPrimary,
    onPrimary = NaturalLightOnPrimary,
    primaryContainer = NaturalLightPrimaryContainer,
    onPrimaryContainer = NaturalLightOnPrimaryContainer,
    secondary = NaturalLightSecondary,
    onSecondary = NaturalLightOnSecondary,
    secondaryContainer = NaturalLightSecondaryContainer,
    onSecondaryContainer = NaturalLightOnSecondaryContainer,
    tertiary = NaturalLightTertiary,
    onTertiary = NaturalLightOnTertiary,
    tertiaryContainer = NaturalLightTertiaryContainer,
    onTertiaryContainer = NaturalLightOnTertiaryContainer,
    background = NaturalLightBackground,
    onBackground = NaturalLightOnBackground,
    surface = NaturalLightSurface,
    onSurface = NaturalLightOnSurface,
    surfaceVariant = NaturalLightSurfaceVariant,
    onSurfaceVariant = NaturalLightOnSurfaceVariant,
    outline = NaturalLightOutline,
    outlineVariant = NaturalLightOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted signature colors for distinct identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
