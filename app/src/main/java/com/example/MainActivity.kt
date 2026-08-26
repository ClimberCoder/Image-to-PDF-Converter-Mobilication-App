package com.example
import com.example.data.model.*

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PdfDocumentEntity
import com.example.data.preferences.AppThemeMode
import com.example.ui.components.AppleFloatingTaskbar
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.PdfViewerDialog
import com.example.ui.components.RenamePdfDialog
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UsageScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: PdfViewModel = viewModel()

            var isSplashVisible by remember { mutableStateOf(true) }
            LaunchedEffect(intent) {
                if (intent.action == android.content.Intent.ACTION_VIEW || 
                    intent.action == android.content.Intent.ACTION_SEND || 
                    intent.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
                    viewModel.handleExternalIntent(intent, this@MainActivity)
                    isSplashVisible = false
                }
            }
            val userSettings by viewModel.settings.collectAsState()

            val isDark = when (userSettings.appTheme) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }


            MyApplicationTheme(darkTheme = isDark) {
                AnimatedContent(
                    targetState = isSplashVisible,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(800, easing = FastOutSlowInEasing)))
                            .togetherWith(fadeOut(animationSpec = tween(600)))
                    },
                    label = "splash_transition"
                ) { showSplash ->
                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = { isSplashVisible = false }
                        )
                    } else {
                        MainAppScaffold(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: PdfViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect state
    val conversionState by viewModel.conversionState.collectAsState()
    val previewState by viewModel.previewState.collectAsState()
    val filteredPdfs by viewModel.filteredPdfs.collectAsState()
    val recentPdfs by viewModel.recentPdfs.collectAsState()
    val totalPdfCount by viewModel.totalPdfCount.collectAsState()
    val totalPagesConverted by viewModel.totalPagesConverted.collectAsState()
    val totalBytesStored by viewModel.totalBytesStored.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val storageFilter by viewModel.storageFilter.collectAsState()
    val isScanningDevice by viewModel.isScanningDevice.collectAsState()
    val userSettings by viewModel.settings.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    // Navigation & Dialog State
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var pdfToRename by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfDocumentEntity?>(null) }

    // Request permissions on startup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshDevicePdfs() // Re-scan after permission result
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                viewModel.refreshDevicePdfs()
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Multi-Image Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImageUris(uris)
            currentDestination = AppDestination.HOME
        }
    }

    fun launchPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Snackbar effect
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (currentDestination) {
                                    AppDestination.HOME -> if (conversionState.selectedImages.isNotEmpty()) "Convert Studio" else "Img to PDF"
                                    AppDestination.FILES -> "Device PDFs"
                                    AppDestination.USAGE -> "Storage & Specs"
                                    AppDestination.PROFILE -> "Profile & Settings"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        if (currentDestination == AppDestination.HOME && conversionState.selectedImages.isNotEmpty()) {
                            IconButton(
                                onClick = { launchPhotoPicker() },
                                modifier = Modifier.testTag("top_bar_add_images")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add More Images",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(500, easing = FastOutSlowInEasing)))
                            .togetherWith(fadeOut(animationSpec = tween(300)))
                    },
                    label = "screen_crossfade"
                ) { destination ->
                    when (destination) {
                        AppDestination.HOME -> {
                            if (conversionState.selectedImages.isNotEmpty()) {
                                ConvertScreen(
                                    state = conversionState,
                                    onAddMoreClick = { launchPhotoPicker() },
                                    onRemoveImage = { viewModel.removeImage(it) },
                                    onMoveImage = { from, to -> viewModel.moveImage(from, to) },
                                    onRotateImage = { viewModel.rotateImage(it) },
                                    onToggleScale = { viewModel.toggleScaleType(it) },
                                    onClearAll = { viewModel.clearDraft() },
                                    onUpdateFileName = { viewModel.updateFileName(it) },
                                    onUpdatePageSize = { viewModel.updatePageSize(it) },
                                    onUpdateOrientation = { viewModel.updateOrientation(it) },
                                    onUpdateQuality = { viewModel.updateQuality(it) },
                                    onUpdateMargin = { viewModel.updateMargin(it) },
                                    onLoadDemoPhotos = { viewModel.loadDemoPhotos() },
                                    onConvertClick = { 
                                        viewModel.convertPdf {
                                            currentDestination = AppDestination.HOME
                                            viewModel.clearDraft()
                                        } 
                                    }
                                )
                            } else {
                                HomeScreen(
                                    recentPdfs = recentPdfs,
                                    totalCount = totalPdfCount,
                                    onSelectImagesClick = { launchPhotoPicker() },
                                    onLoadDemoPhotos = { viewModel.loadDemoPhotos() },
                                    onOpenPdf = { viewModel.openPdf(context, it) },
                                    onSharePdf = { viewModel.sharePdf(context, it) },
                                    onPreviewPdf = { viewModel.previewPdf(it) },
                                    onNavigateTo = { currentDestination = it }
                                )
                            }
                        }
                        AppDestination.FILES -> {
                            FilesScreen(
                                pdfs = filteredPdfs,
                                searchQuery = searchQuery,
                                sortOption = sortOption,
                                storageFilter = storageFilter,
                                isScanning = isScanningDevice,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onSortChange = { viewModel.setSortOption(it) },
                                onStorageFilterChange = { viewModel.setStorageFilter(it) },
                                onRefreshScan = { viewModel.refreshDevicePdfs() },
                                onOpenPdf = { viewModel.openPdf(context, it) },
                                onPreviewPdf = { viewModel.previewPdf(it) },
                                onSharePdf = { viewModel.sharePdf(context, it) },
                                onRenamePdf = { pdfToRename = it },
                                onDeletePdf = { pdfToDelete = it }
                            )
                        }
                        AppDestination.USAGE -> {
                            UsageScreen(
                                totalPdfsCreated = totalPdfCount,
                                totalImagesConverted = totalPagesConverted,
                                totalLocalFiles = filteredPdfs.size,
                                totalLocalBytes = totalBytesStored,
                                recentPdfs = recentPdfs
                            )
                        }
                        AppDestination.PROFILE -> {
                            ProfileScreen(
                                settings = userSettings, totalPdfsGenerated = totalPdfCount,
                                onUpdatePageSize = { viewModel.setDefaultPageSize(it) },
                                onUpdateOrientation = { viewModel.setDefaultOrientation(it) },
                                onUpdateQuality = { viewModel.setDefaultQuality(it) },
                                onUpdateMargin = { viewModel.setDefaultMargin(it) },
                                onUpdateTheme = { viewModel.setAppTheme(it) },
                                onClearCache = { viewModel.loadDemoPhotos() }
                            )
                        }
                    }
                }
            }
        }

        // Apple-style Floating Frosted Glass Taskbar
        // Top-Aligned Dynamic Island Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .statusBarsPadding()
        ) { data ->
            com.example.ui.components.CustomAnimatedSnackbar(data)
        }

        AppleFloatingTaskbar(
            currentDestination = currentDestination,
            onNavigate = { currentDestination = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Rename Dialog
    pdfToRename?.let { pdf ->
        RenamePdfDialog(
            pdf = pdf,
            onDismiss = { pdfToRename = null },
            onConfirm = { newName ->
                viewModel.renamePdf(pdf, newName)
                pdfToRename = null
            }
        )
    }

    // Delete Confirmation Dialog
    pdfToDelete?.let { pdf ->
        DeleteConfirmDialog(
            pdf = pdf,
            onDismiss = { pdfToDelete = null },
            onConfirm = {
                viewModel.deletePdf(pdf)
                pdfToDelete = null
            }
        )
    }

    // In-App PDF Preview Reader Dialog
    if (previewState.activePdf != null) {
        PdfViewerDialog(
            loadPage = { index, width -> viewModel.renderPreviewPage(index, width) },
            loadPagePatch = { index, baseW, s, pX, pY, pW, pH -> viewModel.renderPreviewPagePatch(index, baseW, s, pX, pY, pW, pH) },
            previewState = previewState,
            onDismiss = { viewModel.closePreview() },
            onShare = {
                previewState.activePdf?.let { viewModel.sharePdf(context, it) }
            },
            onRename = {
                previewState.activePdf?.let {
                    pdfToRename = it
                    viewModel.closePreview()
                }
            },
            onDelete = {
                previewState.activePdf?.let {
                    pdfToDelete = it
                    viewModel.closePreview()
                }
            },
            onOpenExternal = {
                previewState.activePdf?.let { viewModel.openPdf(context, it) }
            },
            onRetry = {
                previewState.activePdf?.let { viewModel.previewPdf(it) }
            }
        )
    if (conversionState.showSuccessDialog && conversionState.lastCreatedPdf != null) {
        com.example.ui.components.SuccessConversionBottomSheet(
            pdf = conversionState.lastCreatedPdf!!,
            onDismiss = { viewModel.dismissSuccessDialog() },
            onView = { viewModel.previewPdf(conversionState.lastCreatedPdf!!) },
            onShare = { viewModel.sharePdf(context, conversionState.lastCreatedPdf!!) },
            onOpenWith = { viewModel.openPdf(context, conversionState.lastCreatedPdf!!) }
        )
    }
    }
}
