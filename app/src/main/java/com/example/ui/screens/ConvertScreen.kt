package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.theme.SuccessGreen
import com.example.data.model.MarginOption
import com.example.data.model.OrientationOption
import com.example.data.model.PageSizeOption
import com.example.data.model.QualityOption
import com.example.data.model.ScaleTypeOption
import com.example.data.model.SelectedImageItem
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.viewmodel.ConversionUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    state: ConversionUiState,
    onAddMoreClick: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onMoveImage: (from: Int, to: Int) -> Unit,
    onRotateImage: (String) -> Unit,
    onToggleScale: (String) -> Unit,
    onClearAll: () -> Unit,
    onUpdateFileName: (String) -> Unit,
    onUpdatePageSize: (PageSizeOption) -> Unit,
    onUpdateOrientation: (OrientationOption) -> Unit,
    onUpdateQuality: (QualityOption) -> Unit,
    onUpdateMargin: (MarginOption) -> Unit,
    onToggleStoreInMongo: (Boolean) -> Unit = {},
    onLoadDemoPhotos: () -> Unit = {},
    onConvertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsAccordion by remember { mutableStateOf(true) }
    var previewPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // Fullscreen Photo Inspector Preview Dialog
    previewPhotoIndex?.let { index ->
        PhotoPreviewDialog(
            initialIndex = index,
            images = state.selectedImages,
            onDismiss = { previewPhotoIndex = null },
            onRotateImage = onRotateImage,
            onToggleScale = onToggleScale
        )
    }

    if (state.selectedImages.isEmpty()) {
        // Empty State
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("convert_screen_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "No Images Selected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Import pictures from your device or load demo photos to arrange, customize, and convert into a PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAddMoreClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .fillMaxWidth(0.82f)
                        .testTag("empty_select_images_button")
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Images", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onLoadDemoPhotos,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(0.82f)
                        .testTag("load_demo_photos_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Demo Photos (4 Samples)")
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("convert_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar with count and actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${state.selectedImages.size} ${if (state.selectedImages.size == 1) "Photo" else "Photos"} Selected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (state.config.orientation == OrientationOption.AUTO) "Auto Aspect" else state.config.orientation.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tap any photo for full preview & details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        OutlinedButton(
                            onClick = onAddMoreClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_more_images_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        TextButton(
                            onClick = onClearAll,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("clear_all_images_button")
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            // Quick Horizontal Photo Preview Strip
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PHOTO PREVIEW GALLERY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap to expand",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(state.selectedImages, key = { _, item -> "strip_${item.id}" }) { idx, item ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 84.dp, height = 94.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { previewPhotoIndex = idx }
                                        .testTag("strip_thumbnail_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Coil Low Memory Request
                                    val thumbRequest = remember(item.uriString) {
                                        ImageRequest.Builder(context)
                                            .data(Uri.parse(item.uriString))
                                            .size(160, 160)
                                            .precision(Precision.INEXACT)
                                            .crossfade(true)
                                            .build()
                                    }

                                    AsyncImage(
                                        model = thumbRequest,
                                        contentDescription = "Preview $idx",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .rotate(item.rotationDegrees.toFloat()),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Badge #
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(bottomEnd = 6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }

                                    // Aspect Ratio tag bottom
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = item.aspectRatioLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PDF Output Settings Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSettingsAccordion = !showSettingsAccordion }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "PDF Output Settings",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${state.config.pageSize.label} • ${state.config.orientation.label} • ${state.config.quality.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (showSettingsAccordion) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showSettingsAccordion) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showSettingsAccordion) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                // PDF File Name
                                OutlinedTextField(
                                    value = state.config.fileName,
                                    onValueChange = onUpdateFileName,
                                    label = { Text("PDF File Name") },
                                    suffix = { Text(".pdf") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pdf_name_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Convert Button directly after the PDF name change
                                Button(
                                    onClick = onConvertClick,
                                    enabled = !state.isConverting && state.selectedImages.isNotEmpty(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("convert_to_pdf_action_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Convert to PDF (${state.selectedImages.size} ${if (state.selectedImages.size == 1) "Page" else "Pages"})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Orientation (Auto vs Fixed)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Page Orientation",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Auto Detect adjusts page layout dynamically to each photo's real aspect ratio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OrientationOption.entries.forEach { orient ->
                                        FilterChip(
                                            selected = state.config.orientation == orient,
                                            onClick = { onUpdateOrientation(orient) },
                                            label = { Text(orient.label) },
                                            modifier = Modifier.weight(1f).testTag("chip_orientation_${orient.name}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Page Size
                                Text(
                                    text = "Page Size",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    PageSizeOption.entries.forEach { option ->
                                        FilterChip(
                                            selected = state.config.pageSize == option,
                                            onClick = { onUpdatePageSize(option) },
                                            label = { Text(option.label) },
                                            modifier = Modifier.testTag("chip_page_size_${option.name}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Quality / Compression
                                Text(
                                    text = "Image Quality (Memory Optimized)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    QualityOption.entries.forEach { qual ->
                                        FilterChip(
                                            selected = state.config.quality == qual,
                                            onClick = { onUpdateQuality(qual) },
                                            label = { Text(qual.label) },
                                            modifier = Modifier.testTag("chip_quality_${qual.name}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Margins
                                Text(
                                    text = "Page Margins",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MarginOption.entries.forEach { margin ->
                                        FilterChip(
                                            selected = state.config.margin == margin,
                                            onClick = { onUpdateMargin(margin) },
                                            label = { Text(margin.label) },
                                            modifier = Modifier.testTag("chip_margin_${margin.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Image Items List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page Order & Fine-Tuning (${state.selectedImages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Individual Image Page Items
            itemsIndexed(state.selectedImages, key = { _, item -> item.id }) { index, item ->
                ImagePageCard(
                    index = index,
                    totalCount = state.selectedImages.size,
                    item = item,
                    onPreview = { previewPhotoIndex = index },
                    onMoveUp = { onMoveImage(index, index - 1) },
                    onMoveDown = { onMoveImage(index, index + 1) },
                    onRotate = { onRotateImage(item.id) },
                    onToggleScale = { onToggleScale(item.id) },
                    onRemove = { onRemoveImage(item.id) }
                )
            }
        }

        // Conversion Progress Dialog
        if (state.isConverting) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Generating PDF...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Processing page ${state.currentStep} of ${state.totalSteps}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val progress = if (state.totalSteps > 0) state.currentStep.toFloat() / state.totalSteps.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePageCard(
    index: Int,
    totalCount: Int,
    item: SelectedImageItem,
    onPreview: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onToggleScale: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("image_page_card_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Number Badge + Clickable Thumbnail for instant preview
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPreview() }
                    .testTag("page_card_thumb_$index")
            ) {
                // Downsampled thumbnail request for zero lag
                val imageRequest = remember(item.uriString) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(item.uriString))
                        .size(200, 200)
                        .precision(Precision.INEXACT)
                        .crossfade(true)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(item.rotationDegrees.toFloat()),
                    contentScale = ContentScale.Crop
                )

                // Page Tag in top corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(bottomEnd = 6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Eye Preview Icon in bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(topStart = 6.dp)
                        )
                        .padding(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview Photo",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details & Controls
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${index + 1} of $totalCount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.aspectRatioLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = "${item.dimensionString} • ${item.scaleType.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Move Up
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp).testTag("move_up_$index")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Move earlier",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Move Down
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp).testTag("move_down_$index")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Move later",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Rotate 90°
                    IconButton(
                        onClick = onRotate,
                        modifier = Modifier.size(32.dp).testTag("rotate_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90 degrees",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Scale Type Toggle
                    IconButton(
                        onClick = onToggleScale,
                        modifier = Modifier.size(32.dp).testTag("scale_type_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Change fit mode",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Preview Button
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier.size(32.dp).testTag("preview_btn_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View Photo",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Remove Image
                    IconButton(
                        onClick = onRemove,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.size(32.dp).testTag("remove_image_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove page",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
