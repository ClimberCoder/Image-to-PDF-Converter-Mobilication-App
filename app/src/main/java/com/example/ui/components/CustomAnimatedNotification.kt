package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CustomAnimatedSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Scale and drop animation on appear (Dynamic Island Style)
    val enterAnim = remember { Animatable(0f) }
    
    LaunchedEffect(snackbarData) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.65f, // Bouncy!
                stiffness = 250f
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // Memory optimization: All animations happen in graphicsLayer to avoid layout recomposition
            .graphicsLayer {
                // Mimic dynamic island expansion from the top center
                transformOrigin = TransformOrigin(0.5f, 0f) 
                
                translationY = offsetY - (100f * (1f - enterAnim.value))
                scaleX = 0.3f + (0.7f * enterAnim.value)
                scaleY = 0.3f + (0.7f * enterAnim.value)
                
                // Fade out when swiping UP
                alpha = enterAnim.value * (1f - (-offsetY / 300f).coerceIn(0f, 1f))
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        // Swipe up to dismiss
                        if (offsetY < -100f) {
                            coroutineScope.launch {
                                snackbarData.dismiss()
                            }
                        } else {
                            coroutineScope.launch {
                                // Reset position with a bounce
                                Animatable(offsetY, Float.VectorConverter).animateTo(
                                    targetValue = 0f, 
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                ) {
                                    offsetY = value
                                }
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow dragging UP
                        if (offsetY + dragAmount < 20f) {
                            offsetY += dragAmount
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Dynamic Island / Pill style background
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(32.dp),
            color = Color.Black, // True black for island look
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulse animation for the icon
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_icon")
                val iconScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "icon_scale"
                )
                
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_alpha"
                )

                // Glowing Icon Box
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                alpha = glowAlpha
                            }
                            .background(Color(0xFF4ADE80).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    )
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notification",
                        tint = Color(0xFF4ADE80), // Neon green accent for the dark island
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "System Notice",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (snackbarData.visuals.actionLabel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4ADE80))
                    ) {
                        Text(
                            text = snackbarData.visuals.actionLabel!!,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
