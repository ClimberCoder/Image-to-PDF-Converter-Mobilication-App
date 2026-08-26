# Remove the snackbarHost line from Scaffold
sed -i '/snackbarHost = { SnackbarHost(hostState = snackbarHostState)/d' app/src/main/java/com/example/MainActivity.kt

# Inject the SnackbarHost after AppleFloatingTaskbar inside the Box block
sed -i '/AppleFloatingTaskbar(/i \        // Top-Aligned Dynamic Island Snackbar\n        SnackbarHost(\n            hostState = snackbarHostState,\n            modifier = Modifier\n                .align(Alignment.TopCenter)\n                .padding(top = 16.dp)\n                .statusBarsPadding()\n        ) { data ->\n            com.example.ui.components.CustomAnimatedSnackbar(data)\n        }\n' app/src/main/java/com/example/MainActivity.kt

