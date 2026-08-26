package com.example.data.util

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.model.PdfDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DevicePdfScanner {

    /**
     * Scans phone storage and memory card / SD card for all PDF files.
     * Highly optimized for low memory usage and high speed on older Android devices.
     */
    suspend fun scanAllDevicePdfs(context: Context): List<PdfDocumentEntity> = withContext(Dispatchers.IO) {
        val foundPdfs = LinkedHashMap<String, PdfDocumentEntity>()

        // 1. Scan MediaStore (covers both internal storage and mounted SD cards)
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
            val selectionArgs = arrayOf("application/pdf", "%.pdf")
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            val queryUri = MediaStore.Files.getContentUri("external")
            val cursor: Cursor? = context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (c.moveToNext()) {
                    val filePath = c.getString(dataCol) ?: continue
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0 && file.name.endsWith(".pdf", ignoreCase = true)) {
                        val fileName = c.getString(nameCol) ?: file.name
                        val size = if (c.getLong(sizeCol) > 0) c.getLong(sizeCol) else file.length()
                        val modifiedSeconds = c.getLong(dateCol)
                        val timestamp = if (modifiedSeconds > 0) modifiedSeconds * 1000L else file.lastModified()

                        val entity = PdfDocumentEntity(
                            id = 0L,
                            fileName = fileName,
                            filePath = file.absolutePath,
                            fileSizeBytes = size,
                            pageCount = 1, // Fast placeholder; rendered on demand
                            createdAtTimestamp = timestamp,
                            thumbnailPath = null,
                            pageSizeLabel = "A4",
                            orientationLabel = "Portrait"
                        )
                        foundPdfs[file.canonicalPath] = entity
                    }
                }
            }
        } catch (_: Exception) {
            // MediaStore might be restricted or empty in some environments
        }

        // 2. Direct scan standard internal storage folders (Documents, Downloads)
        try {
            val standardDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(Environment.getExternalStorageDirectory(), "PDFs"),
                File(Environment.getExternalStorageDirectory(), "Documents"),
                context.getExternalFilesDir(null)
            )

            for (dir in standardDirs) {
                if (dir != null && dir.exists() && dir.isDirectory) {
                    scanDirectoryShallow(dir, foundPdfs, maxDepth = 2)
                }
            }
        } catch (_: Exception) {}

        // 3. Scan external SD Cards / Memory Cards
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (extDir in externalDirs) {
                if (extDir != null) {
                    // Traverse up to find root of SD card (e.g. /storage/XXXX-XXXX/)
                    var parent: File? = extDir
                    var sdCardRoot: File? = null
                    while (parent != null) {
                        if (parent.parentFile?.absolutePath == "/storage" &&
                            !parent.name.equals("emulated", ignoreCase = true) &&
                            !parent.name.equals("self", ignoreCase = true)
                        ) {
                            sdCardRoot = parent
                            break
                        }
                        parent = parent.parentFile
                    }

                    if (sdCardRoot != null && sdCardRoot.exists() && sdCardRoot.canRead()) {
                        scanDirectoryShallow(sdCardRoot, foundPdfs, maxDepth = 2)
                    }
                }
            }
        } catch (_: Exception) {}

        // Return sorted newest first
        foundPdfs.values.sortedByDescending { it.createdAtTimestamp }
    }

    private fun scanDirectoryShallow(
        dir: File,
        targetMap: MutableMap<String, PdfDocumentEntity>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth > maxDepth || !dir.canRead()) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryShallow(file, targetMap, maxDepth, currentDepth + 1)
            } else if (file.isFile && file.name.endsWith(".pdf", ignoreCase = true) && file.length() > 0) {
                val canonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                if (!targetMap.containsKey(canonical)) {
                    targetMap[canonical] = PdfDocumentEntity(
                        id = 0L,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileSizeBytes = file.length(),
                        pageCount = 1,
                        createdAtTimestamp = file.lastModified(),
                        thumbnailPath = null,
                        pageSizeLabel = "A4",
                        orientationLabel = "Portrait"
                    )
                }
            }
        }
    }
}
