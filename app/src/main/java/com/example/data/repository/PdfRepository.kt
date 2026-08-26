package com.example.data.repository

import android.content.Context
import com.example.core.pdf.PdfGenerationResult
import com.example.core.pdf.PdfGenerator
import com.example.data.cloud.MongoPdfApiService
import com.example.data.cloud.SecureMongoCloudApiService
import com.example.data.local.PdfDao
import com.example.data.model.CloudPdfMetadata
import com.example.data.model.CloudStorageQuota
import com.example.data.model.PdfConversionConfig
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.SelectedImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PdfRepository(
    private val context: Context,
    private val pdfDao: PdfDao,
    private val cloudApiService: MongoPdfApiService = SecureMongoCloudApiService()
) {
    private val pdfGenerator = PdfGenerator(context)

    val allPdfs: Flow<List<PdfDocumentEntity>> = pdfDao.getAllPdfs()
    val totalPdfCount: Flow<Int> = pdfDao.getTotalPdfCount()
    val totalPagesConverted: Flow<Int?> = pdfDao.getTotalPagesConverted()
    val totalBytesStored: Flow<Long?> = pdfDao.getTotalBytesStored()

    fun getRecentPdfs(limit: Int = 5): Flow<List<PdfDocumentEntity>> = pdfDao.getRecentPdfs(limit)

    suspend fun insertPdf(pdf: PdfDocumentEntity): Long = pdfDao.insertPdf(pdf)
    suspend fun scanDeviceAndSdCardPdfs(): List<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        com.example.data.util.DevicePdfScanner.scanAllDevicePdfs(context)
    }

    suspend fun generateAndSavePdf(
        images: List<SelectedImageItem>,
        config: PdfConversionConfig,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        val result = pdfGenerator.generatePdf(images, config, onProgress)
        if (result.isSuccess) {
            val genResult: PdfGenerationResult = result.getOrThrow()
            val entity = PdfDocumentEntity(
                fileName = genResult.file.name,
                filePath = genResult.file.absolutePath,
                fileSizeBytes = genResult.fileSizeBytes,
                pageCount = genResult.pageCount,
                thumbnailPath = genResult.thumbnailPath,
                pageSizeLabel = config.pageSize.label,
                orientationLabel = config.orientation.label,
                createdAtTimestamp = System.currentTimeMillis(),
                cloudSyncStatus = PdfDocumentEntity.SYNC_STATUS_LOCAL_ONLY
            )
            val newId = pdfDao.insertPdf(entity)
            Result.success(entity.copy(id = newId))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to generate PDF"))
        }
    }

    suspend fun renamePdf(pdf: PdfDocumentEntity, newNameWithoutExt: String): Result<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanName = newNameWithoutExt.trim().replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
            val targetFileName = if (cleanName.endsWith(".pdf", ignoreCase = true)) cleanName else "$cleanName.pdf"
            val currentFile = File(pdf.filePath)

            if (!currentFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Source PDF file does not exist"))
            }

            val targetFile = File(currentFile.parentFile, targetFileName)
            if (targetFile.exists() && targetFile.absolutePath != currentFile.absolutePath) {
                return@withContext Result.failure(IllegalArgumentException("A file with this name already exists"))
            }

            val renamed = currentFile.renameTo(targetFile)
            if (!renamed) {
                return@withContext Result.failure(IllegalStateException("Could not rename file"))
            }

            val updatedEntity = pdf.copy(
                fileName = targetFileName,
                filePath = targetFile.absolutePath
            )
            pdfDao.updatePdf(updatedEntity)

            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath, currentFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
            } catch (_: Exception) {}

            Result.success(updatedEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePdf(pdf: PdfDocumentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete actual file
            val file = File(pdf.filePath)
            if (file.exists()) {
                file.delete()
            }
            // Delete thumbnail if exists
            pdf.thumbnailPath?.let { path ->
                val thumb = File(path)
                if (thumb.exists()) thumb.delete()
            }
            // Delete database row
            pdfDao.deletePdf(pdf)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testAndConnectMongoUri(
        uri: String,
        databaseName: String = "pdf_vault",
        collectionName: String = "stored_pdfs"
    ): Result<com.example.data.model.MongoConnectionConfig> = withContext(Dispatchers.IO) {
        cloudApiService.testAndConnectMongoUri(uri, databaseName, collectionName)
    }

    suspend fun uploadPdfToCloud(
        pdf: PdfDocumentEntity,
        userEmail: String,
        destination: com.example.data.model.UploadDestination = com.example.data.model.UploadDestination.MONGODB,
        mongoConfig: com.example.data.model.MongoConnectionConfig = com.example.data.model.MongoConnectionConfig(),
        onProgress: (progress: Float) -> Unit
    ): Result<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        try {
            // Mark uploading
            val uploadingEntity = pdf.copy(cloudSyncStatus = PdfDocumentEntity.SYNC_STATUS_UPLOADING)
            pdfDao.updatePdf(uploadingEntity)

            val file = File(pdf.filePath)
            if (!file.exists()) {
                val failedEntity = pdf.copy(cloudSyncStatus = PdfDocumentEntity.SYNC_STATUS_FAILED)
                pdfDao.updatePdf(failedEntity)
                return@withContext Result.failure(IllegalStateException("File not found on device"))
            }

            val uploadResult = cloudApiService.uploadPdfMetadataAndFile(
                file = file,
                fileName = pdf.fileName,
                pageCount = pdf.pageCount,
                userId = userEmail,
                destination = destination,
                mongoConfig = mongoConfig,
                onProgress = onProgress
            )

            if (uploadResult.isSuccess) {
                val metadata = uploadResult.getOrThrow()
                val syncedEntity = pdf.copy(
                    cloudSyncStatus = PdfDocumentEntity.SYNC_STATUS_SYNCED,
                    cloudFileId = metadata.fileId,
                    cloudUrl = metadata.cloudStorageUrl,
                    cloudUploadTimestamp = metadata.uploadDate
                )
                pdfDao.updatePdf(syncedEntity)
                Result.success(syncedEntity)
            } else {
                val failedEntity = pdf.copy(cloudSyncStatus = PdfDocumentEntity.SYNC_STATUS_FAILED)
                pdfDao.updatePdf(failedEntity)
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("Cloud upload failed"))
            }
        } catch (e: Exception) {
            val failedEntity = pdf.copy(cloudSyncStatus = PdfDocumentEntity.SYNC_STATUS_FAILED)
            pdfDao.updatePdf(failedEntity)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudFiles(
        userEmail: String,
        destination: com.example.data.model.UploadDestination = com.example.data.model.UploadDestination.MONGODB,
        mongoConfig: com.example.data.model.MongoConnectionConfig = com.example.data.model.MongoConnectionConfig()
    ): Result<List<CloudPdfMetadata>> = withContext(Dispatchers.IO) {
        cloudApiService.fetchUserCloudFiles(userEmail, destination, mongoConfig)
    }

    suspend fun deleteCloudFile(
        fileId: String,
        userEmail: String,
        destination: com.example.data.model.UploadDestination = com.example.data.model.UploadDestination.MONGODB
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val res = cloudApiService.deleteCloudFile(fileId, userEmail, destination)
        res
    }

    suspend fun getCloudQuota(
        userEmail: String,
        destination: com.example.data.model.UploadDestination = com.example.data.model.UploadDestination.MONGODB
    ): Result<CloudStorageQuota> = withContext(Dispatchers.IO) {
        cloudApiService.getStorageQuota(userEmail, destination)
    }

    suspend fun getMongoDatabases(): Result<List<com.example.data.model.MongoDatabaseInfo>> = withContext(Dispatchers.IO) {
        cloudApiService.getMongoDatabases()
    }

    suspend fun getMongoCollections(databaseName: String): Result<List<com.example.data.model.MongoCollectionInfo>> = withContext(Dispatchers.IO) {
        cloudApiService.getMongoCollections(databaseName)
    }

    suspend fun createMongoDatabase(databaseName: String, initialCollection: String = "stored_pdfs"): Result<Boolean> = withContext(Dispatchers.IO) {
        cloudApiService.createMongoDatabase(databaseName, initialCollection)
    }

    suspend fun createMongoCollection(databaseName: String, collectionName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        cloudApiService.createMongoCollection(databaseName, collectionName)
    }
}

