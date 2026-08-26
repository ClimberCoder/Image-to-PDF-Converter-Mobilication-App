package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val cloudSyncStatus: String = SYNC_STATUS_LOCAL_ONLY, // LOCAL_ONLY, UPLOADING, SYNCED, FAILED
    val cloudFileId: String? = null,
    val cloudUrl: String? = null,
    val cloudUploadTimestamp: Long? = null,
    val pageSizeLabel: String = "A4",
    val orientationLabel: String = "Portrait"
) {
    companion object {
        const val SYNC_STATUS_LOCAL_ONLY = "LOCAL_ONLY"
        const val SYNC_STATUS_UPLOADING = "UPLOADING"
        const val SYNC_STATUS_SYNCED = "SYNCED"
        const val SYNC_STATUS_FAILED = "FAILED"
    }

    val formattedSize: String
        get() {
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$fileSizeBytes B"
            }
        }

    val isMemoryCard: Boolean
        get() {
            val path = filePath.lowercase()
            return (path.contains("/storage/") || path.contains("/mnt/")) &&
                    !path.contains("/storage/emulated/0") &&
                    !path.contains("/storage/emulated/legacy") &&
                    !path.contains("/data/user/") &&
                    !path.contains("/data/data/")
        }

    val storageLocationLabel: String
        get() = if (isMemoryCard) "Memory Card" else "Phone Storage"
}
