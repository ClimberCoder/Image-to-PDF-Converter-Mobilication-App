sed -i '/override fun onCreate/i \
    override fun onNewIntent(intent: android.content.Intent) {\
        super.onNewIntent(intent)\
        setIntent(intent)\
    }\
' app/src/main/java/com/example/MainActivity.kt
