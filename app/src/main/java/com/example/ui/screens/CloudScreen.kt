package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudPdfMetadata
import com.example.data.model.CloudStorageQuota
import com.example.data.model.MongoCollectionInfo
import com.example.data.model.MongoConnectionConfig
import com.example.data.model.MongoDatabaseInfo
import com.example.data.model.PdfDocumentEntity
import com.example.data.model.UploadDestination
import com.example.data.preferences.UserSettings
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudScreen(
    cloudFiles: List<CloudPdfMetadata>,
    localPdfs: List<PdfDocumentEntity>,
    quota: CloudStorageQuota,
    uploadProgressMap: Map<Long, Float>,
    userSettings: UserSettings,
    isConnectingMongo: Boolean = false,
    mongoDatabases: List<MongoDatabaseInfo> = emptyList(),
    mongoCollections: List<MongoCollectionInfo> = emptyList(),
    selectedDbFilter: String? = null,
    selectedColFilter: String? = null,
    onSelectDbFilter: (String?) -> Unit = {},
    onSelectColFilter: (String?) -> Unit = {},
    onCreateDatabase: (dbName: String, initialCol: String) -> Unit = { _, _ -> },
    onCreateCollection: (dbName: String, colName: String) -> Unit = { _, _ -> },
    onSelectActiveDatabaseAndCollection: (dbName: String, colName: String) -> Unit = { _, _ -> },
    onUploadLocalPdf: (PdfDocumentEntity) -> Unit,
    onDeleteCloudFile: (String) -> Unit,
    onRefresh: () -> Unit,
    onConnectMongo: (uri: String, dbName: String, colName: String) -> Unit,
    onDisconnectMongo: () -> Unit,
    onSelectDestination: (UploadDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showCreateDbDialog by remember { mutableStateOf(false) }
    var showCreateColDialog by remember { mutableStateOf(false) }
    var selectedDocForDetails by remember { mutableStateOf<CloudPdfMetadata?>(null) }
    var deleteCandidateId by remember { mutableStateOf<String?>(null) }

    val activeDestination = userSettings.uploadDestination
    val mongoConfig = userSettings.mongoConfig

    val filteredCloudFiles = cloudFiles.filter { doc ->
        (selectedDbFilter == null || doc.mongoDatabase == selectedDbFilter) &&
                (selectedColFilter == null || doc.mongoCollection == selectedColFilter)
    }

    val unsyncedLocalPdfs = localPdfs.filter { it.cloudSyncStatus != PdfDocumentEntity.SYNC_STATUS_SYNCED }
    val syncedLocalPdfs = localPdfs.filter { it.cloudSyncStatus == PdfDocumentEntity.SYNC_STATUS_SYNCED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cloud_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Storage Destination Chooser Card (MongoDB vs App Cloud Vault)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("destination_selector_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PDF Cloud Storage Target",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select database engine for storing documents",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // MongoDB Option
                        DestinationOptionCard(
                            title = "MongoDB Vault",
                            subtitle = if (mongoConfig.isConnected) "● Connected (${mongoConfig.clusterHost})" else "○ Disconnected (URI needed)",
                            icon = Icons.Default.Storage,
                            isSelected = activeDestination == UploadDestination.MONGODB,
                            isConnected = mongoConfig.isConnected,
                            onClick = { onSelectDestination(UploadDestination.MONGODB) },
                            modifier = Modifier.weight(1f).testTag("destination_mongodb")
                        )

                        // Cloud Vault Option
                        DestinationOptionCard(
                            title = "App Cloud Vault",
                            subtitle = "Default Encrypted Vault",
                            icon = Icons.Default.CloudQueue,
                            isSelected = activeDestination == UploadDestination.APP_VAULT,
                            isConnected = true,
                            onClick = { onSelectDestination(UploadDestination.APP_VAULT) },
                            modifier = Modifier.weight(1f).testTag("destination_app_vault")
                        )
                    }
                }
            }
        }

        // 2. MongoDB Connection Status / Dynamic DB & Collection Manager (Shown when MongoDB is active)
        if (activeDestination == UploadDestination.MONGODB) {
            item {
                if (mongoConfig.isConnected) {
                    // Connected Status Banner with Dynamic DB & Collection Manager
                    MongoConnectedCard(
                        config = mongoConfig,
                        databases = mongoDatabases,
                        collections = mongoCollections,
                        onDisconnect = onDisconnectMongo,
                        onEditUri = { showConnectDialog = true },
                        onCreateDatabaseClick = { showCreateDbDialog = true },
                        onCreateCollectionClick = { showCreateColDialog = true },
                        onSelectActive = { db, col -> onSelectActiveDatabaseAndCollection(db, col) }
                    )
                } else {
                    // Not Connected - Form to enter URI
                    MongoDisconnectedSetupCard(
                        currentConfig = mongoConfig,
                        isConnecting = isConnectingMongo,
                        onConnect = { uri, db, col ->
                            onConnectMongo(uri, db, col)
                        }
                    )
                }
            }
        } else {
            // App Cloud Vault Quota Banner
            item {
                AppVaultQuotaCard(quota = quota, filesCount = cloudFiles.size)
            }
        }

        // 3. Tab Selector: (Stored PDFs vs Unsynced Local PDFs)
        item {
            val destinationTitle = if (activeDestination == UploadDestination.MONGODB) "MongoDB Stored PDFs" else "Cloud Vault Backups"
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .fillMaxWidth()
                    .testTag("cloud_tab_row")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "$destinationTitle (${filteredCloudFiles.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_stored_files")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Unsynced Local (${unsyncedLocalPdfs.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_unsynced_local")
                )
            }
        }

        // 4. Tab 0: MongoDB Stored PDFs (Showing file names, created date, time, and day, plus DB & Collection tags)
        if (selectedTab == 0) {
            // Database Filter Chips (When MongoDB is active and connected)
            if (activeDestination == UploadDestination.MONGODB && mongoDatabases.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Filter by Database:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedDbFilter == null,
                                onClick = {
                                    onSelectDbFilter(null)
                                    onSelectColFilter(null)
                                },
                                label = { Text("All Databases (${cloudFiles.size})") }
                            )
                            mongoDatabases.forEach { dbInfo ->
                                val isSelected = selectedDbFilter == dbInfo.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            onSelectDbFilter(null)
                                            onSelectColFilter(null)
                                        } else {
                                            onSelectDbFilter(dbInfo.name)
                                            onSelectColFilter(null)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("${dbInfo.name} (${dbInfo.documentCount})") }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredCloudFiles.isEmpty()) {
                item {
                    EmptyVaultCard(
                        destination = activeDestination,
                        isConnected = if (activeDestination == UploadDestination.MONGODB) mongoConfig.isConnected else true,
                        onConnectClick = { showConnectDialog = true }
                    )
                }
            } else {
                item {
                    Text(
                        text = if (activeDestination == UploadDestination.MONGODB) {
                            if (selectedDbFilter != null) "Showing PDF documents in database '$selectedDbFilter'"
                            else "Showing all PDF documents stored across MongoDB databases"
                        } else {
                            "Showing all PDF documents backed up in Cloud Vault"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                items(filteredCloudFiles, key = { it.fileId }) { cloudDoc ->
                    MongoPdfItemCard(
                        cloudDoc = cloudDoc,
                        onViewDetails = { selectedDocForDetails = cloudDoc },
                        onDelete = { deleteCandidateId = cloudDoc.fileId }
                    )
                }
            }
        }

        // 5. Tab 1: Unsynced Local PDFs (Ready to Upload to MongoDB / Cloud)
        if (selectedTab == 1) {
            if (unsyncedLocalPdfs.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "All Local PDFs Synced!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (activeDestination == UploadDestination.MONGODB) {
                                    "Every local PDF is uploaded to your MongoDB database."
                                } else {
                                    "Every local PDF is backed up to Cloud Vault."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = if (activeDestination == UploadDestination.MONGODB) {
                            "Select any local PDF to upload into MongoDB (${mongoConfig.databaseName}.${mongoConfig.collectionName})"
                        } else {
                            "Select any local PDF to backup into Cloud Vault"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                items(unsyncedLocalPdfs, key = { it.id }) { localPdf ->
                    val progress = uploadProgressMap[localPdf.id]
                    UnsyncedLocalPdfItemCard(
                        pdf = localPdf,
                        destination = activeDestination,
                        isMongoConnected = mongoConfig.isConnected,
                        uploadProgress = progress,
                        onUpload = {
                            if (activeDestination == UploadDestination.MONGODB && !mongoConfig.isConnected) {
                                showConnectDialog = true
                            } else {
                                onUploadLocalPdf(localPdf)
                            }
                        }
                    )
                }
            }
        }
    }

    // Connect MongoDB Modal Dialog
    if (showConnectDialog) {
        MongoConnectDialog(
            initialConfig = mongoConfig,
            isConnecting = isConnectingMongo,
            onDismiss = { showConnectDialog = false },
            onConnect = { uri, db, col ->
                onConnectMongo(uri, db, col)
                showConnectDialog = false
            }
        )
    }

    // Create Database Dialog
    if (showCreateDbDialog) {
        CreateMongoDatabaseDialog(
            onDismiss = { showCreateDbDialog = false },
            onCreate = { dbName, initialCol ->
                onCreateDatabase(dbName, initialCol)
                showCreateDbDialog = false
            }
        )
    }

    // Create Collection Dialog
    if (showCreateColDialog) {
        CreateMongoCollectionDialog(
            currentDatabase = mongoConfig.databaseName,
            availableDatabases = mongoDatabases.map { it.name },
            onDismiss = { showCreateColDialog = false },
            onCreate = { dbName, colName ->
                onCreateCollection(dbName, colName)
                showCreateColDialog = false
            }
        )
    }

    // Document Details Dialog (Shows created day, date, time, ObjectId, host, Auto-Provision details)
    selectedDocForDetails?.let { doc ->
        MongoDocDetailsDialog(
            doc = doc,
            onDismiss = { selectedDocForDetails = null }
        )
    }

    // Delete Confirmation Dialog
    deleteCandidateId?.let { fileId ->
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to remove this document from ${activeDestination.label}? Local device files will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCloudFile(fileId)
                        deleteCandidateId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Destination Selector Option Tile
@Composable
private fun DestinationOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isConnected && isSelected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Connected MongoDB Status Banner with Auto-Provisioning Info & Database Management
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MongoConnectedCard(
    config: MongoConnectionConfig,
    databases: List<MongoDatabaseInfo>,
    collections: List<MongoCollectionInfo>,
    onDisconnect: () -> Unit,
    onEditUri: () -> Unit,
    onCreateDatabaseClick: () -> Unit,
    onCreateCollectionClick: () -> Unit,
    onSelectActive: (dbName: String, colName: String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mongo_connected_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Connected Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing green dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected to MongoDB",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${config.lastPingMs}ms Ping",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Auto-Provisioning Notice Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-Provision Active: If database or collection does not exist in MongoDB, it is created automatically on-the-fly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Target Database & Collection Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cluster Host:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = config.clusterHost,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Active Target:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${config.databaseName}.${config.collectionName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Database and Collection Quick Management Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateDatabaseClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("mongo_btn_new_db")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ New DB", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onCreateCollectionClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("mongo_btn_new_col")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Collection", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons (Edit URI / Disconnect)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditUri,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("mongo_edit_uri_button")
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit URI")
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("mongo_disconnect_button")
                ) {
                    Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect")
                }
            }
        }
    }
}

// Disconnected Setup Form Card (Takes MongoDB URI)
@Composable
private fun MongoDisconnectedSetupCard(
    currentConfig: MongoConnectionConfig,
    isConnecting: Boolean,
    onConnect: (uri: String, database: String, collection: String) -> Unit
) {
    var uriInput by remember {
        mutableStateOf(
            if (currentConfig.uri.isNotBlank()) currentConfig.uri
            else ""
        )
    }
    var dbNameInput by remember { mutableStateOf(currentConfig.databaseName) }
    var colNameInput by remember { mutableStateOf(currentConfig.collectionName) }
    var showAdvanced by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mongo_setup_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Connect MongoDB URI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter connection string to store PDFs in MongoDB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto creation badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SuccessGreen.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "If database or collection does not exist, MongoDB creates it on first write.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Error notice if previous attempt failed
            currentConfig.connectionError?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // MongoDB URI input field
            OutlinedTextField(
                value = uriInput,
                onValueChange = { uriInput = it },
                label = { Text("MongoDB Connection URI") },
                placeholder = { Text("mongodb+srv://user:pass@cluster0.mongodb.net/dbname") },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mongo_uri_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        uriInput = "mongodb+srv://user:password@cluster.mongodb.net/pdf_vault"
                    },
                    label = { Text("Atlas Cluster", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        uriInput = "mongodb://localhost:27017/pdf_vault"
                    },
                    label = { Text("Local Mongo", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Database & Collection settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Database & Collection Settings",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = showAdvanced) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dbNameInput,
                            onValueChange = { dbNameInput = it },
                            label = { Text("Database") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("mongo_db_input")
                        )
                        OutlinedTextField(
                            value = colNameInput,
                            onValueChange = { colNameInput = it },
                            label = { Text("Collection") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("mongo_col_input")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connect Button
            Button(
                onClick = { onConnect(uriInput, dbNameInput, colNameInput) },
                enabled = !isConnecting && uriInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("connect_mongo_button")
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting & Testing Ping...")
                } else {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect to MongoDB", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Create Mongo Database Dialog
@Composable
private fun CreateMongoDatabaseDialog(
    onDismiss: () -> Unit,
    onCreate: (databaseName: String, initialCollection: String) -> Unit
) {
    var dbName by remember { mutableStateOf("") }
    var colName by remember { mutableStateOf("stored_pdfs") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create MongoDB Database")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a name for the new database. If it doesn't already exist on your cluster, it will be automatically provisioned.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = dbName,
                    onValueChange = { dbName = it },
                    label = { Text("Database Name") },
                    placeholder = { Text("e.g. accounting_docs") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_new_db_name")
                )
                OutlinedTextField(
                    value = colName,
                    onValueChange = { colName = it },
                    label = { Text("Initial Collection Name") },
                    placeholder = { Text("e.g. invoices") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_new_db_col_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(dbName, colName) },
                enabled = dbName.isNotBlank()
            ) {
                Text("Create Database")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Create Mongo Collection Dialog
@Composable
private fun CreateMongoCollectionDialog(
    currentDatabase: String,
    availableDatabases: List<String>,
    onDismiss: () -> Unit,
    onCreate: (databaseName: String, collectionName: String) -> Unit
) {
    var selectedDb by remember { mutableStateOf(currentDatabase) }
    var colName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Collection")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add a new collection to your target MongoDB database.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = selectedDb,
                    onValueChange = { selectedDb = it },
                    label = { Text("Target Database") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colName,
                    onValueChange = { colName = it },
                    label = { Text("New Collection Name") },
                    placeholder = { Text("e.g. receipts_2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_new_col_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedDb, colName) },
                enabled = colName.isNotBlank() && selectedDb.isNotBlank()
            ) {
                Text("Create Collection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// App Vault Quota Card
@Composable
private fun AppVaultQuotaCard(
    quota: CloudStorageQuota,
    filesCount: Int
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "App Cloud Vault Storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${quota.formattedUsed} / ${quota.formattedTotal}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { quota.usagePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Encrypted Rest Storage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$filesCount Files",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// MongoDB Stored PDF Item Card displaying file names, Created Date, Time, Day, and Auto-Creation badge!
@Composable
private fun MongoPdfItemCard(
    cloudDoc: CloudPdfMetadata,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mongo_pdf_item_${cloudDoc.fileId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon + File Name + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (cloudDoc.destination == UploadDestination.MONGODB) SuccessGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (cloudDoc.destination == UploadDestination.MONGODB) Icons.Default.Storage else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = if (cloudDoc.destination == UploadDestination.MONGODB) SuccessGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cloudDoc.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${cloudDoc.pageCount} pgs",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = cloudDoc.formattedSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Date, Time, and Day of Week Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day & Date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${cloudDoc.dayOfWeek}, ${cloudDoc.createdDateFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cloudDoc.createdTimeFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // MongoDB Database & Collection Target Info
            if (cloudDoc.destination == UploadDestination.MONGODB) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Collection: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${cloudDoc.mongoDatabase}.${cloudDoc.mongoCollection}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (cloudDoc.wasDatabaseCreatedOnFly || cloudDoc.wasCollectionCreatedOnFly) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-Created",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("PDF Metadata", cloudDoc.cloudStorageUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy URI", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details", fontSize = 11.sp)
                }
            }
        }
    }
}

// Unsynced Local PDF Item Card
@Composable
private fun UnsyncedLocalPdfItemCard(
    pdf: PdfDocumentEntity,
    destination: UploadDestination,
    isMongoConnected: Boolean,
    uploadProgress: Float?,
    onUpload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdf.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${pdf.pageCount} pages • ${pdf.formattedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uploadProgress != null) {
                    FilledTonalButton(onClick = {}, enabled = false) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uploading", fontSize = 12.sp)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onUpload,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("upload_pdf_${pdf.id}")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (destination == UploadDestination.MONGODB) "Upload to Mongo" else "Upload",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (uploadProgress != null && uploadProgress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

// Empty state placeholder
@Composable
private fun EmptyVaultCard(
    destination: UploadDestination,
    isConnected: Boolean,
    onConnectClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (destination == UploadDestination.MONGODB) Icons.Default.Storage else Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (destination == UploadDestination.MONGODB) "No MongoDB Documents Stored" else "No Cloud Backups Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (destination == UploadDestination.MONGODB) {
                    if (isConnected) "Upload unsynced local PDFs to save them in your connected MongoDB cluster."
                    else "Connect your MongoDB URI above to view and upload stored PDFs."
                } else {
                    "Upload local PDFs to back them up securely in your cloud vault."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (destination == UploadDestination.MONGODB && !isConnected) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect MongoDB URI")
                }
            }
        }
    }
}

// MongoDB Connection Dialog
@Composable
private fun MongoConnectDialog(
    initialConfig: MongoConnectionConfig,
    isConnecting: Boolean,
    onDismiss: () -> Unit,
    onConnect: (uri: String, db: String, col: String) -> Unit
) {
    var uri by remember { mutableStateOf(initialConfig.uri) }
    var db by remember { mutableStateOf(initialConfig.databaseName) }
    var col by remember { mutableStateOf(initialConfig.collectionName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MongoDB Connection")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Provide your MongoDB connection URI. If the target database or collection does not exist, it will be automatically created upon saving documents:",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = uri,
                    onValueChange = { uri = it },
                    label = { Text("MongoDB URI") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = db,
                    onValueChange = { db = it },
                    label = { Text("Database Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = col,
                    onValueChange = { col = it },
                    label = { Text("Collection Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(uri, db, col) },
                enabled = !isConnecting && uri.isNotBlank()
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// MongoDB Document Details Dialog
@Composable
private fun MongoDocDetailsDialog(
    doc: CloudPdfMetadata,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Document Info", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = doc.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                HorizontalDivider()

                DocDetailRow(label = "Day of Week", value = doc.dayOfWeek)
                DocDetailRow(label = "Created Date", value = doc.createdDateFormatted)
                DocDetailRow(label = "Created Time", value = doc.createdTimeFormatted)
                DocDetailRow(label = "Full Timestamp", value = doc.fullCreatedDayDateTime)
                DocDetailRow(label = "File Size", value = doc.formattedSize)
                DocDetailRow(label = "Page Count", value = "${doc.pageCount} pages")
                DocDetailRow(label = "Database", value = doc.mongoDatabase)
                DocDetailRow(label = "Collection", value = doc.mongoCollection)
                DocDetailRow(label = "MongoDB ObjectId", value = doc.mongoObjectId)

                if (doc.wasDatabaseCreatedOnFly || doc.wasCollectionCreatedOnFly) {
                    DocDetailRow(label = "Dynamic Provision", value = "Auto-Created on Fly")
                }
                if (doc.mongoResolutionMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = doc.mongoResolutionMessage,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Document Details", doc.fullCreatedDayDateTime + "\n" + doc.mongoObjectId + "\n" + doc.cloudStorageUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Details copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Info")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DocDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
