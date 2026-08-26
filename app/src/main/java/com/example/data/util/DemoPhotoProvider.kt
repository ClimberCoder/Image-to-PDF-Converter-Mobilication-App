package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object DemoPhotoProvider {

    data class DemoPhotoItem(
        val title: String,
        val subtitle: String,
        val tag: String,
        val uri: Uri
    )

    fun getOrGenerateDemoPhotos(context: Context): List<DemoPhotoItem> {
        val demoDir = File(context.cacheDir, "demo_photos").apply { if (!exists()) mkdirs() }

        val photo1 = getOrGenerateLandscapePhoto(File(demoDir, "demo_travel_mountain.jpg"))
        val photo2 = getOrGenerateReceiptPhoto(File(demoDir, "demo_store_receipt.jpg"))
        val photo3 = getOrGenerateNotesPhoto(File(demoDir, "demo_study_notes.jpg"))
        val photo4 = getOrGenerateCertificatePhoto(File(demoDir, "demo_certificate.jpg"))

        return listOf(
            DemoPhotoItem(
                title = "Travel Sunset Photo",
                subtitle = "Landscape photography (1200x900)",
                tag = "Travel",
                uri = Uri.fromFile(photo1)
            ),
            DemoPhotoItem(
                title = "Business Invoice & Receipt",
                subtitle = "Itemized expense document",
                tag = "Receipt",
                uri = Uri.fromFile(photo2)
            ),
            DemoPhotoItem(
                title = "Whiteboard Study Notes",
                subtitle = "Lecture formulas & diagrams",
                tag = "Notes",
                uri = Uri.fromFile(photo3)
            ),
            DemoPhotoItem(
                title = "Official Certificate",
                subtitle = "Document with verification seal",
                tag = "Document",
                uri = Uri.fromFile(photo4)
            )
        )
    }

    private fun getOrGenerateLandscapePhoto(file: File): File {
        if (file.exists() && file.length() > 0) return file
        val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sky gradient
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(0f, 0f, 1200f, 900f, paint)

        // Sunset Glow
        paint.color = Color.parseColor("#EA580C")
        canvas.drawRect(0f, 300f, 1200f, 600f, paint)
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawCircle(600f, 420f, 130f, paint)

        // Mountains
        val mtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#334155") }
        val path1 = Path().apply {
            moveTo(0f, 600f)
            lineTo(350f, 280f)
            lineTo(700f, 600f)
            close()
        }
        canvas.drawPath(path1, mtnPaint)

        mtnPaint.color = Color.parseColor("#1E293B")
        val path2 = Path().apply {
            moveTo(500f, 600f)
            lineTo(850f, 320f)
            lineTo(1200f, 600f)
            close()
        }
        canvas.drawPath(path2, mtnPaint)

        // Lake reflection
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 600f, 1200f, 900f, paint)
        paint.color = Color.parseColor("#38BDF8")
        paint.alpha = 70
        for (i in 620..880 step 25) {
            canvas.drawRoundRect(RectF(300f, i.toFloat(), 900f, i + 8f), 4f, 4f, paint)
        }

        // Caption watermark
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            isFakeBoldText = true
        }
        canvas.drawText("Rocky Mountains National Park • Photo Sample", 60f, 840f, textPaint)

        saveBitmapToFile(bitmap, file)
        return file
    }

    private fun getOrGenerateReceiptPhoto(file: File): File {
        if (file.exists() && file.length() > 0) return file
        val bitmap = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Warm receipt paper background
        paint.color = Color.parseColor("#FDFBF7")
        canvas.drawRect(0f, 0f, 800f, 1100f, paint)

        // Border outline
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(20f, 20f, 780f, 1080f, paint)
        paint.style = Paint.Style.FILL

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 38f
            isFakeBoldText = true
        }
        canvas.drawText("OFFICE & COFFEE DEPOT", 160f, 100f, textPaint)

        textPaint.textSize = 22f
        textPaint.isFakeBoldText = false
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Tax Invoice #INV-2026-8941", 240f, 140f, textPaint)
        canvas.drawText("Date: 2026-08-25  |  Time: 12:45 PM", 210f, 175f, textPaint)

        // Divider
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(50f, 210f, 750f, 210f, paint)

        // Line items
        textPaint.textSize = 26f
        textPaint.color = Color.parseColor("#1E293B")
        val items = listOf(
            "1. Cloud Storage Enterprise 100GB" to "$24.99",
            "2. Scanner & PDF Pro License" to "$49.00",
            "3. Document Shredder Service" to "$15.50",
            "4. Premium Matte Paper (500 sheets)" to "$12.80",
            "5. Barcode & OCR Plugin Kit" to "$29.99"
        )
        var y = 280f
        items.forEach { (name, price) ->
            canvas.drawText(name, 60f, y, textPaint)
            canvas.drawText(price, 650f, y, textPaint)
            y += 60f
        }

        // Subtotal & Total
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(50f, 620f, 750f, 620f, paint)

        textPaint.isFakeBoldText = true
        textPaint.textSize = 30f
        canvas.drawText("TOTAL AMOUNT PAID:", 60f, 680f, textPaint)
        textPaint.color = Color.parseColor("#DC2626")
        canvas.drawText("$132.28", 630f, 680f, textPaint)

        // Barcode lines at bottom
        paint.color = Color.parseColor("#0F172A")
        var barX = 180f
        for (i in 0..40) {
            val width = if (i % 3 == 0) 8f else if (i % 2 == 0) 4f else 2f
            canvas.drawRect(barX, 850f, barX + width, 980f, paint)
            barX += width + if (i % 4 == 0) 8f else 5f
        }

        saveBitmapToFile(bitmap, file)
        return file
    }

    private fun getOrGenerateNotesPhoto(file: File): File {
        if (file.exists() && file.length() > 0) return file
        val bitmap = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Grid paper background
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(0f, 0f, 800f, 1100f, paint)

        // Blue horizontal ruled lines
        paint.color = Color.parseColor("#E2E8F0")
        for (y in 80..1050 step 45) {
            canvas.drawLine(40f, y.toFloat(), 760f, y.toFloat(), paint)
        }
        // Red margin line
        paint.color = Color.parseColor("#FCA5A5")
        canvas.drawLine(120f, 0f, 120f, 1100f, paint)

        // Title
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 34f
            isFakeBoldText = true
        }
        canvas.drawText("Computer Science • Database Architectures", 140f, 65f, textPaint)

        // Notes content
        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#334155")
        textPaint.isFakeBoldText = false
        val notes = listOf(
            "1. Document Databases (MongoDB):",
            "   • Stores data in BSON / JSON format.",
            "   • Flexible schema & high write throughput.",
            "   • GridFS handles large binary files (>16MB).",
            "",
            "2. PDF Generation Pipeline:",
            "   • Image compression (JPEG/WebP) -> Canvas.",
            "   • Page dimensioning: A4 (595x842 pt).",
            "   • Local SQLite Room indices for fast query.",
            "",
            "3. Key Takeaways:",
            "   ✓ Fully offline image rendering pipeline.",
            "   ✓ 100% on-device cryptographic security."
        )

        var noteY = 155f
        notes.forEach { line ->
            canvas.drawText(line, 140f, noteY, textPaint)
            noteY += 45f
        }

        saveBitmapToFile(bitmap, file)
        return file
    }

    private fun getOrGenerateCertificatePhoto(file: File): File {
        if (file.exists() && file.length() > 0) return file
        val bitmap = Bitmap.createBitmap(1100, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Certificate Parchment
        paint.color = Color.parseColor("#FEFCE8")
        canvas.drawRect(0f, 0f, 1100f, 800f, paint)

        // Gold border
        paint.color = Color.parseColor("#CA8A04")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawRect(30f, 30f, 1070f, 770f, paint)
        paint.strokeWidth = 3f
        canvas.drawRect(50f, 50f, 1050f, 750f, paint)
        paint.style = Paint.Style.FILL

        // Header
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#854D0E")
            textSize = 42f
            isFakeBoldText = true
        }
        canvas.drawText("CERTIFICATE OF COMPLETION", 220f, 160f, textPaint)

        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#4B5563")
        textPaint.isFakeBoldText = false
        canvas.drawText("This sample document verifies successful completion of", 260f, 250f, textPaint)

        textPaint.textSize = 34f
        textPaint.color = Color.parseColor("#1E293B")
        textPaint.isFakeBoldText = true
        canvas.drawText("Professional Mobile Engineering Course", 230f, 320f, textPaint)

        // Gold Ribbon Seal
        paint.color = Color.parseColor("#EAB308")
        canvas.drawCircle(880f, 580f, 70f, paint)
        paint.color = Color.parseColor("#A16207")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(880f, 580f, 62f, paint)
        paint.style = Paint.Style.FILL

        val sealText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText("VERIFIED", 835f, 585f, sealText)

        saveBitmapToFile(bitmap, file)
        return file
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
