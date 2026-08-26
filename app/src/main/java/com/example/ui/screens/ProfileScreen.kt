package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import com.example.data.model.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    settings: UserSettings,
    totalPdfsGenerated: Int,
    onUpdatePageSize: (PageSizeOption) -> Unit,
    onUpdateOrientation: (OrientationOption) -> Unit,
    onUpdateQuality: (QualityOption) -> Unit,
    onUpdateMargin: (MarginOption) -> Unit,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onClearCache: () -> Unit
) {
    val deviceName = remember { "${Build.MANUFACTURER} ${Build.MODEL}".uppercase() }
    var memoryUsedMB by remember { mutableIntStateOf(0) }
    
    // Live memory monitor animation
    LaunchedEffect(Unit) {
        while(true) {
            val runtime = Runtime.getRuntime()
            memoryUsedMB = ((runtime.totalMemory() - runtime.freeMemory()) / 1048576L).toInt()
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp, top = 24.dp)
    ) {
        item {
            // Stats Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Name (Full width)
                AnimatedStatCard(
                    title = "Device",
                    value = deviceName,
                    icon = Icons.Default.PhoneAndroid,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // PDFs Created and Memory Used (Side by side)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedStatCard(
                        title = "PDFs Created",
                        value = totalPdfsGenerated.toString(),
                        icon = Icons.Default.PictureAsPdf,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedStatCard(
                        title = "Memory Used",
                        value = "${memoryUsedMB} MB",
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("PDF Generation Defaults")
        }

        item {
            SettingsCard {
                SettingsDropdownGroup(
                    title = "Page Size",
                    options = PageSizeOption.entries.map { it.label },
                    selected = settings.defaultPageSize.label,
                    onSelect = { label -> 
                        PageSizeOption.entries.find { it.label == label }?.let { onUpdatePageSize(it) }
                    }
                )
                SettingsDivider()
                SettingsDropdownGroup(
                    title = "Orientation",
                    options = OrientationOption.entries.map { it.label },
                    selected = settings.defaultOrientation.label,
                    onSelect = { label -> 
                        OrientationOption.entries.find { it.label == label }?.let { onUpdateOrientation(it) }
                    }
                )
                SettingsDivider()
                SettingsDropdownGroup(
                    title = "Quality",
                    options = QualityOption.entries.map { it.label },
                    selected = settings.defaultQuality.label,
                    onSelect = { label -> 
                        QualityOption.entries.find { it.label == label }?.let { onUpdateQuality(it) }
                    }
                )
                SettingsDivider()
                SettingsDropdownGroup(
                    title = "Margin",
                    options = MarginOption.entries.map { it.label },
                    selected = settings.defaultMargin.label,
                    onSelect = { label -> 
                        MarginOption.entries.find { it.label == label }?.let { onUpdateMargin(it) }
                    }
                )
            }
        }

        item {
            SectionTitle("App Settings")
        }

        item {
            SettingsCard {
                SettingsDropdownGroup(
                    title = "Appearance",
                    options = AppThemeMode.entries.map { it.label },
                    selected = settings.appTheme.label,
                    onSelect = { label -> 
                        AppThemeMode.entries.find { it.label == label }?.let { onUpdateTheme(it) }
                    }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "Clear Cache",
                    subtitle = "Free up device storage",
                    icon = Icons.Default.DeleteOutline,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onClearCache
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Created by Vansh Aggarwal(the genz)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "phone no: 8287453009",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stat_scale"
    )

    Surface(
        modifier = modifier.scale(if (title == "Memory Used") scale else 1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
