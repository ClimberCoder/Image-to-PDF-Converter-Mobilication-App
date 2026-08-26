package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerSheet(
    logicalPages: List<Int>,
    bookmarks: List<Int>,
    onUpdateLogicalPages: (List<Int>) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    loadThumbnail: suspend (Int) -> Bitmap?,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.8f)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Pages") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bookmarks") })
            }
            
            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(logicalPages.size) { logicalIndex ->
                        val originalIndex = logicalPages[logicalIndex]
                        var thumbnail by remember(originalIndex) { mutableStateOf<Bitmap?>(null) }
                        
                        LaunchedEffect(originalIndex) {
                            thumbnail = loadThumbnail(originalIndex)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.7f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { 
                                        onNavigateToPage(logicalIndex)
                                        onDismiss()
                                    }
                            ) {
                                if (thumbnail != null) {
                                    Image(
                                        bitmap = thumbnail!!.asImageBitmap(),
                                        contentDescription = "Page $logicalIndex",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (bookmarks.contains(originalIndex)) {
                                    Box(modifier = Modifier.padding(4.dp).align(Alignment.TopEnd).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(4.dp)) {
                                        Text("★", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Page ${logicalIndex + 1}", style = MaterialTheme.typography.labelSmall)
                            Row {
                                IconButton(onClick = {
                                    val newPages = logicalPages.toMutableList()
                                    newPages.removeAt(logicalIndex)
                                    onUpdateLogicalPages(newPages)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = {
                                    val newPages = logicalPages.toMutableList()
                                    newPages.add(logicalIndex + 1, originalIndex)
                                    onUpdateLogicalPages(newPages)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.FileCopy, "Duplicate", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bookmarks) { originalIndex ->
                        val logicalIndex = logicalPages.indexOf(originalIndex)
                        if (logicalIndex != -1) {
                            var thumbnail by remember(originalIndex) { mutableStateOf<Bitmap?>(null) }
                            
                            LaunchedEffect(originalIndex) {
                                thumbnail = loadThumbnail(originalIndex)
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.7f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .clickable { 
                                            onNavigateToPage(logicalIndex)
                                            onDismiss()
                                        }
                                ) {
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail!!.asImageBitmap(),
                                            contentDescription = "Bookmark",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(modifier = Modifier.padding(4.dp).align(Alignment.TopEnd).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(4.dp)) {
                                        Text("★", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Page ${logicalIndex + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
