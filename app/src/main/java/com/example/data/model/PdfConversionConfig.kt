package com.example.data.model

enum class PageSizeOption(val label: String, val widthPt: Int, val heightPt: Int, val description: String) {
    A4("A4 Standard", 595, 842, "Standard 210 × 297 mm"),
    ORIGINAL("Match Real Image", 0, 0, "Adapts page size to exact image aspect ratio"),
    LETTER("Letter (US)", 612, 792, "Standard 8.5 × 11 in"),
    LEGAL("Legal", 612, 1008, "Standard 8.5 × 14 in")
}

enum class OrientationOption(val label: String, val description: String = "") {
    AUTO("Auto Detect", "Matches each photo's real orientation (Portrait/Landscape)"),
    PORTRAIT("Portrait", "Fixed vertical page orientation"),
    LANDSCAPE("Landscape", "Fixed horizontal page orientation")
}

enum class QualityOption(val label: String, val compressionQuality: Int, val description: String) {
    HIGH("High Quality", 100, "Maximum clarity, optimized size"),
    MEDIUM("Balanced (Fast)", 80, "Optimal clarity and compact size"),
    LOW("Compressed", 50, "Smallest file size for fast sharing")
}

enum class MarginOption(val label: String, val marginPt: Int) {
    NONE("No Margin", 0),
    SMALL("Small (12 pt)", 12),
    MEDIUM("Medium (24 pt)", 24),
    LARGE("Large (36 pt)", 36)
}

data class PdfConversionConfig(
    val fileName: String = "",
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val orientation: OrientationOption = OrientationOption.AUTO,
    val quality: QualityOption = QualityOption.HIGH,
    val margin: MarginOption = MarginOption.SMALL,
    val storeInMongo: Boolean = true
)

