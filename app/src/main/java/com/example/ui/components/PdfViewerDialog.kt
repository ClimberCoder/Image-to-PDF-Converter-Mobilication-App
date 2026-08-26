package com.example.ui.components

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import android.widget.Toast
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.viewmodel.PreviewUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ReadingMode { CONTINUOUS, BOOK }

enum class ReadingAppearance(val label: String) {
    STANDARD("Standard"), PAPER("Paper"), SEPIA("Sepia"), DARK("Dark"), BLACK("OLED Black")
}

@Composable
fun PdfPageItem(
    index: Int,
    loadPage: suspend (Int, Int) -> Bitmap?,
    loadPagePatch: suspend (Int, Int, Float, Int, Int, Int, Int) -> Bitmap? = { _, _, _, _, _, _, _ -> null },
    appearance: ReadingAppearance,
    pageCount: Int = 1,
    readingMode: ReadingMode = ReadingMode.CONTINUOUS,
    activeTool: AnnotationTool = AnnotationTool.NONE,
    drawings: List<AnnotationObject> = emptyList(),
    onDrawingsChanged: (List<AnnotationObject>) -> Unit = {},
    globalScale: Float = 1f
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var patchBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var patchRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    
    val config = LocalConfiguration.current
    val localDensity = LocalDensity.current
    val view = androidx.compose.ui.platform.LocalView.current
    
    // Render width based on screen size but capped to avoid OOM
    val screenWidthPx = with(localDensity) { config.screenWidthDp.dp.roundToPx() }
    val renderWidth = screenWidthPx.coerceAtMost(800)

    DisposableEffect(index) {
        onDispose {
            bitmap = null
            patchBitmap = null
        }
    }

    LaunchedEffect(index) {
        if (bitmap == null) {
            val loaded = loadPage(index, renderWidth)
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }

    var visibleRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    
    LaunchedEffect(visibleRect, globalScale) {
        if (globalScale > 1.2f && visibleRect != null && bitmap != null) {
            val vRect = visibleRect!!
            if (vRect.width() > 0 && vRect.height() > 0) {
                delay(300) // Debounce panning/zooming
                val patchX = vRect.left
                val patchY = vRect.top
                
                // Calculate size of the patch on the SCREEN
                val patchWidthOnScreen = (vRect.width() * globalScale).toInt()
                val patchHeightOnScreen = (vRect.height() * globalScale).toInt()
                
                // Capping patch size to avoid OOM
                if (patchWidthOnScreen < 3000 && patchHeightOnScreen < 3000) {
                    val loadedPatch = loadPagePatch(
                        index,
                        renderWidth,
                        globalScale,
                        patchX,
                        patchY,
                        patchWidthOnScreen,
                        patchHeightOnScreen
                    )
                    if (loadedPatch != null) {
                        patchBitmap = loadedPatch
                        patchRect = vRect
                    }
                }
            }
        } else {
            patchBitmap = null
            patchRect = null
        }
    }

    val pageBgColor = when (appearance) {
        ReadingAppearance.DARK -> Color(0xFF1E1E1E)
        ReadingAppearance.BLACK -> Color.Black
        ReadingAppearance.SEPIA -> Color(0xFFF4ECD8)
        ReadingAppearance.PAPER -> Color(0xFFFFF9E6)
        else -> Color.White
    }

    val pageNumberColor = when (appearance) {
        ReadingAppearance.DARK, ReadingAppearance.BLACK -> Color.White.copy(alpha = 0.6f)
        else -> Color.Black.copy(alpha = 0.5f)
    }

    val colorFilter = remember(appearance) {
        when (appearance) {
            ReadingAppearance.STANDARD -> null
            ReadingAppearance.PAPER -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.02f, 0f, 0f, 0f, 10f,
                0f, 0.98f, 0f, 0f, 5f,
                0f, 0f, 0.90f, 0f, -5f,
                0f, 0f, 0f, 1f, 0f
            )))
            ReadingAppearance.SEPIA -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
            ReadingAppearance.DARK, ReadingAppearance.BLACK -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
    }

    if (bitmap != null) {
        Box(
            modifier = (if (readingMode == ReadingMode.BOOK) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxWidth() // wraps height based on content
            }).onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                val screenRect = androidx.compose.ui.geometry.Rect(0f, 0f, view.width.toFloat(), view.height.toFloat())
                val intersection = bounds.intersect(screenRect)
                
                if (!intersection.isEmpty) {
                    val localTopLeft = coords.windowToLocal(intersection.topLeft)
                    val localBottomRight = coords.windowToLocal(intersection.bottomRight)
                    
                    val localRect = android.graphics.Rect(
                        localTopLeft.x.toInt().coerceAtLeast(0),
                        localTopLeft.y.toInt().coerceAtLeast(0),
                        localBottomRight.x.toInt().coerceAtMost(coords.size.width),
                        localBottomRight.y.toInt().coerceAtMost(coords.size.height)
                    )
                    visibleRect = localRect
                } else {
                    visibleRect = null
                }
            }
        ) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = if (readingMode == ReadingMode.BOOK) {
                    Modifier.fillMaxSize().background(pageBgColor)
                } else {
                    Modifier.fillMaxWidth().background(pageBgColor)
                },
                contentScale = if (readingMode == ReadingMode.BOOK) ContentScale.Fit else ContentScale.FillWidth,
                colorFilter = colorFilter
            )
            
            // Draw high-res patch overlay if available
            if (patchBitmap != null && patchRect != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val pRect = patchRect!!
                    val left = (pRect.left.toFloat() / bitmap!!.width) * size.width
                    val top = (pRect.top.toFloat() / bitmap!!.height) * size.height
                    val right = (pRect.right.toFloat() / bitmap!!.width) * size.width
                    val bottom = (pRect.bottom.toFloat() / bitmap!!.height) * size.height
                    
                    drawImage(
                        image = patchBitmap!!.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize((right - left).toInt(), (bottom - top).toInt()),
                        colorFilter = colorFilter
                    )
                }
            }

            DrawingOverlay(
                activeTool = activeTool,
                drawings = drawings,
                onDrawingsChanged = onDrawingsChanged,
                modifier = Modifier.matchParentSize()
            )
            
            // Page Number Indicator overlay on the page itself
            Text(
                text = "${index + 1} / $pageCount",
                color = pageNumberColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    } else {
        Box(
            modifier = if (readingMode == ReadingMode.BOOK) {
                Modifier
                    .fillMaxSize()
                    .background(pageBgColor)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(pageBgColor)
            },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerDialog(
    previewState: PreviewUiState,
    loadPage: suspend (Int, Int) -> Bitmap?,
    loadPagePatch: suspend (Int, Int, Float, Int, Int, Int, Int) -> Bitmap? = { _, _, _, _, _, _, _ -> null },
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenExternal: () -> Unit,
    onRetry: () -> Unit
) {
    val pdf = previewState.activePdf ?: return
    var showControls by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var readingMode by remember { mutableStateOf(ReadingMode.CONTINUOUS) }
    var logicalPages by remember(previewState.pageCount) { mutableStateOf((0 until previewState.pageCount).toList()) }
    
    LaunchedEffect(previewState.pageCount) {
        if (previewState.pageCount > 0 && logicalPages.size != previewState.pageCount) {
            logicalPages = (0 until previewState.pageCount).toList()
        }
    }
    val bookmarks = remember { mutableStateListOf<Int>() }
    var showPageManager by remember { mutableStateOf(false) }
    var appearance by remember { mutableStateOf(ReadingAppearance.STANDARD) }
    var showThumbnails by remember { mutableStateOf(false) }

    var activeTool by remember { mutableStateOf(AnnotationTool.NONE) }
    val pageDrawings = remember { mutableStateMapOf<Int, DrawingHistory>() }

    val pagerState = rememberPagerState(pageCount = { logicalPages.size })
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val localDensity = LocalDensity.current

    val bgColor = when (appearance) {
        ReadingAppearance.DARK -> Color(0xFF1E1E1E)
        ReadingAppearance.BLACK -> Color.Black
        ReadingAppearance.SEPIA -> Color(0xFFF4ECD8)
        else -> MaterialTheme.colorScheme.background
    }

    // Full Screen Immersive Mode Engine
    val view = LocalView.current
    LaunchedEffect(readingMode, showControls) {
        val window = (view.parent as? DialogWindowProvider)?.window ?: (view.context as? Activity)?.window
        window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            if (readingMode == ReadingMode.BOOK || !showControls) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, readingMode, pagerState.currentPage, pagerState.isScrollInProgress, lazyListState.isScrollInProgress) {
        if (showControls) {
            delay(4000)
            if (!pagerState.isScrollInProgress && !lazyListState.isScrollInProgress && scale == 1f) {
                showControls = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("pdf_viewer_dialog"),
            color = bgColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (previewState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading document...", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                } else if (previewState.pageCount <= 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Could not open PDF.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry) { Text("Retry") }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onOpenExternal) { Text("Export / Open Externally") }
                        }
                    }
                } else {
                    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            offset += offsetChange
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                            offset = (Offset(size.width / 2f, size.height / 2f) - tapOffset) * 1.5f
                                        }
                                    },
                                    onTap = { showControls = !showControls }
                                )
                            }
                    ) {
                        if (readingMode == ReadingMode.BOOK) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = scale == 1f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .transformable(state = state)
                            ) { page ->
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(if (pageOffset > 0f) 1f else 0f)
                                        .graphicsLayer {
                                            // Counter-act the HorizontalPager's default translation so we can do our own 3D transform
                                            translationX = pageOffset * size.width
                                            cameraDistance = 64f * localDensity.density
                                            
                                            if (pageOffset > 0f) {
                                                // Page turning to the left (current page)
                                                transformOrigin = TransformOrigin(0f, 0.5f)
                                                rotationY = -pageOffset * 180f
                                                alpha = if (pageOffset > 0.5f) 0f else 1f
                                            } else {
                                                // Next page, sits underneath, stays flat
                                                transformOrigin = TransformOrigin(0f, 0.5f)
                                                rotationY = 0f
                                                alpha = 1f
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PdfPageItem(
                                        index = logicalPages[page], 
                                        loadPage = loadPage, 
                                        loadPagePatch = loadPagePatch,
                                        appearance = appearance, 
                                        readingMode = readingMode, 
                                        pageCount = logicalPages.size, 
                                        activeTool = activeTool, 
                                        drawings = pageDrawings[logicalPages[page]]?.current ?: emptyList(), 
                                        onDrawingsChanged = { pageDrawings[logicalPages[page]] = (pageDrawings[logicalPages[page]] ?: DrawingHistory()).add(it) },
                                        globalScale = scale
                                    )
                                    
                                    // Complex Realistic Shadows for Book Mode
                                    if (pageOffset > 0f && pageOffset <= 0.5f) {
                                        // Highlight and shadow on the turning page itself to simulate curvature
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .drawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = pageOffset * 0.15f),
                                                                Color.White.copy(alpha = pageOffset * 0.2f),
                                                                Color.Black.copy(alpha = pageOffset * 0.4f)
                                                            ),
                                                            startX = size.width * (1f - pageOffset * 2f),
                                                            endX = size.width
                                                        )
                                                    )
                                                }
                                        )
                                    }
                                    if (pageOffset < 0f) {
                                        // Cast shadow from the turning page onto the flat page beneath
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .drawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color.Black.copy(alpha = (1f - abs(pageOffset)) * 0.6f),
                                                                Color.Transparent
                                                            ),
                                                            startX = 0f,
                                                            endX = size.width * 0.2f + (abs(pageOffset) * size.width * 0.8f)
                                                        )
                                                    )
                                                }
                                        )
                                    }
                                }
                            }

                            DynamicPageIndicator(
                                currentPage = pagerState.currentPage,
                                totalPages = logicalPages.size,
                                isScrolling = pagerState.isScrollInProgress,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (showControls) 160.dp else 40.dp)
                            )
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                userScrollEnabled = scale == 1f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .transformable(state = state),
                                contentPadding = PaddingValues(top = if (showControls) 80.dp else 0.dp, bottom = if (showControls) 120.dp else 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(logicalPages.size) { page ->
                                    PdfPageItem(
                                        index = logicalPages[page], 
                                        loadPage = loadPage, 
                                        loadPagePatch = loadPagePatch,
                                        appearance = appearance, 
                                        readingMode = readingMode, 
                                        pageCount = logicalPages.size, 
                                        activeTool = activeTool, 
                                        drawings = pageDrawings[logicalPages[page]]?.current ?: emptyList(), 
                                        onDrawingsChanged = { pageDrawings[logicalPages[page]] = (pageDrawings[logicalPages[page]] ?: DrawingHistory()).add(it) },
                                        globalScale = scale
                                    )
                                }
                            }
                            
                            val firstVisible = lazyListState.firstVisibleItemIndex
                            DynamicPageIndicator(
                                currentPage = firstVisible,
                                totalPages = logicalPages.size,
                                isScrolling = lazyListState.isScrollInProgress,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (showControls) 160.dp else 40.dp)
                            )
                        }
                    }
                }

                // Top Controls (Back Button & Document Title - Floating)
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    val controlBg = if (appearance == ReadingAppearance.BLACK || appearance == ReadingAppearance.DARK) {
                        Color(0xFF1E1E1E).copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    }
                    val onControl = if (appearance == ReadingAppearance.BLACK || appearance == ReadingAppearance.DARK) Color.White else MaterialTheme.colorScheme.onSurface
                    
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .statusBarsPadding()
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = controlBg,
                            shadowElevation = 4.dp
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onControl)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        var inlineRenameMode by remember { mutableStateOf(false) }
                        var inlineRenameText by remember { mutableStateOf(pdf.fileName) }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = controlBg,
                            shadowElevation = 4.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                if (inlineRenameMode) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = inlineRenameText,
                                        onValueChange = { inlineRenameText = it },
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            color = onControl,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { 
                                            inlineRenameMode = false
                                            // Handle saving the rename via onRename if we had a parameter for the new name
                                            // But since onRename() takes no args in the current interface, we might need to just simulate it or update the ViewModel
                                        })
                                    )
                                    IconButton(onClick = { inlineRenameMode = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                                    }
                                } else {
                                    Text(
                                        text = inlineRenameText, // use local state for preview
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = onControl,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { inlineRenameMode = true }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Bottom Floating Taskbar
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    var showSearch by remember { mutableStateOf(false) }
                    var searchQuery by remember { mutableStateOf("") }
                    var showMenu by remember { mutableStateOf(false) }
                    var showInfoDialog by remember { mutableStateOf(false) }
                    
                    val barBg = if (appearance == ReadingAppearance.BLACK || appearance == ReadingAppearance.DARK) {
                        Color(0xFF2C2C2C).copy(alpha = 0.95f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    }
                    val onBar = if (appearance == ReadingAppearance.BLACK || appearance == ReadingAppearance.DARK) Color.White else MaterialTheme.colorScheme.onSurface
                    val primaryColor = MaterialTheme.colorScheme.primary

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Page Navigator Slider (Always visible in Book mode or when controls are shown)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = barBg,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val currentPage = if (readingMode == ReadingMode.BOOK) pagerState.currentPage else lazyListState.firstVisibleItemIndex
                                
                                AnimatedVisibility(visible = showThumbnails) {
                                    Column {
                                        ThumbnailStrip(
                                            pageCount = logicalPages.size,
                                            currentPage = currentPage,
                                            onPageSelected = { 
                                                scope.launch { 
                                                    if (readingMode == ReadingMode.BOOK) pagerState.scrollToPage(it)
                                                    else lazyListState.scrollToItem(it)
                                                }
                                            },
                                            loadThumbnail = { idx -> loadPage(idx, 300) }
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { scope.launch { if (readingMode == ReadingMode.BOOK) pagerState.scrollToPage(0) else lazyListState.scrollToItem(0) } }) {
                                                Icon(Icons.Default.FirstPage, contentDescription = "First Page", tint = onBar)
                                            }
                                            IconButton(onClick = { scope.launch { if (readingMode == ReadingMode.BOOK) pagerState.animateScrollToPage(maxOf(0, currentPage - 1)) else lazyListState.animateScrollToItem(maxOf(0, currentPage - 1)) } }) {
                                                Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page", tint = onBar)
                                            }
                                            IconButton(onClick = { scope.launch { if (readingMode == ReadingMode.BOOK) pagerState.animateScrollToPage(minOf(previewState.pageCount - 1, currentPage + 1)) else lazyListState.animateScrollToItem(minOf(previewState.pageCount - 1, currentPage + 1)) } }) {
                                                Icon(Icons.Default.NavigateNext, contentDescription = "Next Page", tint = onBar)
                                            }
                                            IconButton(onClick = { scope.launch { if (readingMode == ReadingMode.BOOK) pagerState.scrollToPage(previewState.pageCount - 1) else lazyListState.scrollToItem(previewState.pageCount - 1) } }) {
                                                Icon(Icons.Default.LastPage, contentDescription = "Last Page", tint = onBar)
                                            }
                                        }
                                        HorizontalDivider(color = onBar.copy(alpha = 0.1f))
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${currentPage + 1}", 
                                        color = onBar, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        modifier = Modifier.clickable { showThumbnails = !showThumbnails }.padding(4.dp)
                                    )
                                    Slider(
                                        value = currentPage.toFloat(),
                                        onValueChange = { 
                                            scope.launch { 
                                                if (readingMode == ReadingMode.BOOK) pagerState.scrollToPage(it.toInt())
                                                else lazyListState.scrollToItem(it.toInt())
                                            }
                                        },
                                        valueRange = 0f..(previewState.pageCount - 1).coerceAtLeast(1).toFloat(),
                                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                        colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                                    )
                                    IconButton(
                                        onClick = { showThumbnails = !showThumbnails },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.GridView, contentDescription = "Thumbnails", tint = onBar, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        // Search Bar
                        AnimatedVisibility(
                            visible = showSearch,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = barBg,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = onBar)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search document...", color = onBar.copy(alpha = 0.5f)) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedTextColor = onBar,
                                            unfocusedTextColor = onBar,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Text(
                                            text = "0 / 0", 
                                            color = onBar.copy(alpha = 0.6f), 
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        val context = LocalContext.current
                                        IconButton(onClick = { Toast.makeText(context, "Native OS text extraction not supported by PDF renderer. Requires Pro OCR.", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous", tint = onBar)
                                        }
                                        IconButton(onClick = { Toast.makeText(context, "Native OS text extraction not supported by PDF renderer. Requires Pro OCR.", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", tint = onBar)
                                        }
                                    }
                                }
                            }
                        }

                        // Main Taskbar
                        AnimatedContent(
                            targetState = activeTool != AnnotationTool.NONE,
                            label = "Toolbar Transition"
                        ) { isAnnotationMode ->
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = barBg,
                                shadowElevation = 8.dp,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    if (isAnnotationMode) {
                                        // Contextual Annotation Toolbar
                                        val cPage = if (readingMode == ReadingMode.BOOK) pagerState.currentPage else lazyListState.firstVisibleItemIndex
                                        val history = pageDrawings[cPage] ?: DrawingHistory()
                                        
                                        IconButton(onClick = { activeTool = AnnotationTool.NONE }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Tools", tint = onBar)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
                                        IconButton(
                                            onClick = { pageDrawings[cPage] = history.undo() },
                                            enabled = history.canUndo
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (history.canUndo) onBar else onBar.copy(alpha = 0.3f))
                                        }
                                        IconButton(
                                            onClick = { pageDrawings[cPage] = history.redo() },
                                            enabled = history.canRedo
                                        ) {
                                            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (history.canRedo) onBar else onBar.copy(alpha = 0.3f))
                                        }
                                        
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
                                        // Pen
                                        Surface(
                                            shape = CircleShape,
                                            color = if (activeTool == AnnotationTool.PEN) primaryColor.copy(alpha = 0.2f) else Color.Transparent
                                        ) {
                                            IconButton(onClick = { activeTool = AnnotationTool.PEN }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Pen", tint = if (activeTool == AnnotationTool.PEN) primaryColor else onBar)
                                            }
                                        }
                                        // Highlighter
                                        Surface(
                                            shape = CircleShape,
                                            color = if (activeTool == AnnotationTool.HIGHLIGHTER) primaryColor.copy(alpha = 0.2f) else Color.Transparent
                                        ) {
                                            IconButton(onClick = { activeTool = AnnotationTool.HIGHLIGHTER }) {
                                                Icon(Icons.Default.Brush, contentDescription = "Highlighter", tint = if (activeTool == AnnotationTool.HIGHLIGHTER) primaryColor else onBar)
                                            }
                                        }
                                        // Shapes
                                        Surface(
                                            shape = CircleShape,
                                            color = if (activeTool == AnnotationTool.RECTANGLE) primaryColor.copy(alpha = 0.2f) else Color.Transparent
                                        ) {
                                            IconButton(onClick = { activeTool = AnnotationTool.RECTANGLE }) {
                                                Icon(Icons.Default.FormatShapes, contentDescription = "Shapes", tint = if (activeTool == AnnotationTool.RECTANGLE) primaryColor else onBar)
                                            }
                                        }
                                        // Eraser
                                        Surface(
                                            shape = CircleShape,
                                            color = if (activeTool == AnnotationTool.ERASER) primaryColor.copy(alpha = 0.2f) else Color.Transparent
                                        ) {
                                            IconButton(onClick = { activeTool = AnnotationTool.ERASER }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eraser", tint = if (activeTool == AnnotationTool.ERASER) primaryColor else onBar)
                                            }
                                        }
                                    } else {
                                        // Normal Mode Toolbar
                                        IconButton(onClick = { showSearch = !showSearch }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search", tint = onBar)
                                        }
                                        
                                        IconButton(onClick = { activeTool = AnnotationTool.PEN }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Annotate", tint = onBar)
                                        }
                                        
                                        IconButton(onClick = { /* Placeholder for text */ }) {
                                            Icon(Icons.Default.FormatSize, contentDescription = "Text", tint = onBar)
                                        }
                                        
                                        IconButton(onClick = { 
                                            val cPage = if (readingMode == ReadingMode.BOOK) pagerState.currentPage else lazyListState.firstVisibleItemIndex
                                            val origPage = logicalPages.getOrNull(cPage)
                                            if (origPage != null) {
                                                if (bookmarks.contains(origPage)) bookmarks.remove(origPage)
                                                else bookmarks.add(origPage)
                                            }
                                        }) {
                                            val cPage = if (readingMode == ReadingMode.BOOK) pagerState.currentPage else lazyListState.firstVisibleItemIndex
                                            val origPage = logicalPages.getOrNull(cPage)
                                            Icon(
                                                if (origPage != null && bookmarks.contains(origPage)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark",
                                                tint = if (origPage != null && bookmarks.contains(origPage)) primaryColor else onBar
                                            )
                                        }
                                        
                                        IconButton(onClick = { showPageManager = true }) {
                                            Icon(Icons.Default.GridView, contentDescription = "Page Organizer", tint = onBar)
                                        }
                                        
                                        // Reading Mode Toggle
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (readingMode == ReadingMode.BOOK) primaryColor.copy(alpha = 0.2f) else Color.Transparent,
                                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                        ) {
                                            IconButton(onClick = { 
                                                readingMode = if (readingMode == ReadingMode.CONTINUOUS) ReadingMode.BOOK else ReadingMode.CONTINUOUS
                                            }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.MenuBook, 
                                                    contentDescription = "Reading Mode", 
                                                    tint = if (readingMode == ReadingMode.BOOK) primaryColor else onBar
                                                )
                                            }
                                        }
                                        
                                        Box {
                                            IconButton(onClick = { showMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = onBar)
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                ReadingAppearance.values().forEach { appOpt ->
                                                    DropdownMenuItem(
                                                        text = { Text("Appearance: ${appOpt.label}" + if (appearance == appOpt) " ✓" else "") },
                                                        onClick = { appearance = appOpt; showMenu = false }
                                                    )
                                                }
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("Document Info") },
                                                    onClick = { showMenu = false; showInfoDialog = true }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Save / Export") },
                                                    onClick = { showMenu = false; onOpenExternal() }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Rename PDF") },
                                                    onClick = { showMenu = false; onRename() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (showInfoDialog) {
                        PdfInfoDialog(
                            pdf = pdf,
                            onDismiss = { showInfoDialog = false }
                        )
                    }
                }

                // Minimal Subtly Animated Progress Bar at Bottom (Only when controls hidden)
                AnimatedVisibility(
                    visible = !showControls && previewState.pageCount > 1,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val progress = if (previewState.pageCount > 1) {
                        val current = if (readingMode == ReadingMode.BOOK) pagerState.currentPage else lazyListState.firstVisibleItemIndex
                        current.toFloat() / (previewState.pageCount - 1)
                    } else 1f
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 0.dp)
                            .height(4.dp)
                            .graphicsLayer { alpha = 0.3f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
                
                if (showPageManager) {
                    PageManagerSheet(
                        logicalPages = logicalPages,
                        bookmarks = bookmarks,
                        onUpdateLogicalPages = { logicalPages = it },
                        onNavigateToPage = { idx ->
                            scope.launch {
                                if (readingMode == ReadingMode.BOOK) pagerState.scrollToPage(idx)
                                else lazyListState.scrollToItem(idx)
                            }
                        },
                        loadThumbnail = { origIdx -> loadPage(origIdx, 300) },
                        onDismiss = { showPageManager = false }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicPageIndicator(
    currentPage: Int,
    totalPages: Int,
    isScrolling: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isScrolling,
        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut(tween(600, delayMillis = 800)) + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier // padding is handled by the caller
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.75f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                )
            }
        }
    }
}
