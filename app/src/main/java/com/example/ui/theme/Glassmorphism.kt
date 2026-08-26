package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass & Glassmorphism Design System Helper.
 * Provides specular highlights, frosted refraction gradients,
 * and high-contrast ambient glass elevations for modern iOS/Android UI.
 */
object Glassmorphism {

    // Liquid Glass Gradient Borders with Specular Lighting
    fun specularBorder(
        isDark: Boolean,
        accentColor: Color = Color(0xFFEF4444),
        width: Dp = 1.2.dp
    ): BorderStroke {
        return BorderStroke(
            width = width,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(
                        Color(0x99FFFFFF), // Bright specular highlight on top-left edge
                        Color(0x20FFFFFF), // Soft translucent mid
                        Color(0x10000000), // Shadowed edge
                        accentColor.copy(alpha = 0.45f) // Refractive accent tint
                    )
                } else {
                    listOf(
                        Color(0xE6FFFFFF), // Specular reflection
                        Color(0x40FFFFFF),
                        Color(0x1A000000),
                        accentColor.copy(alpha = 0.35f)
                    )
                }
            )
        )
    }

    // Glass Background Fill Brush
    fun glassBackgroundBrush(
        isDark: Boolean,
        tintColor: Color = Color.Transparent
    ): Brush {
        return Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0x38334155),
                    Color(0x261E293B),
                    Color(0x1A0F172A)
                )
            } else {
                listOf(
                    Color(0xF5FFFFFF),
                    Color(0xE6F8FAFC),
                    Color(0xCCF1F5F9)
                )
            }
        )
    }
}

/**
 * Liquid Glass Surface Composable with specular highlights and refraction effect.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.let {
        (it.red * 0.299 + it.green * 0.587 + it.blue * 0.114) < 0.5
    }

    Surface(
        shape = shape,
        color = if (isDark) Color(0x331E293B) else Color(0xEBFFFFFF),
        shadowElevation = elevation,
        tonalElevation = 4.dp,
        border = Glassmorphism.specularBorder(isDark = isDark, accentColor = accentColor),
        modifier = modifier
            .clip(shape)
            .background(Glassmorphism.glassBackgroundBrush(isDark = isDark))
    ) {
        content()
    }
}
