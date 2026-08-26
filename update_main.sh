sed -i '/val viewModel: PdfViewModel = viewModel()/a \
\
            LaunchedEffect(intent) {\
                if (intent.action == android.content.Intent.ACTION_VIEW || \
                    intent.action == android.content.Intent.ACTION_SEND || \
                    intent.action == android.content.Intent.ACTION_SEND_MULTIPLE) {\
                    viewModel.handleExternalIntent(intent, this@MainActivity)\
                    isSplashVisible = false\
                }\
            }' app/src/main/java/com/example/MainActivity.kt
