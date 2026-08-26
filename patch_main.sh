sed -i '409i \
    if (conversionState.showSuccessDialog && conversionState.lastCreatedPdf != null) {\
        com.example.ui.components.SuccessConversionBottomSheet(\
            pdf = conversionState.lastCreatedPdf!!,\
            onDismiss = { viewModel.dismissSuccessDialog() },\
            onView = { viewModel.previewPdf(conversionState.lastCreatedPdf!!) },\
            onShare = { viewModel.sharePdf(context, conversionState.lastCreatedPdf!!) },\
            onOpenWith = { viewModel.openPdf(context, conversionState.lastCreatedPdf!!) }\
        )\
    }' app/src/main/java/com/example/MainActivity.kt
