# First, remove the code that was incorrectly appended
sed -i '/fun handleExternalIntent(intent: Intent, context: Context)/,$d' app/src/main/java/com/example/ui/viewmodel/PdfViewModel.kt

# Now insert it before the last closing brace
sed -i '$d' app/src/main/java/com/example/ui/viewmodel/PdfViewModel.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/viewmodel/PdfViewModel.kt
    fun handleExternalIntent(intent: Intent, context: Context) {
        viewModelScope.launch {
            try {
                if (intent.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("image/") == true) {
                    val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uris != null) {
                        addImageUris(uris)
                    }
                } else if (intent.action == Intent.ACTION_SEND) {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uri != null) {
                        if (intent.type?.startsWith("image/") == true) {
                            addImageUris(listOf(uri))
                        } else if (intent.type == "application/pdf") {
                            importAndPreviewPdf(uri, context)
                        }
                    }
                } else if (intent.action == Intent.ACTION_VIEW) {
                    val uri = intent.data
                    if (uri != null && (intent.type == "application/pdf" || intent.type == "application/octet-stream")) {
                        importAndPreviewPdf(uri, context)
                    }
                }
            } catch (e: Exception) {
                showMessage("Failed to open file: ${e.message}")
            }
        }
    }

    private suspend fun importAndPreviewPdf(uri: Uri, context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "imported_pdfs").apply { if (!exists()) mkdirs() }
            val fileName = getFileNameFromUri(uri, context) ?: "Imported_Document_${System.currentTimeMillis()}.pdf"
            val localFile = File(cacheDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (localFile.exists()) {
                val entity = PdfDocumentEntity(
                    fileName = fileName,
                    filePath = localFile.absolutePath,
                    fileSizeBytes = localFile.length(),
                    pageCount = 0 // Will be updated when rendered
                )
                // Add to DB
                val id = repository.insertPdf(entity)
                val savedEntity = entity.copy(id = id)
                previewPdf(savedEntity)
            }
        } catch (e: Exception) {
            showMessage("Error importing PDF: ${e.message}")
        }
    }

    private fun getFileNameFromUri(uri: Uri, context: Context): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path?.let { File(it).name }
        }
        return result
    }
}
INNER_EOF
