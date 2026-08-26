package com.example.data.model

import java.util.UUID

enum class ScaleTypeOption(val label: String, val description: String) {
    FIT_PAGE("Fit to Page", "Entire image visible with clean margins"),
    FILL_PAGE("Fill Page", "Fills page boundary, cropping edges if needed"),
    ORIGINAL("Original", "Matches original image aspect & pixel size")
}

data class SelectedImageItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val rotationDegrees: Int = 0,
    val scaleType: ScaleTypeOption = ScaleTypeOption.FIT_PAGE,
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0L
) {
    val effectiveWidth: Int
        get() = if (rotationDegrees % 180 != 0) height else width

    val effectiveHeight: Int
        get() = if (rotationDegrees % 180 != 0) width else height

    val isLandscape: Boolean
        get() = effectiveWidth > effectiveHeight && effectiveHeight > 0

    val aspectRatioLabel: String
        get() {
            if (effectiveWidth <= 0 || effectiveHeight <= 0) return "Auto"
            val gcdVal = gcd(effectiveWidth, effectiveHeight)
            val simpleW = effectiveWidth / gcdVal
            val simpleH = effectiveHeight / gcdVal
            return if (simpleW in 1..20 && simpleH in 1..20) {
                "$simpleW:$simpleH"
            } else if (isLandscape) {
                "Landscape"
            } else {
                "Portrait"
            }
        }

    val dimensionString: String
        get() = if (effectiveWidth > 0 && effectiveHeight > 0) "${effectiveWidth}×${effectiveHeight}" else "Auto Size"

    private fun gcd(a: Int, b: Int): Int {
        var n1 = a
        var n2 = b
        while (n2 != 0) {
            val temp = n2
            n2 = n1 % n2
            n1 = temp
        }
        return if (n1 > 0) n1 else 1
    }
}
