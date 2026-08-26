sed -i '/<category android:name="android.intent.category.LAUNCHER" \/>/a \
            </intent-filter>\
            <intent-filter>\
                <action android:name="android.intent.action.VIEW" />\
                <category android:name="android.intent.category.DEFAULT" />\
                <category android:name="android.intent.category.BROWSABLE" />\
                <data android:mimeType="application/pdf" />\
                <data android:mimeType="application/octet-stream" />\
            </intent-filter>\
            <intent-filter>\
                <action android:name="android.intent.action.SEND" />\
                <category android:name="android.intent.category.DEFAULT" />\
                <data android:mimeType="application/pdf" />\
                <data android:mimeType="image/*" />\
            </intent-filter>\
            <intent-filter>\
                <action android:name="android.intent.action.SEND_MULTIPLE" />\
                <category android:name="android.intent.category.DEFAULT" />\
                <data android:mimeType="image/*" />' app/src/main/AndroidManifest.xml
