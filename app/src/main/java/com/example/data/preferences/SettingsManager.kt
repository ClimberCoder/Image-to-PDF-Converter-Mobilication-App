package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MarginOption
import com.example.data.model.MongoConnectionConfig
import com.example.data.model.OrientationOption
import com.example.data.model.PageSizeOption
import com.example.data.model.QualityOption
import com.example.data.model.UploadDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserSettings(
    val defaultPageSize: PageSizeOption = PageSizeOption.A4,
    val defaultOrientation: OrientationOption = OrientationOption.AUTO,
    val defaultQuality: QualityOption = QualityOption.HIGH,
    val defaultMargin: MarginOption = MarginOption.SMALL,
    val appTheme: AppThemeMode = AppThemeMode.DARK,
    val autoCloudSync: Boolean = false,
    val userEmail: String = "local.user@img2pdf.app",
    val cloudApiEndpoint: String = "",
    val uploadDestination: UploadDestination = UploadDestination.MONGODB,
    val mongoConfig: MongoConnectionConfig = MongoConnectionConfig(
        uri = "",
        databaseName = "pdf_vault",
        collectionName = "stored_pdfs",
        isConnected = false,
        clusterHost = "",
        lastPingMs = 0L,
        lastConnectedTimestamp = 0L
    )
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("image_to_pdf_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): UserSettings {
        val pageSizeStr = prefs.getString("default_page_size", PageSizeOption.A4.name) ?: PageSizeOption.A4.name
        val orientationStr = prefs.getString("default_orientation", OrientationOption.AUTO.name) ?: OrientationOption.AUTO.name
        val qualityStr = prefs.getString("default_quality", QualityOption.HIGH.name) ?: QualityOption.HIGH.name
        val marginStr = prefs.getString("default_margin", MarginOption.SMALL.name) ?: MarginOption.SMALL.name
        val themeStr = prefs.getString("app_theme", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val autoCloud = prefs.getBoolean("auto_cloud_sync", false)
        val userEmail = prefs.getString("user_email", "local.user@img2pdf.app") ?: "local.user@img2pdf.app"
        val destinationStr = prefs.getString("upload_destination", UploadDestination.MONGODB.name) ?: UploadDestination.MONGODB.name
        
        val mongoUri = prefs.getString("mongo_uri", "") ?: ""
        val mongoDb = prefs.getString("mongo_db", "pdf_vault") ?: "pdf_vault"
        val mongoCol = prefs.getString("mongo_col", "stored_pdfs") ?: "stored_pdfs"
        val mongoConnected = prefs.getBoolean("mongo_connected", false)
        val mongoCluster = prefs.getString("mongo_cluster", "") ?: ""
        val mongoPing = prefs.getLong("mongo_ping", 0L)
        val mongoLastConn = prefs.getLong("mongo_last_conn", 0L)

        return UserSettings(
            defaultPageSize = runCatching { PageSizeOption.valueOf(pageSizeStr) }.getOrDefault(PageSizeOption.A4),
            defaultOrientation = runCatching { OrientationOption.valueOf(orientationStr) }.getOrDefault(OrientationOption.AUTO),
            defaultQuality = runCatching { QualityOption.valueOf(qualityStr) }.getOrDefault(QualityOption.HIGH),
            defaultMargin = runCatching { MarginOption.valueOf(marginStr) }.getOrDefault(MarginOption.SMALL),
            appTheme = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.DARK),
            autoCloudSync = autoCloud,
            userEmail = userEmail,
            uploadDestination = runCatching { UploadDestination.valueOf(destinationStr) }.getOrDefault(UploadDestination.MONGODB),
            mongoConfig = MongoConnectionConfig(
                uri = mongoUri,
                databaseName = mongoDb,
                collectionName = mongoCol,
                isConnected = mongoConnected,
                clusterHost = mongoCluster,
                lastPingMs = mongoPing,
                lastConnectedTimestamp = mongoLastConn
            )
        )
    }

    fun updateDefaultPageSize(pageSize: PageSizeOption) {
        prefs.edit().putString("default_page_size", pageSize.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultPageSize = pageSize)
    }

    fun updateDefaultOrientation(orientation: OrientationOption) {
        prefs.edit().putString("default_orientation", orientation.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultOrientation = orientation)
    }

    fun updateDefaultQuality(quality: QualityOption) {
        prefs.edit().putString("default_quality", quality.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultQuality = quality)
    }

    fun updateDefaultMargin(margin: MarginOption) {
        prefs.edit().putString("default_margin", margin.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultMargin = margin)
    }

    fun updateAppTheme(theme: AppThemeMode) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(appTheme = theme)
    }

    fun updateAutoCloudSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_cloud_sync", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoCloudSync = enabled)
    }

    fun updateUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
        _settingsFlow.value = _settingsFlow.value.copy(userEmail = email)
    }

    fun updateUploadDestination(destination: UploadDestination) {
        prefs.edit().putString("upload_destination", destination.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(uploadDestination = destination)
    }

    fun updateMongoConfig(config: MongoConnectionConfig) {
        prefs.edit()
            .putString("mongo_uri", config.uri)
            .putString("mongo_db", config.databaseName)
            .putString("mongo_col", config.collectionName)
            .putBoolean("mongo_connected", config.isConnected)
            .putString("mongo_cluster", config.clusterHost)
            .putLong("mongo_ping", config.lastPingMs)
            .putLong("mongo_last_conn", config.lastConnectedTimestamp)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(mongoConfig = config)
    }

}



