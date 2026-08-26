package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.AppDestination

/**
 * Ultra-sleek, fancy Apple & Telegram inspired floating taskbar navigation bar.
 * Features frosted glass styling, vibrant gradient borders, and smooth spring animations.
 */
@Composable
fun AppleFloatingTaskbar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.let {
        (it.red * 0.299 + it.green * 0.587 + it.blue * 0.114) < 0.5
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("apple_floating_taskbar"),
        contentAlignment = Alignment.Center
    ) {
        // Frosted Glass Capsule Bar with Rich Glow
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xF218181B) else Color(0xF6FFFFFF),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFFFF3B30).copy(alpha = 0.5f), Color(0x30FFFFFF), Color(0xFFFF3B30).copy(alpha = 0.2f))
                    } else {
                        listOf(Color(0xFFDC2626).copy(alpha = 0.4f), Color(0x80FFFFFF), Color(0xFFDC2626).copy(alpha = 0.2f))
                    }
                )
            ),
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDestination.bottomNavDestinations.forEach { destination ->
                    val isSelected = currentDestination == destination

                    Box(
                        modifier = Modifier
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        TaskbarFixedSlotItem(
                            destination = destination,
                            isSelected = isSelected,
                            isDark = isDark,
                            onClick = { onNavigate(destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskbarFixedSlotItem(
    destination: AppDestination,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val activeGradient = Brush.horizontalGradient(
        listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFFF6B6B))
    )
    val inactiveColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else inactiveColor,
        animationSpec = tween(180),
        label = "icon_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("nav_item_${destination.route}")
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier
                .height(44.dp)
                .background(
                    brush = if (isSelected) activeGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = if (isSelected) 14.dp else 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = destination.title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(100))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

