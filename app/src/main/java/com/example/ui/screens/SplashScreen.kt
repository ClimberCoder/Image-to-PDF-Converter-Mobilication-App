package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 0: Welcome, 1: to, 2: a redefined document experience
    var onboardingStep by remember { mutableIntStateOf(0) }
    
    // Smooth timing for the splash text sequence
    LaunchedEffect(Unit) {
        delay(1800)
        onboardingStep = 1 // "to"
        delay(1600)
        onboardingStep = 2 // "a redefined document experience"
        delay(3200)
        onSplashFinished()
    }

    // Live continuous morphing ambient gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_mesh_gradient")
    val bgTopColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF000000),
        targetValue = Color(0xFF030303),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_top"
    )
    val bgBottomColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF000000),
        targetValue = Color(0xFF050505),
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_bottom"
    )

    // Floating Ambient Light Orbs with calm fluid motion
    val orbX1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_x1"
    )
    val orbY1 by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_y1"
    )
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(bgTopColor, bgBottomColor))),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Liquid Glow Layers
        Box(
            modifier = Modifier
                .offset(x = orbX1.dp, y = orbY1.dp)
                .size(380.dp)
                .scale(orbScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x303B82F6),
                            Color(0x109333EA),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = (-orbX1 * 1.3f).dp, y = (orbY1 * 1.6f).dp)
                .size(320.dp)
                .scale(orbScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x20EC4899),
                            Color(0x103B82F6),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center Dynamic Content: Cinematic Sequence
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = onboardingStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1200, easing = LinearOutSlowInEasing)) togetherWith 
                    fadeOut(animationSpec = tween(1000, easing = FastOutLinearInEasing))
                },
                label = "cinematic_step_transition"
            ) { step ->
                
                // Add blur effect per step transitioning in
                val blurAnimation by transition.animateDp(
                    transitionSpec = { tween(1200, easing = FastOutSlowInEasing) },
                    label = "text_blur"
                ) { state ->
                    if (state == androidx.compose.animation.EnterExitState.Visible) 0.dp else 16.dp
                }
                
                val scaleAnimation by transition.animateFloat(
                    transitionSpec = { tween(1400, easing = FastOutSlowInEasing) },
                    label = "text_scale"
                ) { state ->
                    if (state == androidx.compose.animation.EnterExitState.Visible) 1f else 0.95f
                }

                Box(
                    modifier = Modifier
                        .blur(blurAnimation)
                        .scale(scaleAnimation),
                    contentAlignment = Alignment.Center
                ) {
                    when (step) {
                        0 -> {
                            Text(
                                text = "Welcome",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-1).sp,
                                    fontSize = 54.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        1 -> {
                            Text(
                                text = "to",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraLight,
                                    letterSpacing = (-0.5).sp,
                                    fontSize = 42.sp
                                ),
                                color = Color(0xDDFFFFFF),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "a redefined\ndocument\nexperience",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.5).sp,
                                    fontSize = 38.sp,
                                    lineHeight = 44.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
