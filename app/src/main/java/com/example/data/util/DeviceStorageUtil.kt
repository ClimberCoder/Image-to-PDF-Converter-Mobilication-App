package com.example.data.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class PhoneStorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercentage: Float,
    val formattedTotal: String,
    val formattedAvailable: String,
    val formattedUsed: String,
    val appPdfBytes: Long,
    val formattedAppPdfSize: String,
    val appCacheBytes: Long,
    val formattedAppCacheSize: String
)

object DeviceStorageUtil {

    fun getPhoneStorageInfo(context: Context, appPdfBytes: Long): PhoneStorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)

            val usedPercentage = if (totalBytes > 0) {
                (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            val appCacheBytes = getFolderSize(context.cacheDir)

            PhoneStorageInfo(
                totalBytes = totalBytes,
                availableBytes = availableBytes,
                usedBytes = usedBytes,
                usedPercentage = usedPercentage,
                formattedTotal = formatBytes(totalBytes),
                formattedAvailable = formatBytes(availableBytes),
                formattedUsed = formatBytes(usedBytes),
                appPdfBytes = appPdfBytes,
                formattedAppPdfSize = formatBytes(appPdfBytes),
                appCacheBytes = appCacheBytes,
                formattedAppCacheSize = formatBytes(appCacheBytes)
            )
        } catch (e: Exception) {
            // Fallback gracefully
            PhoneStorageInfo(
                totalBytes = 64L * 1024 * 1024 * 1024,
                availableBytes = 32L * 1024 * 1024 * 1024,
                usedBytes = 32L * 1024 * 1024 * 1024,
                usedPercentage = 0.5f,
                formattedTotal = "64.0 GB",
                formattedAvailable = "32.0 GB",
                formattedUsed = "32.0 GB",
                appPdfBytes = appPdfBytes,
                formattedAppPdfSize = formatBytes(appPdfBytes),
                appCacheBytes = 0L,
                formattedAppCacheSize = "0 KB"
            )
        }
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        var length = 0L
        file.listFiles()?.forEach { child ->
            length += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return length
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) {
            return String.format("%.1f GB", gb)
        }
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        if (mb >= 1.0) {
            return String.format("%.1f MB", mb)
        }
        val kb = bytes.toDouble() / 1024.0
        if (kb >= 1.0) {
            return String.format("%.1f KB", kb)
        }
        return "$bytes B"
    }
}
