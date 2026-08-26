package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.R
import com.example.data.model.MarginOption
import com.example.data.model.OrientationOption
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfConversionConfig
import com.example.data.model.QualityOption
import com.example.data.model.ScaleTypeOption
import com.example.data.model.SelectedImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class PdfGenerationResult(
    val file: File,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val thumbnailPath: String?
)

class PdfGenerator(private val context: Context) {

    suspend fun generatePdf(
        images: List<SelectedImageItem>,
        config: PdfConversionConfig,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<PdfGenerationResult> = withContext(Dispatchers.IO) {
        if (images.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No images selected for PDF conversion"))
        }

        val pdfDocument = PdfDocument()
        var thumbnailSavedPath: String? = null

        try {
            val appFolderName = try {
                context.getString(R.string.app_name).ifBlank { "Img to PDF" }
            } catch (_: Exception) {
                "Img to PDF"
            }

            // Target public Documents/AppName or app-specific documents
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val publicFolder = File(publicDocs, appFolderName)

            val pdfDir = try {
                if (!publicFolder.exists()) {
                    publicFolder.mkdirs()
                }
                if (publicFolder.exists() && publicFolder.canWrite()) {
                    publicFolder
                } else {
                    val extDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), appFolderName)
                    if (!extDir.exists()) extDir.mkdirs()
                    extDir
                }
            } catch (_: Exception) {
                File(context.filesDir, appFolderName).apply { if (!exists()) mkdirs() }
            }

            val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }

            // Format Filename
            val baseName = if (config.fileName.isNotBlank()) {
                config.fileName.trim().replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
            } else {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                "Doc_$timeStamp"
            }
            val finalFileName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
            val outputFile = File(pdfDir, finalFileName)

            val totalImages = images.size

            images.forEachIndexed { index, imageItem ->
                onProgress(index + 1, totalImages)

                val bitmap = decodeBitmapFromUri(context, Uri.parse(imageItem.uriString), imageItem.rotationDegrees, config.quality)
                    ?: throw IllegalStateException("Failed to load image: ${imageItem.uriString}")

                // Save first page thumbnail for quick UI preview
                if (index == 0) {
                    val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.png")
                    saveThumbnail(bitmap, thumbFile)
                    thumbnailSavedPath = thumbFile.absolutePath
                }

                // Determine Page Width and Height in points (72 points = 1 inch)
                val (pageWidth, pageHeight) = calculatePageDimensions(
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                    pageSize = config.pageSize,
                    orientation = config.orientation
                )

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fill background white
                canvas.drawColor(Color.WHITE)

                // Calculate drawing rect considering margins
                val marginPt = config.margin.marginPt.toFloat()
                val availableWidth = (pageWidth - (marginPt * 2)).coerceAtLeast(1f)
                val availableHeight = (pageHeight - (marginPt * 2)).coerceAtLeast(1f)

                val destRect = calculateDestinationRect(
                    bitmapWidth = bitmap.width.toFloat(),
                    bitmapHeight = bitmap.height.toFloat(),
                    availableWidth = availableWidth,
                    availableHeight = availableHeight,
                    marginPt = marginPt,
                    scaleType = imageItem.scaleType
                )

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(page)
                bitmap.recycle() // free memory immediately
            }

            // Write output file
            FileOutputStream(outputFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }

