package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.PdfDocumentEntity
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun home_screen_screenshot() {
        val samplePdfs = listOf(
            PdfDocumentEntity(
                id = 1L,
                fileName = "Scanned_Receipts.pdf",
                filePath = "/dummy/path/Scanned_Receipts.pdf",
                fileSizeBytes = 1024 * 1024 * 2,
                pageCount = 3,
                createdAtTimestamp = System.currentTimeMillis()
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    recentPdfs = samplePdfs,
                    totalCount = 1,
                    onSelectImagesClick = {},
                    onOpenPdf = {},
                    onSharePdf = {},
                    onPreviewPdf = {},
                    onNavigateTo = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen.png")
    }
}
