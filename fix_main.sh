sed -i '/var isSplashVisible by remember { mutableStateOf(true) }/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/LaunchedEffect(intent) {/i \            var isSplashVisible by remember { mutableStateOf(true) }' app/src/main/java/com/example/MainActivity.kt
