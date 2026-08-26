package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.pdf.PdfRendererHelper
import com.example.data.local.AppDatabase
import com.example.data.model.CloudPdfMetadata
import com.example.data.model.CloudStorageQuota
import com.example.data.model.MarginOption
import com.example.data.model.MongoConnectionConfig
import com.example.data.model.OrientationOption
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfConversionConfig
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.QualityOption
import com.example.data.model.ScaleTypeOption
import com.example.data.model.SelectedImageItem
import com.example.data.model.UploadDestination
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.SettingsManager
import com.example.data.preferences.UserSettings
import com.example.data.repository.PdfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)")
}

enum class StorageFilter(val label: String) {
    ALL("All Storage"),
    PHONE("Phone Storage"),
    MEMORY_CARD("Memory Card")
}

data class ConversionUiState(
    val selectedImages: List<SelectedImageItem> = emptyList(),
    val config: PdfConversionConfig = PdfConversionConfig(),
    val isConverting: Boolean = false,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val lastCreatedPdf: PdfDocumentEntity? = null,
    val showSuccessDialog: Boolean = false
)

data class PreviewUiState(
    val activePdf: PdfDocumentEntity? = null,
    val pageCount: Int = 0,
    val isLoading: Boolean = false,
    val currentPageIndex: Int = 0
)

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PdfRepository(application, db.pdfDao())
    private val settingsManager = SettingsManager(application)

    val settings: StateFlow<UserSettings> = settingsManager.settingsFlow

    val totalPdfCount: StateFlow<Int> = repository.totalPdfCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPagesConverted: StateFlow<Int> = repository.totalPagesConverted
        .combine(MutableStateFlow(0)) { total, _ -> total ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBytesStored: StateFlow<Long> = repository.totalBytesStored
        .combine(MutableStateFlow(0L)) { total, _ -> total ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val rawAllPdfs = repository.allPdfs
    private val _scannedDevicePdfs = MutableStateFlow<List<PdfDocumentEntity>>(emptyList())
    val scannedDevicePdfs = _scannedDevicePdfs.asStateFlow()

    private val _isScanningDevice = MutableStateFlow(false)
    val isScanningDevice = _isScanningDevice.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption = _sortOption.asStateFlow()

    private val _storageFilter = MutableStateFlow(StorageFilter.ALL)
    val storageFilter = _storageFilter.asStateFlow()

    val allCombinedPdfs: StateFlow<List<PdfDocumentEntity>> = combine(
        rawAllPdfs,
        _scannedDevicePdfs
    ) { dbPdfs, scannedPdfs ->
        val map = LinkedHashMap<String, PdfDocumentEntity>()
        // 1. First add database records (accurate page counts & metadata)
        dbPdfs.forEach { pdf ->
            val path = try { File(pdf.filePath).canonicalPath } catch (_: Exception) { pdf.filePath }
            map[path] = pdf
        }
        // 2. Add any scanned PDFs from phone or memory card not already in DB
        scannedPdfs.forEach { pdf ->
            val path = try { File(pdf.filePath).canonicalPath } catch (_: Exception) { pdf.filePath }
            if (!map.containsKey(path)) {
                map[path] = pdf
            }
        }
        map.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPdfs: StateFlow<List<PdfDocumentEntity>> = combine(
        allCombinedPdfs,
        _searchQuery,
        _sortOption,
        _storageFilter
    ) { pdfs, query, sort, filter ->
        var list = pdfs
        if (query.isNotBlank()) {
            list = list.filter { it.fileName.contains(query, ignoreCase = true) }
        }
        when (filter) {
            StorageFilter.ALL -> {}
            StorageFilter.PHONE -> list = list.filter { !it.isMemoryCard }
            StorageFilter.MEMORY_CARD -> list = list.filter { it.isMemoryCard }
        }
        when (sort) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAtTimestamp }
            SortOption.OLDEST -> list.sortedBy { it.createdAtTimestamp }
            SortOption.NAME_ASC -> list.sortedBy { it.fileName.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.fileName.lowercase() }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.fileSizeBytes }
            SortOption.SIZE_ASC -> list.sortedBy { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPdfs: StateFlow<List<PdfDocumentEntity>> = repository.getRecentPdfs(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStorageFilter(filter: StorageFilter) {
        _storageFilter.value = filter
    }

    fun refreshDevicePdfs() {
        viewModelScope.launch {
            _isScanningDevice.value = true
            try {
                val scanned = repository.scanDeviceAndSdCardPdfs()
                _scannedDevicePdfs.value = scanned
            } catch (_: Exception) {
            } finally {
                _isScanningDevice.value = false
            }
        }
    }

    // Conversion workflow state
    private val _conversionState = MutableStateFlow(ConversionUiState())
    val conversionState = _conversionState.asStateFlow()

    // Preview state
    private val _previewState = MutableStateFlow(PreviewUiState())
    val previewState = _previewState.asStateFlow()

    // Cloud & MongoDB state
    private val _cloudFiles = MutableStateFlow<List<CloudPdfMetadata>>(emptyList())
    val cloudFiles = _cloudFiles.asStateFlow()

    private val _cloudQuota = MutableStateFlow(CloudStorageQuota(usedBytes = 0))
    val cloudQuota = _cloudQuota.asStateFlow()

    private val _uploadProgressMap = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val uploadProgressMap = _uploadProgressMap.asStateFlow()

    private val _isConnectingMongo = MutableStateFlow(false)
    val isConnectingMongo = _isConnectingMongo.asStateFlow()

    private val _mongoDatabases = MutableStateFlow<List<com.example.data.model.MongoDatabaseInfo>>(emptyList())
    val mongoDatabases = _mongoDatabases.asStateFlow()

    private val _mongoCollections = MutableStateFlow<List<com.example.data.model.MongoCollectionInfo>>(emptyList())
    val mongoCollections = _mongoCollections.asStateFlow()

    private val _selectedDbFilter = MutableStateFlow<String?>(null)
    val selectedDbFilter = _selectedDbFilter.asStateFlow()

    private val _selectedColFilter = MutableStateFlow<String?>(null)
    val selectedColFilter = _selectedColFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()


    init {
        // Initialize config from user preferences
        val userSettings = settingsManager.settingsFlow.value
        val defaultName = generateDefaultFileName()
        _conversionState.update {
            it.copy(
                config = PdfConversionConfig(
                    fileName = defaultName,
                    pageSize = userSettings.defaultPageSize,
                    orientation = userSettings.defaultOrientation,
                    quality = userSettings.defaultQuality,
                    margin = userSettings.defaultMargin
                )
            )
        }
        refreshCloud()
        refreshDevicePdfs()
    }

    private fun generateDefaultFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return "PDF_$timeStamp"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- Image Selection & Arrangement ---

    fun addImageUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val appContext = getApplication<Application>().applicationContext
        val cacheDir = File(appContext.cacheDir, "draft_images").apply { if (!exists()) mkdirs() }

        val persistedUris = uris.map { originalUri ->
            try {
                if (originalUri.scheme == "file") {
                    originalUri.toString()
                } else {
                    val localFile = File(cacheDir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
                    val stream: InputStream? = appContext.contentResolver.openInputStream(originalUri)
                    if (stream != null) {
                        stream.use { input ->
                            FileOutputStream(localFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    if (localFile.exists() && localFile.length() > 0) {
                        Uri.fromFile(localFile).toString()
                    } else {
                        originalUri.toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                originalUri.toString()
            }
        }

        val currentList = _conversionState.value.selectedImages.toMutableList()
        val newItems = persistedUris.map { uriStr ->
            val uri = Uri.parse(uriStr)
            var imgWidth = 0
            var imgHeight = 0
            var fileSize = 0L

            try {
                if (uri.scheme == "file" && uri.path != null) {
                    val file = File(uri.path!!)
                    fileSize = file.length()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    imgWidth = bounds.outWidth
                    imgHeight = bounds.outHeight
                } else {
                    appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(stream, null, bounds)
                        imgWidth = bounds.outWidth
                        imgHeight = bounds.outHeight
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            SelectedImageItem(
                uriString = uriStr,
                width = imgWidth,
                height = imgHeight,
                fileSizeBytes = fileSize
            )
        }
        currentList.addAll(newItems)
        _conversionState.update {
            it.copy(
                selectedImages = currentList,
                config = it.config.copy(
                    fileName = if (it.config.fileName.isBlank()) generateDefaultFileName() else it.config.fileName
                )
            )
        }
    }

    fun loadDemoPhotos() {
        val appContext = getApplication<Application>().applicationContext
        val demoItems = com.example.data.util.DemoPhotoProvider.getOrGenerateDemoPhotos(appContext)
        addImageUris(demoItems.map { it.uri })
        showMessage("Loaded ${demoItems.size} sample demo photos (Landscape, Receipt, Notes, Certificate)")
    }

    fun removeImage(id: String) {
        _conversionState.update { state ->
            val updated = state.selectedImages.filterNot { it.id == id }
            state.copy(selectedImages = updated)
        }
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        val current = _conversionState.value.selectedImages.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            Collections.swap(current, fromIndex, toIndex)
            _conversionState.update { it.copy(selectedImages = current) }
        }
    }

    fun rotateImage(id: String) {
        _conversionState.update { state ->
            val updated = state.selectedImages.map { item ->
                if (item.id == id) {
                    val nextRot = (item.rotationDegrees + 90) % 360
                    item.copy(rotationDegrees = nextRot)
                } else {
                    item
                }
            }
            state.copy(selectedImages = updated)
        }
    }

    fun toggleScaleType(id: String) {
        _conversionState.update { state ->
            val updated = state.selectedImages.map { item ->
                if (item.id == id) {
                    val nextType = when (item.scaleType) {
                        ScaleTypeOption.FIT_PAGE -> ScaleTypeOption.FILL_PAGE
                        ScaleTypeOption.FILL_PAGE -> ScaleTypeOption.ORIGINAL
                        ScaleTypeOption.ORIGINAL -> ScaleTypeOption.FIT_PAGE
                    }
                    item.copy(scaleType = nextType)
                } else {
                    item
                }
            }
            state.copy(selectedImages = updated)
        }
    }

    fun clearDraft() {
        _conversionState.update {
            ConversionUiState(
                selectedImages = emptyList(),
                config = it.config.copy(fileName = generateDefaultFileName())
            )
        }
    }

    fun updateFileName(name: String) {
        _conversionState.update { it.copy(config = it.config.copy(fileName = name)) }
    }

    fun updatePageSize(pageSize: PageSizeOption) {
        _conversionState.update { it.copy(config = it.config.copy(pageSize = pageSize)) }
    }

    fun updateOrientation(orientation: OrientationOption) {
        _conversionState.update { it.copy(config = it.config.copy(orientation = orientation)) }
    }

    fun updateQuality(quality: QualityOption) {
        _conversionState.update { it.copy(config = it.config.copy(quality = quality)) }
    }

    fun updateMargin(margin: MarginOption) {
        _conversionState.update { it.copy(config = it.config.copy(margin = margin)) }
    }

    fun updateStoreInMongo(store: Boolean) {
        _conversionState.update { it.copy(config = it.config.copy(storeInMongo = store)) }
    }

    // --- Conversion Execution ---

    fun convertPdf(onSuccess: (PdfDocumentEntity) -> Unit = {}) {
        val state = _conversionState.value
        if (state.selectedImages.isEmpty()) {
            _snackbarMessage.value = "Please select at least one image."
            return
        }

        viewModelScope.launch {
            _conversionState.update {
                it.copy(
                    isConverting = true,
                    currentStep = 1,
                    totalSteps = state.selectedImages.size
                )
            }

            val result = repository.generateAndSavePdf(
                images = state.selectedImages,
                config = state.config,
                onProgress = { current, total ->
                    _conversionState.update {
                        it.copy(currentStep = current, totalSteps = total)
                    }
                }
            )

            if (result.isSuccess) {
                val createdPdf = result.getOrThrow()
                _conversionState.update {
                    it.copy(
                        isConverting = false,
                        lastCreatedPdf = createdPdf,
                        showSuccessDialog = true
                    )
                }
                _snackbarMessage.value = "PDF created successfully!"
                onSuccess(createdPdf)

                // Store in MongoDB or Cloud Vault
                if (state.config.storeInMongo || settings.value.autoCloudSync) {
                    uploadPdfToCloud(createdPdf)
                }
            } else {
                _conversionState.update { it.copy(isConverting = false) }
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to create PDF"
                _snackbarMessage.value = "Error: $err"
            }
        }
    }

    fun dismissSuccessDialog() {
        _conversionState.update { it.copy(showSuccessDialog = false) }
    }

    // --- File Actions: Open, Share, Rename, Delete ---

    fun openPdf(context: Context, pdf: PdfDocumentEntity) {
        try {
            val file = File(pdf.filePath)
            if (!file.exists()) {
                _snackbarMessage.value = "PDF file not found on device."
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
        } catch (e: Exception) {
            // If no PDF viewer app is installed, open inside built-in previewer
            previewPdf(pdf)
        }
    }

    fun sharePdf(context: Context, pdf: PdfDocumentEntity) {
        try {
            val file = File(pdf.filePath)
            if (!file.exists()) {
                _snackbarMessage.value = "PDF file not found on device."
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, pdf.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share PDF via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _snackbarMessage.value = "Could not share PDF: ${e.localizedMessage}"
        }
    }

    fun renamePdf(pdf: PdfDocumentEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val result = repository.renamePdf(pdf, newName)
            if (result.isSuccess) {
                _snackbarMessage.value = "PDF renamed to ${result.getOrThrow().fileName}"
            } else {
                _snackbarMessage.value = "Rename failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deletePdf(pdf: PdfDocumentEntity) {
        viewModelScope.launch {
            val result = repository.deletePdf(pdf)
            if (result.isSuccess) {
                _snackbarMessage.value = "${pdf.fileName} deleted."
                if (_previewState.value.activePdf?.id == pdf.id) {
                    closePreview()
                }
            } else {
                _snackbarMessage.value = "Delete failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    // --- In-App PDF Previewer ---

    private var activePdfRenderer: PdfRendererHelper? = null

    fun previewPdf(pdf: PdfDocumentEntity) {
        viewModelScope.launch {
            _previewState.value = PreviewUiState(
                activePdf = pdf,
                isLoading = true,
                currentPageIndex = 0
            )
            val file = File(pdf.filePath)
            activePdfRenderer?.close()
            activePdfRenderer = PdfRendererHelper(getApplication(), file)
            val success = activePdfRenderer!!.init()
            
            _previewState.value = PreviewUiState(
                activePdf = pdf,
                pageCount = if (success) activePdfRenderer!!.pageCount else 0,
                isLoading = false,
                currentPageIndex = 0
            )
        }
    }

    fun closePreview() {
        activePdfRenderer?.close()
        activePdfRenderer = null
        _previewState.value = PreviewUiState()
    }

    suspend fun renderPreviewPage(index: Int, width: Int = 900): Bitmap? {
        return activePdfRenderer?.renderPage(index, width)
    }

    suspend fun renderPreviewPagePatch(
        index: Int,
        baseWidth: Int,
        scale: Float,
        patchX: Int,
        patchY: Int,
        patchWidth: Int,
        patchHeight: Int
    ): Bitmap? {
        return activePdfRenderer?.renderPagePatch(index, baseWidth, scale, patchX, patchY, patchWidth, patchHeight)
    }

    // --- Search & Sort ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // --- Cloud & MongoDB Sync ---

    fun setUploadDestination(destination: UploadDestination) {
        settingsManager.updateUploadDestination(destination)
        refreshCloud()
    }

    fun connectMongoDb(
        uri: String,
        databaseName: String = "pdf_vault",
        collectionName: String = "stored_pdfs"
    ) {
        viewModelScope.launch {
            _isConnectingMongo.value = true
            val result = repository.testAndConnectMongoUri(uri, databaseName, collectionName)
            _isConnectingMongo.value = false

            if (result.isSuccess) {
                val config = result.getOrThrow()
                settingsManager.updateMongoConfig(config)
                _snackbarMessage.value = "Connected to MongoDB (${config.clusterHost})!"
                refreshCloud()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to connect to MongoDB URI"
                val failedConfig = settings.value.mongoConfig.copy(
                    uri = uri,
                    isConnected = false,
                    connectionError = err
                )
                settingsManager.updateMongoConfig(failedConfig)
                _snackbarMessage.value = "Connection error: $err"
            }
        }
    }

    fun disconnectMongoDb() {
        val current = settings.value.mongoConfig
        val disconnected = current.copy(
            isConnected = false,
            connectionError = null
        )
        settingsManager.updateMongoConfig(disconnected)
        _snackbarMessage.value = "Disconnected from MongoDB."
        refreshCloud()
    }

    fun uploadPdfToCloud(pdf: PdfDocumentEntity) {
        val userSettings = settings.value
        val userEmail = userSettings.userEmail
        val destination = userSettings.uploadDestination
        val mongoConfig = userSettings.mongoConfig

        if (destination == UploadDestination.MONGODB && !mongoConfig.isConnected) {
            _snackbarMessage.value = "Please connect to MongoDB with your URI before uploading."
            return
        }

        viewModelScope.launch {
            _uploadProgressMap.update { it + (pdf.id to 0.1f) }
            val result = repository.uploadPdfToCloud(
                pdf = pdf,
                userEmail = userEmail,
                destination = destination,
                mongoConfig = mongoConfig,
                onProgress = { prog ->
                    _uploadProgressMap.update { it + (pdf.id to prog) }
                }
            )

            _uploadProgressMap.update { it - pdf.id }

            if (result.isSuccess) {
                val syncedDoc = result.getOrThrow()
                val targetName = if (destination == UploadDestination.MONGODB) {
                    "MongoDB (${mongoConfig.databaseName}.${mongoConfig.collectionName})"
                } else {
                    "App Cloud Vault"
                }
                _snackbarMessage.value = "${pdf.fileName} saved to $targetName!"
                refreshCloud()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Upload failed"
                _snackbarMessage.value = "Upload failed: $err"
            }
        }
    }

    fun refreshCloud() {
        val userSettings = settings.value
        val userEmail = userSettings.userEmail
        val destination = userSettings.uploadDestination
        val mongoConfig = userSettings.mongoConfig

        viewModelScope.launch {
            val filesResult = repository.fetchCloudFiles(userEmail, destination, mongoConfig)
            if (filesResult.isSuccess) {
                _cloudFiles.value = filesResult.getOrThrow()
            }
            val quotaResult = repository.getCloudQuota(userEmail, destination)
            if (quotaResult.isSuccess) {
                _cloudQuota.value = quotaResult.getOrThrow()
            }
            loadMongoDatabases()
        }
    }

    fun loadMongoDatabases() {
        viewModelScope.launch {
            val res = repository.getMongoDatabases()
            if (res.isSuccess) {
                _mongoDatabases.value = res.getOrThrow()
                val currentDb = settings.value.mongoConfig.databaseName
                loadMongoCollections(currentDb)
            }
        }
    }

    fun loadMongoCollections(dbName: String) {
        viewModelScope.launch {
            val res = repository.getMongoCollections(dbName)
            if (res.isSuccess) {
                _mongoCollections.value = res.getOrThrow()
            }
        }
    }

    fun createMongoDatabase(dbName: String, initialCol: String = "stored_pdfs") {
        viewModelScope.launch {
            val cleanDb = dbName.trim()
            val cleanCol = initialCol.trim().ifBlank { "stored_pdfs" }
            if (cleanDb.isBlank()) {
                _snackbarMessage.value = "Database name cannot be blank"
                return@launch
            }
            val res = repository.createMongoDatabase(cleanDb, cleanCol)
            if (res.isSuccess) {
                val currentConfig = settings.value.mongoConfig
                val updatedConfig = currentConfig.copy(
                    databaseName = cleanDb,
                    collectionName = cleanCol,
                    availableDatabases = (currentConfig.availableDatabases + cleanDb).distinct()
                )
                settingsManager.updateMongoConfig(updatedConfig)
                _snackbarMessage.value = "Database '$cleanDb' with collection '$cleanCol' ready!"
                loadMongoDatabases()
                refreshCloud()
            }
        }
    }

    fun createMongoCollection(dbName: String, colName: String) {
        viewModelScope.launch {
            val cleanDb = dbName.trim().ifBlank { settings.value.mongoConfig.databaseName }
            val cleanCol = colName.trim()
            if (cleanCol.isBlank()) {
                _snackbarMessage.value = "Collection name cannot be blank"
                return@launch
            }
            val res = repository.createMongoCollection(cleanDb, cleanCol)
            if (res.isSuccess) {
                val currentConfig = settings.value.mongoConfig
                val updatedConfig = currentConfig.copy(
                    databaseName = cleanDb,
                    collectionName = cleanCol
                )
                settingsManager.updateMongoConfig(updatedConfig)
                _snackbarMessage.value = "Collection '$cleanCol' added to database '$cleanDb'!"
                loadMongoDatabases()
                loadMongoCollections(cleanDb)
                refreshCloud()
            }
        }
    }

    fun selectActiveMongoDatabaseAndCollection(dbName: String, colName: String) {
        val currentConfig = settings.value.mongoConfig
        val updatedConfig = currentConfig.copy(
            databaseName = dbName,
            collectionName = colName
        )
        settingsManager.updateMongoConfig(updatedConfig)
        _snackbarMessage.value = "Active MongoDB storage set to: $dbName.$colName"
        loadMongoCollections(dbName)
        refreshCloud()
    }

    fun setMongoDatabaseFilter(db: String?) {
        _selectedDbFilter.value = db
    }

    fun setMongoCollectionFilter(col: String?) {
        _selectedColFilter.value = col
    }


    fun deleteCloudFile(fileId: String) {
        val userSettings = settings.value
        val userEmail = userSettings.userEmail
        val destination = userSettings.uploadDestination
        viewModelScope.launch {
            val res = repository.deleteCloudFile(fileId, userEmail, destination)
            if (res.isSuccess) {
                _snackbarMessage.value = "Document removed from ${destination.label}."
                refreshCloud()
            }
        }
    }

    // --- Settings Updates ---

    fun setDefaultPageSize(size: PageSizeOption) {
        settingsManager.updateDefaultPageSize(size)
    }

    fun setDefaultOrientation(orientation: OrientationOption) {
        settingsManager.updateDefaultOrientation(orientation)
    }

    fun setDefaultQuality(quality: QualityOption) {
        settingsManager.updateDefaultQuality(quality)
    }

    fun setDefaultMargin(margin: MarginOption) {
        settingsManager.updateDefaultMargin(margin)
    }

    fun setAppTheme(theme: AppThemeMode) {
        settingsManager.updateAppTheme(theme)
    }

    fun setAutoCloudSync(enabled: Boolean) {
        settingsManager.updateAutoCloudSync(enabled)
    }

    fun setUserEmail(email: String) {
        settingsManager.updateUserEmail(email)
        refreshCloud()
    }

    fun handleExternalIntent(intent: Intent, context: Context) {
        viewModelScope.launch {
            try {
                if (intent.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("image/") == true) {
                    val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uris != null) {
                        addImageUris(uris)
                    }
                } else if (intent.action == Intent.ACTION_SEND) {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uri != null) {
                        if (intent.type?.startsWith("image/") == true) {
                            addImageUris(listOf(uri))
                        } else if (intent.type == "application/pdf") {
                            importAndPreviewPdf(uri, context)
                        }
                    }
                } else if (intent.action == Intent.ACTION_VIEW) {
                    val uri = intent.data
                    if (uri != null && (intent.type == "application/pdf" || intent.type == "application/octet-stream")) {
                        importAndPreviewPdf(uri, context)
                    }
                }
            } catch (e: Exception) {
                showMessage("Failed to open file: ${e.message}")
            }
        }
    }

    private suspend fun importAndPreviewPdf(uri: Uri, context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "imported_pdfs").apply { if (!exists()) mkdirs() }
            val fileName = getFileNameFromUri(uri, context) ?: "Imported_Document_${System.currentTimeMillis()}.pdf"
            val localFile = File(cacheDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (localFile.exists()) {
                val entity = PdfDocumentEntity(
                    fileName = fileName,
                    filePath = localFile.absolutePath,
                    fileSizeBytes = localFile.length(),
                    pageCount = 0 // Will be updated when rendered
                )
                // Add to DB
                val id = repository.insertPdf(entity)
                val savedEntity = entity.copy(id = id)
                previewPdf(savedEntity)
            }
        } catch (e: Exception) {
            showMessage("Error importing PDF: ${e.message}")
        }
    }

    private fun getFileNameFromUri(uri: Uri, context: Context): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path?.let { File(it).name }
        }
        return result
    }
}
