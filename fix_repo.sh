sed -i '/suspend fun scanDeviceAndSdCardPdfs/i \
    suspend fun insertPdf(pdf: PdfDocumentEntity): Long = pdfDao.insertPdf(pdf)' app/src/main/java/com/example/data/repository/PdfRepository.kt
