package com.example.ui.downloads

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.detector.MediaDetector
import com.example.data.local.entity.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaType
import com.example.ui.MainViewModel
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.Success
import com.example.ui.theme.Warning

@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val progressMap by viewModel.downloadProgress.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Completed", "Failed")

    val filteredList = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            "Active" -> downloads.filter { it.status == DownloadStatus.DOWNLOADING.name || it.status == DownloadStatus.QUEUED.name || it.status == DownloadStatus.PAUSED.name }
            "Completed" -> downloads.filter { it.status == DownloadStatus.COMPLETED.name }
            "Failed" -> downloads.filter { it.status == DownloadStatus.FAILED.name || it.status == DownloadStatus.CANCELLED.name }
            else -> downloads
        }
    }

    var itemToDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Downloads",
                    color = colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${downloads.size} total items",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            if (downloads.isNotEmpty()) {
                IconButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.testTag("clear_all_downloads_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear all downloads",
                        tint = SecondaryText
                    )
                }
            }
        }

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryAccent,
                        selectedLabelColor = Color.White,
                        containerColor = colorScheme.surface,
                        labelColor = colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colorScheme.outline,
                        selectedBorderColor = PrimaryAccent,
                        enabled = true,
                        selected = selectedFilter == filter
                    )
                )
            }
        }

        // List of Downloads or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "No downloads",
                            tint = SecondaryText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "No $selectedFilter downloads",
                        color = colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Active and completed downloads will show up here.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    val liveProgress = progressMap[item.id]
                    DownloadCard(
                        entity = item,
                        liveProgress = liveProgress,
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.retryDownload(item.id) },
                        onCancel = { viewModel.cancelDownload(item.id) },
                        onRetry = { viewModel.retryDownload(item.id) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Delete item dialog
    itemToDelete?.let { dl ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Download?", color = colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete '${dl.fileName}'?", color = colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDownload(dl.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = SecondaryText)
                }
            },
            containerColor = colorScheme.surface
        )
    }

    // Clear all dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Downloads?", color = colorScheme.onSurface) },
            text = { Text("This will clear all download history records.", color = colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearDownloads()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            },
            containerColor = colorScheme.surface
        )
    }
}

@Composable
fun DownloadCard(
    entity: DownloadEntity,
    liveProgress: com.example.data.download.DownloadProgressInfo?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val currentStatus = liveProgress?.status?.name ?: entity.status
    val downloaded = liveProgress?.downloadedBytes ?: entity.downloadedBytes
    val total = if ((liveProgress?.totalBytes ?: 0L) > 0) liveProgress!!.totalBytes else entity.fileSizeBytes
    val speed = liveProgress?.speedBytesPerSec ?: 0L

    val progressPercent = if (total > 0) {
        ((downloaded.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("download_card_${entity.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Media Type Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (entity.mediaType) {
                        MediaType.VIDEO.name -> Icons.Default.Videocam
                        MediaType.IMAGE.name -> Icons.Default.Image
                        MediaType.GIF.name -> Icons.Default.Gif
                        MediaType.AUDIO.name -> Icons.Default.Audiotrack
                        MediaType.DOCUMENT.name -> Icons.Default.Description
                        else -> Icons.Default.Download
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = entity.mediaType,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.title,
                        color = colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entity.platform} • ${entity.fileName}",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                StatusBadge(status = currentStatus)
            }

            // Progress bar if active
            if (currentStatus == DownloadStatus.DOWNLOADING.name || currentStatus == DownloadStatus.PAUSED.name || currentStatus == DownloadStatus.QUEUED.name) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { if (total > 0) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (currentStatus == DownloadStatus.PAUSED.name) Warning else PrimaryAccent,
                        trackColor = colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${MediaDetector.formatBytes(downloaded)} / ${MediaDetector.formatBytes(total)} ($progressPercent%)",
                            color = colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                        if (speed > 0 && currentStatus == DownloadStatus.DOWNLOADING.name) {
                            Text(
                                text = "${MediaDetector.formatBytes(speed)}/s",
                                color = PrimaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (currentStatus == DownloadStatus.COMPLETED.name) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved • ${MediaDetector.formatBytes(entity.fileSizeBytes)}",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            } else if (currentStatus == DownloadStatus.FAILED.name) {
                Text(
                    text = entity.errorDetails ?: "Download interrupted",
                    color = ErrorColor,
                    fontSize = 11.sp
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (currentStatus) {
                    DownloadStatus.DOWNLOADING.name -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Warning, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    DownloadStatus.PAUSED.name, DownloadStatus.QUEUED.name -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    DownloadStatus.FAILED.name, DownloadStatus.CANCELLED.name -> {
                        IconButton(onClick = onRetry, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                    DownloadStatus.COMPLETED.name -> {
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        DownloadStatus.COMPLETED.name -> Triple(Success.copy(alpha = 0.15f), Success, "Completed")
        DownloadStatus.DOWNLOADING.name -> Triple(PrimaryAccent.copy(alpha = 0.15f), PrimaryAccent, "Downloading")
        DownloadStatus.PAUSED.name -> Triple(Warning.copy(alpha = 0.15f), Warning, "Paused")
        DownloadStatus.FAILED.name -> Triple(ErrorColor.copy(alpha = 0.15f), ErrorColor, "Failed")
        DownloadStatus.CANCELLED.name -> Triple(SecondaryText.copy(alpha = 0.15f), SecondaryText, "Cancelled")
        else -> Triple(SecondaryText.copy(alpha = 0.15f), SecondaryText, "Queued")
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
