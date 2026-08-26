package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererHelper(val context: Context, val pdfFile: File) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var tempFile: File? = null
    var pageCount: Int = 0
        private set

    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        try {
            tempFile = File(context.cacheDir, "temp_render_${System.currentTimeMillis()}.pdf")
            try {
                pdfFile.inputStream().use { input ->
                    tempFile!!.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                var copied = false
                try {
                    val cursor = context.contentResolver.query(
                        MediaStore.Files.getContentUri("external"),
                        arrayOf(MediaStore.Files.FileColumns._ID),
                        "${MediaStore.Files.FileColumns.DATA} = ?",
                        arrayOf(pdfFile.absolutePath),
                        null
                    )
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val id = c.getLong(0)
                            val uri = android.content.ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                tempFile!!.outputStream().use { output ->
                                    input.copyTo(output)
                                    copied = true
                                }
                            }
                        }
                    }
                } catch (e2: Exception) {
                    android.util.Log.e("PdfRendererHelper", "MediaStore copy failed", e2)
                }
                
                if (!copied) return@withContext false
            }

            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            if (fileDescriptor == null) return@withContext false
            renderer = PdfRenderer(fileDescriptor!!)
            pageCount = renderer!!.pageCount
            true
        } catch (e: Exception) {
            android.util.Log.e("PdfRendererHelper", "PdfRenderer init failed", e)
            close()
            false
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 900): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(this@PdfRendererHelper) {
            try {
                val rnd = renderer ?: return@withContext null
                if (pageIndex < 0 || pageIndex >= rnd.pageCount) return@withContext null
                
                val page = rnd.openPage(pageIndex)
                val aspectRatio = (page.height.toFloat() / page.width.toFloat()).coerceIn(0.2f, 5.0f)
                val height = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)
                
                val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                android.util.Log.e("PdfRendererHelper", "Error rendering page $pageIndex", e)
                null
            }
        }
    }

    suspend fun renderPagePatch(
        pageIndex: Int,
        baseWidth: Int,
        scale: Float,
        patchX: Int,
        patchY: Int,
        patchWidth: Int,
        patchHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(this@PdfRendererHelper) {
            try {
                val rnd = renderer ?: return@withContext null
                if (pageIndex < 0 || pageIndex >= rnd.pageCount) return@withContext null
                
                if (patchWidth <= 0 || patchHeight <= 0) return@withContext null
                
                val page = rnd.openPage(pageIndex)
                val aspectRatio = (page.height.toFloat() / page.width.toFloat()).coerceIn(0.2f, 5.0f)
                val baseHeight = (baseWidth * aspectRatio).toInt().coerceAtLeast(1)
                
                val bitmap = Bitmap.createBitmap(patchWidth, patchHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                val transform = android.graphics.Matrix()
                transform.postScale(
                    (baseWidth * scale) / page.width.toFloat(),
                    (baseHeight * scale) / page.height.toFloat()
                )
                transform.postTranslate(-(patchX * scale), -(patchY * scale))
                
                val clip = android.graphics.Rect(0, 0, patchWidth, patchHeight)
                
                page.render(bitmap, clip, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                android.util.Log.e("PdfRendererHelper", "Error rendering patch $pageIndex", e)
                null
            }
        }
    }

    fun close() {
        try {
            renderer?.close()
            fileDescriptor?.close()
            tempFile?.delete()
        } catch (e: Exception) {}
        renderer = null
        fileDescriptor = null
    }
}
