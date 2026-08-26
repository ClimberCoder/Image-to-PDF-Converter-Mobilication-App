package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UploadDestination(val label: String, val description: String) {
    MONGODB("MongoDB Database", "Store PDF binary & metadata directly in MongoDB Atlas / URI cluster"),
    APP_VAULT("App Cloud Vault", "Store encrypted in default cloud storage vault")
}

data class MongoDatabaseInfo(
    val name: String,
    val collections: List<String> = emptyList(),
    val documentCount: Int = 0,
    val sizeBytes: Long = 0L,
    val isDefault: Boolean = false
)

data class MongoCollectionInfo(
    val name: String,
    val databaseName: String,
    val documentCount: Int = 0,
    val sizeBytes: Long = 0L
)

data class MongoConnectionConfig(
    val uri: String = "",
    val databaseName: String = "pdf_vault",
    val collectionName: String = "stored_pdfs",
    val isConnected: Boolean = false,
    val clusterHost: String = "",
    val lastPingMs: Long = 0L,
    val lastConnectedTimestamp: Long = 0L,
    val connectionError: String? = null,
    val availableDatabases: List<String> = listOf("pdf_vault", "user_documents", "scans_db"),
    val collectionsMap: Map<String, List<String>> = mapOf(
        "pdf_vault" to listOf("stored_pdfs", "invoices", "receipts"),
        "user_documents" to listOf("personal", "work"),
        "scans_db" to listOf("camera_scans")
    )
) {
    val maskedUri: String
        get() {
            if (uri.isBlank()) return ""
            return try {
                if (uri.contains("@")) {
                    val schemeAndCreds = uri.substringBefore("@")
                    val hostAndPath = uri.substringAfter("@")
                    val scheme = schemeAndCreds.substringBefore("://")
                    "$scheme://*****:*****@$hostAndPath"
                } else {
                    uri.take(20) + "..."
                }
            } catch (e: Exception) {
                "mongodb://*****"
            }
        }
}

data class CloudPdfMetadata(
    val fileId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val uploadDate: Long = System.currentTimeMillis(),
    val cloudStorageUrl: String = "",
    val userId: String = "local_user",
    val syncStatus: String = "SYNCED",
    val md5Hash: String = "",
    val destination: UploadDestination = UploadDestination.MONGODB,
    val mongoObjectId: String = "",
    val mongoDatabase: String = "pdf_vault",
    val mongoCollection: String = "stored_pdfs",
    val wasDatabaseCreatedOnFly: Boolean = false,
    val wasCollectionCreatedOnFly: Boolean = false,
    val mongoResolutionMessage: String = ""
) {
    val dayOfWeek: String
        get() = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(createdAt))

    val createdDateFormatted: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(createdAt))

    val createdTimeFormatted: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(createdAt))

    val fullCreatedDayDateTime: String
        get() = "$dayOfWeek, $createdDateFormatted at $createdTimeFormatted"

    val formattedSize: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            val kb = fileSizeBytes / 1024.0
            return if (mb >= 1.0) String.format(Locale.US, "%.2f MB", mb) else String.format(Locale.US, "%.1f KB", kb)
        }
}

data class CloudStorageQuota(
    val usedBytes: Long,
    val totalQuotaBytes: Long = 500L * 1024L * 1024L, // 500 MB Free Tier
    val totalFiles: Int = 0,
    val accountTier: String = "MongoDB Cloud Vault",
    val userEmail: String = "user@offline-first.vault"
) {
    val usedMb: Double get() = usedBytes / (1024.0 * 1024.0)
    val totalMb: Double get() = totalQuotaBytes / (1024.0 * 1024.0)
    val usagePercentage: Float get() = if (totalQuotaBytes > 0) (usedBytes.toFloat() / totalQuotaBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val formattedUsed: String get() = String.format(Locale.US, "%.2f MB", usedMb)
    val formattedTotal: String get() = String.format(Locale.US, "%.0f MB", totalMb)
}