            // Immediately scan with Android MediaScanner so it appears in File Manager / Files app
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
            } catch (_: Exception) {}

            Result.success(
                PdfGenerationResult(
                    file = outputFile,
                    pageCount = totalImages,
                    fileSizeBytes = outputFile.length(),
                    thumbnailPath = thumbnailSavedPath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }
    }

    private fun calculatePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        pageSize: PageSizeOption,
        orientation: OrientationOption
    ): Pair<Int, Int> {
        if (pageSize == PageSizeOption.ORIGINAL) {
            // Adapt page dimensions proportionally to exact image aspect ratio (normalized to standard PDF point grid)
            val isImageLandscape = bitmapWidth > bitmapHeight
            return if (isImageLandscape) {
                val widthPt = 842
                val heightPt = ((842f * bitmapHeight) / bitmapWidth).toInt().coerceAtLeast(100)
                Pair(widthPt, heightPt)
            } else {
                val widthPt = 595
                val heightPt = ((595f * bitmapHeight) / bitmapWidth).toInt().coerceAtLeast(100)
                Pair(widthPt, heightPt)
            }
        }

        var width = pageSize.widthPt
        var height = pageSize.heightPt

        when (orientation) {
            OrientationOption.PORTRAIT -> {
                if (width > height) {
                    val tmp = width
                    width = height
                    height = tmp
                }
            }
            OrientationOption.LANDSCAPE -> {
                if (height > width) {
                    val tmp = width
                    width = height
                    height = tmp
                }
            }
            OrientationOption.AUTO -> {
                // Automatically match page orientation to the real image aspect ratio
                if (bitmapWidth > bitmapHeight) {
                    // Landscape image -> Landscape PDF page
                    if (height > width) {
                        val tmp = width
                        width = height
                        height = tmp
                    }
                } else {
                    // Portrait image -> Portrait PDF page
                    if (width > height) {
                        val tmp = width
                        width = height
                        height = tmp
                    }
                }
            }
        }
        return Pair(width, height)
    }

    private fun calculateDestinationRect(
        bitmapWidth: Float,
        bitmapHeight: Float,
        availableWidth: Float,
        availableHeight: Float,
        marginPt: Float,
        scaleType: ScaleTypeOption
    ): RectF {
        val imageRatio = bitmapWidth / bitmapHeight
        val containerRatio = availableWidth / availableHeight

        return when (scaleType) {
            ScaleTypeOption.FIT_PAGE -> {
                var drawWidth: Float
                var drawHeight: Float

                if (imageRatio > containerRatio) {
                    drawWidth = availableWidth
                    drawHeight = availableWidth / imageRatio
                } else {
                    drawHeight = availableHeight
                    drawWidth = availableHeight * imageRatio
                }

                val left = marginPt + ((availableWidth - drawWidth) / 2f)
                val top = marginPt + ((availableHeight - drawHeight) / 2f)
                RectF(left, top, left + drawWidth, top + drawHeight)
            }
            ScaleTypeOption.FILL_PAGE -> {
                var drawWidth: Float
                var drawHeight: Float

                if (imageRatio > containerRatio) {
                    drawHeight = availableHeight
                    drawWidth = availableHeight * imageRatio
                } else {
                    drawWidth = availableWidth
                    drawHeight = availableWidth / imageRatio
                }

                val left = marginPt + ((availableWidth - drawWidth) / 2f)
                val top = marginPt + ((availableHeight - drawHeight) / 2f)
                RectF(left, top, left + drawWidth, top + drawHeight)
            }
            ScaleTypeOption.ORIGINAL -> {
                val drawWidth = min(bitmapWidth, availableWidth)
                val drawHeight = min(bitmapHeight, availableHeight)
                val left = marginPt + ((availableWidth - drawWidth) / 2f)
                val top = marginPt + ((availableHeight - drawHeight) / 2f)
                RectF(left, top, left + drawWidth, top + drawHeight)
            }
        }
    }

    private fun decodeBitmapFromUri(
        context: Context,
        uri: Uri,
        rotationDegrees: Int,
        quality: QualityOption
    ): Bitmap? {
        val maxDimension = when (quality) {
            QualityOption.HIGH -> 2400
            QualityOption.MEDIUM -> 1600
            QualityOption.LOW -> 1000
        }

        // If it's a file scheme or direct file path that exists, decode directly from file path
        val filePath = when {
            uri.scheme == "file" -> uri.path
            uri.path != null && File(uri.path ?: "").exists() -> uri.path
            else -> null
        }

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists() && file.length() > 0) {
                try {
                    // Get Exif Orientation
                    var exifRotation = 0
                    try {
                        val exif = android.media.ExifInterface(file.absolutePath)
                        val orientation = exif.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL
                        )
                        exifRotation = when (orientation) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

                    var sampleSize = 1
                    val maxSide = max(boundsOptions.outWidth, boundsOptions.outHeight)
                    if (maxSide > 0) {
                        while ((maxSide / sampleSize) > maxDimension) {
                            sampleSize *= 2
                        }
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    val directFileBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    if (directFileBitmap != null) {
                        return applyRotation(directFileBitmap, rotationDegrees + exifRotation)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Try decoding with content stream or fallback
        fun openStream(): InputStream? {
            return try {
                when (uri.scheme) {
                    "file" -> File(uri.path ?: "").takeIf { it.exists() }?.inputStream()
                    "content" -> context.contentResolver.openInputStream(uri)
                    else -> {
                        val path = uri.path
                        if (path != null && File(path).exists()) {
                            File(path).inputStream()
                        } else {
                            context.contentResolver.openInputStream(uri)
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        try {
            // Read bytes into memory to allow reliable multiple decodes without stream reset issues
            val bytes = openStream()?.use { it.readBytes() } ?: return null
            if (bytes.isEmpty()) return null
            
            // Get Exif Orientation
            var exifRotation = 0
            try {
                val exif = android.media.ExifInterface(bytes.inputStream())
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                exifRotation = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 1. Decode bounds
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight

            // 2. Calculate inSampleSize
            var sampleSize = 1
            val maxSide = max(rawWidth, rawHeight)
            if (maxSide > 0) {
                while ((maxSide / sampleSize) > maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val baseBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return null

            return applyRotation(baseBitmap, rotationDegrees + exifRotation)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun applyRotation(baseBitmap: Bitmap, rotationDegrees: Int): Bitmap {
        return if (rotationDegrees % 360 != 0) {
            try {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)
                if (rotatedBitmap != baseBitmap) {
                    baseBitmap.recycle()
                }
                rotatedBitmap
            } catch (_: Exception) {
                baseBitmap
            }
        } else {
            baseBitmap
        }
    }

    private fun saveThumbnail(source: Bitmap, targetFile: File) {
        try {
            val maxThumbDim = 320
            val ratio = source.width.toFloat() / source.height.toFloat()
            val (thumbWidth, thumbHeight) = if (ratio > 1f) {
                Pair(maxThumbDim, (maxThumbDim / ratio).toInt().coerceAtLeast(1))
            } else {
                Pair((maxThumbDim * ratio).toInt().coerceAtLeast(1), maxThumbDim)
            }

            val thumbBitmap = Bitmap.createScaledBitmap(source, thumbWidth, thumbHeight, true)
            FileOutputStream(targetFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            }
            if (thumbBitmap != source) {
                thumbBitmap.recycle()
            }
        } catch (_: Exception) {}
    }
}
