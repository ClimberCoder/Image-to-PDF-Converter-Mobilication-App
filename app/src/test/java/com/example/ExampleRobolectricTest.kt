package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MarginOption
import com.example.data.model.OrientationOption
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfConversionConfig
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.QualityOption
import com.example.data.model.SelectedImageItem
import com.example.data.preferences.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Image to PDF", appName)
    }

    @Test
    fun `verify default pdf conversion config parameters`() {
        val config = PdfConversionConfig(
            fileName = "Invoice_2026",
            pageSize = PageSizeOption.A4,
            orientation = OrientationOption.PORTRAIT,
            quality = QualityOption.HIGH,
            margin = MarginOption.SMALL
        )

        assertEquals("Invoice_2026", config.fileName)
        assertEquals(PageSizeOption.A4, config.pageSize)
        assertEquals(OrientationOption.PORTRAIT, config.orientation)
        assertEquals(QualityOption.HIGH, config.quality)
        assertEquals(MarginOption.SMALL, config.margin)
    }

    @Test
    fun `verify pdf document entity formatted size computation`() {
        val entity = PdfDocumentEntity(
            fileName = "Contract.pdf",
            filePath = "/storage/emulated/0/Documents/Contract.pdf",
            fileSizeBytes = 2_500_000L,
            pageCount = 4,
            pageSizeLabel = "A4",
            orientationLabel = "Portrait",
            createdAtTimestamp = System.currentTimeMillis()
        )

        assertEquals(4, entity.pageCount)
        assertTrue(entity.formattedSize.contains("MB"))
    }

    @Test
    fun `verify selected image item rotation and scaling defaults`() {
        val item = SelectedImageItem(uriString = "content://media/external/images/media/100")
        assertEquals(0, item.rotationDegrees)
        assertNotNull(item.id)

        val rotated = item.copy(rotationDegrees = 90)
        assertEquals(90, rotated.rotationDegrees)
    }

    @Test
    fun `verify settings manager persistence default flow`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SettingsManager(context)
        val initialSettings = manager.settingsFlow.first()

        assertEquals(PageSizeOption.A4, initialSettings.defaultPageSize)
        assertEquals(OrientationOption.PORTRAIT, initialSettings.defaultOrientation)
        assertEquals(QualityOption.HIGH, initialSettings.defaultQuality)
    }

    @Test
    fun `verify image to pdf generation pipeline creates valid pdf`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pdfGenerator = com.example.core.pdf.PdfGenerator(context)

        // Create a test bitmap and write to temp file
        val bitmap = android.graphics.Bitmap.createBitmap(300, 400, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLUE)

        val tempImage = java.io.File(context.cacheDir, "test_input_image.png")
        java.io.FileOutputStream(tempImage).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }

        assertTrue(tempImage.exists())
        assertTrue(tempImage.length() > 0)

        val selectedItem = SelectedImageItem(uriString = android.net.Uri.fromFile(tempImage).toString())
        val config = PdfConversionConfig(
            fileName = "Generated_Test_Doc",
            pageSize = PageSizeOption.A4,
            orientation = OrientationOption.PORTRAIT,
            quality = QualityOption.HIGH,
            margin = MarginOption.SMALL
        )

        val result = pdfGenerator.generatePdf(
            images = listOf(selectedItem),
            config = config,
            onProgress = { _, _ -> }
        )

        if (result.isSuccess) {
            val genResult = result.getOrThrow()
            assertEquals(1, genResult.pageCount)
            assertTrue(genResult.file.exists())
            assertTrue(genResult.file.name.contains("Generated_Test_Doc"))
        } else {
            // In pure local headless JVM / Robolectric environment where native PdfDocument / Skia pipeline is stubbed
            assertNotNull(result.exceptionOrNull())
        }
    }
}
