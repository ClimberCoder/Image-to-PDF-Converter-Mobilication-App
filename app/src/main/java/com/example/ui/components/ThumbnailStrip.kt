package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ThumbnailStrip(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    loadThumbnail: suspend (Int) -> Bitmap?
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, currentPage - 2))
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(currentPage) {
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(maxOf(0, currentPage - 2))
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2C).copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(pageCount, key = { it }) { index ->
                ThumbnailItem(
                    index = index,
                    isSelected = index == currentPage,
                    loadThumbnail = loadThumbnail,
                    onClick = { onPageSelected(index) }
                )
            }
        }
    }
}

@Composable
fun ThumbnailItem(
    index: Int,
    isSelected: Boolean,
    loadThumbnail: suspend (Int) -> Bitmap?,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(index) {
        if (bitmap == null) {
            bitmap = loadThumbnail(index)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .height(80.dp)
                .width(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Thumbnail for page ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${index + 1}",
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}
