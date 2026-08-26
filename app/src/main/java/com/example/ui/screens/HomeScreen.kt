package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PdfDocumentEntity
import com.example.ui.navigation.AppDestination
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Premium Dark Black UI Home Screen featuring layered card stacks, swipe interactions,
 * and elegant photo-to-pdf conversion workflow.
 */
@Composable
fun HomeScreen(
    recentPdfs: List<PdfDocumentEntity>,
    totalCount: Int,
    onSelectImagesClick: () -> Unit,
    onLoadDemoPhotos: () -> Unit = {},
    onOpenPdf: (PdfDocumentEntity) -> Unit,
    onSharePdf: (PdfDocumentEntity) -> Unit,
    onPreviewPdf: (PdfDocumentEntity) -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var activeCardIndex by remember { mutableIntStateOf(0) }
    val offsetX = remember { Animatable(0f) }

    val cardsData = listOf(
        CardInfo(
            title = "Photo → PDF",
            subtitle = "Transform your gallery into pristine PDF documents instantly",
            badge = "Primary Tool",
            icon = Icons.Default.PictureAsPdf
        ),
        CardInfo(
            title = "Batch Converter",
            subtitle = "Merge multiple images into a single professional PDF portfolio",
            badge = "Pro Feature",
            icon = Icons.Default.AddPhotoAlternate
        ),
        CardInfo(
            title = "Secure Archive",
            subtitle = "On-device encrypted PDF storage with high-speed rendering",
            badge = "Offline Mode",
            icon = Icons.Default.Description
        )
    )

    val currentCard = cardsData[activeCardIndex % cardsData.size]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .testTag("home_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Minimal Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PDF Studio",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            fontSize = 28.sp
                        ),
                        color = Color(0xFFF4F4F5)
                    )
                    Text(
                        text = "Convert photos to high-end PDFs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )
                }

                // Sample Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                    modifier = Modifier.clickable { onLoadDemoPhotos() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sample",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFF4F4F5)
                        )
                    }
                }
            }
        }

        // Main Photo-to-PDF Layered Stack Card with Swipe Gestures
        item {
            val infiniteTransition = rememberInfiniteTransition(label = "anim_stack")
            val pulseAnim by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value < -100f) {
                                    // Swipe Left: Next card in stack (Tinder style)
                                    scope.launch {
                                        offsetX.animateTo(-400f, spring(stiffness = 300f))
                                        activeCardIndex++
                                        offsetX.snapTo(400f)
                                        offsetX.animateTo(0f, spring(stiffness = 300f))
                                    }
                                } else if (offsetX.value > 100f) {
                                    // Swipe Right: Navigate to Files tab
                                    scope.launch {
                                        offsetX.animateTo(400f, spring(stiffness = 300f))
                                        onNavigateTo(AppDestination.FILES)
                                    }
                                } else {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(stiffness = 400f))
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount)
                                }
                            }
                        )
                    }
                    .testTag("hero_convert_card"),
                contentAlignment = Alignment.Center
            ) {
                // Background Card 2 (Offset depth)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF121216),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(340.dp)
                        .offset(y = 20.dp)
                        .scale(0.92f),
                    border = BorderStroke(1.dp, Color(0x15FFFFFF))
                ) {}

                // Background Card 1 (Offset depth)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF16161A),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(360.dp)
                        .offset(y = 10.dp)
                        .scale(0.96f),
                    border = BorderStroke(1.dp, Color(0x20FFFFFF))
                ) {}

                // Foreground Main Card
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF18181C),
                    shadowElevation = 24.dp,
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFF3B30).copy(alpha = 0.6f), Color(0x40FFFFFF), Color(0xFFFF3B30).copy(alpha = 0.3f))
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .offset { IntOffset(offsetX.value.toInt(), 0) }
                        .rotate(offsetX.value / 35f)
                        .clickable { onSelectImagesClick() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x35FF3B30), Color(0x0818181C))
                                )
                            )
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Badge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = currentCard.icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer(scaleX = pulseAnim, scaleY = pulseAnim)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentCard.badge,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    ),
                                    color = Color(0xFFFF3B30)
                                )
                            }

                            Text(
                                text = "Swipe Left / Right",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                        }

                        // Center Visual Animation Mock / Icon Stack
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222228)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer(scaleX = pulseAnim, scaleY = pulseAnim)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF3B30), Color(0xFFFF6B6B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Titles & Minimal Supporting Text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentCard.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = Color(0xFFF4F4F5)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = currentCard.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA1A1AA),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Clear CTA Button
                        Button(
                            onClick = onSelectImagesClick,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF3B30), Color(0xFFEF4444), Color(0xFFFF6B6B))
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Convert Now",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Recent PDFs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent PDFs",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFFF4F4F5)
                )

                if (recentPdfs.isNotEmpty()) {
                    TextButton(
                        onClick = { onNavigateTo(AppDestination.FILES) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "See All ($totalCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
            }
        }

        if (recentPdfs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF121216),
                    border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA).copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No PDFs created yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFF4F4F5)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Convert photos above to generate your first document",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Horizontal scrollable recent PDFs for sleekness
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(recentPdfs, key = { it.id }) { pdf ->
                        RecentPdfCard(
                            pdf = pdf,
                            onOpen = { onOpenPdf(pdf) },
                            onPreview = { onPreviewPdf(pdf) },
                            onShare = { onSharePdf(pdf) }
                        )
                    }
                }
            }
        }
    }
}

data class CardInfo(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun RecentPdfCard(
    pdf: PdfDocumentEntity,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onShare: () -> Unit
) {
    val formattedSize = remember(pdf.fileSizeBytes) {
        val kb = pdf.fileSizeBytes / 1024.0
        if (kb >= 1024.0) {
            String.format(Locale.US, "%.1f MB", kb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141418),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0x20FFFFFF)),
        modifier = Modifier
            .width(180.dp)
            .height(210.dp)
            .clickable { onPreview() }
            .testTag("recent_pdf_card_${pdf.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Thumbnail / Icon Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                if (pdf.thumbnailPath != null && File(pdf.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(pdf.thumbnailPath),
                        contentDescription = "PDF Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Filename and Info
            Column {
                Text(
                    text = pdf.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color(0xFFF4F4F5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${pdf.pageCount} pages • $formattedSize",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1AA)
                )
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
