# Image-to-PDF-Converter-Mobile-App

An Android app for converting images to PDF files and viewing PDFs, with local file management and optional cloud storage configuration.

## Install

1. Open the repository's [Releases](https://github.com/ClimberCoder/Image-to-PDF-Converter-Mobile-App/releases) page on an Android phone.
2. Download the latest `app-release.apk` from the newest release.
3. Open the downloaded APK and allow installation from that source when Android asks.

The APK is built by GitHub Actions as a signed release build. Android 7.0 (API 24) or newer is required.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio and select the repository directory.
2. Allow Android Studio to sync the project.
3. Create a local `.env` file and set `GEMINI_API_KEY` if AI features are needed (see `.env.example`).
4. Run the `app` configuration on an emulator or Android device.