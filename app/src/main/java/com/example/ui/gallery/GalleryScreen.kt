package com.example.ui.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.detector.MediaDetector
import com.example.data.local.entity.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaType
import com.example.ui.MainViewModel
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.SecondaryText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val completedMedia = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.COMPLETED.name }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Videos", "Images", "GIFs", "Audio", "Documents")

    var sortOption by remember { mutableStateOf("Newest") }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredList = remember(completedMedia, selectedCategory, sortOption) {
        val catFiltered = when (selectedCategory) {
            "Videos" -> completedMedia.filter { it.mediaType == MediaType.VIDEO.name }
            "Images" -> completedMedia.filter { it.mediaType == MediaType.IMAGE.name }
            "GIFs" -> completedMedia.filter { it.mediaType == MediaType.GIF.name }
            "Audio" -> completedMedia.filter { it.mediaType == MediaType.AUDIO.name }
            "Documents" -> completedMedia.filter { it.mediaType == MediaType.DOCUMENT.name }
            else -> completedMedia
        }

        when (sortOption) {
            "Oldest" -> catFiltered.sortedBy { it.createdTimestamp }
            "Name" -> catFiltered.sortedBy { it.title.lowercase() }
            "Size" -> catFiltered.sortedByDescending { it.fileSizeBytes }
            else -> catFiltered.sortedByDescending { it.createdTimestamp }
        }
    }

    var selectedMediaItem by remember { mutableStateOf<DownloadEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    val detailsSheetState = rememberModalBottomSheetState()

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
                    text = "Saved Gallery",
                    color = colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredList.size} items in $selectedCategory",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("gallery_sort_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sort media",
                        tint = PrimaryAccent
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(colorScheme.surface)
                ) {
                    listOf("Newest", "Oldest", "Name", "Size").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = colorScheme.onSurface) },
                            onClick = {
                                sortOption = opt
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Category filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
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
                        selected = selectedCategory == cat
                    )
                )
            }
        }

        // Grid or Empty State
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
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Empty gallery",
                            tint = SecondaryText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "No $selectedCategory found",
                        color = colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Downloaded media files will be organized here.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    GalleryGridItem(
                        entity = item,
                        onClick = { selectedMediaItem = item },
                        onShare = { shareMedia(context, item) },
                        onOpen = { openMedia(context, item) }
                    )
                }
            }
        }
    }

    // Media details bottom sheet
    selectedMediaItem?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItem = null },
            sheetState = detailsSheetState,
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface
        ) {
            MediaDetailsSheet(
                item = item,
                onOpen = {
                    openMedia(context, item)
                    selectedMediaItem = null
                },
                onShare = {
                    shareMedia(context, item)
                    selectedMediaItem = null
                },
                onDelete = {
                    itemToDelete = item
                    selectedMediaItem = null
                }
            )
        }
    }

    // Delete confirmation dialog
    itemToDelete?.let { dl ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete File?", color = colorScheme.onSurface) },
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
}

@Composable
fun GalleryGridItem(
    entity: DownloadEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("gallery_item_${entity.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
    ) {
        Column {
            // Preview / Thumbnail box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val file = if (entity.fileLocation.isNotEmpty()) File(entity.fileLocation) else null
                val imageModel = if (file?.exists() == true) file else entity.thumbnailUri

                if (imageModel != null && (entity.mediaType == MediaType.IMAGE.name || entity.mediaType == MediaType.GIF.name || entity.mediaType == MediaType.VIDEO.name)) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = entity.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay icon for Video, Audio, Document
                if (entity.mediaType == MediaType.VIDEO.name) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (entity.mediaType == MediaType.AUDIO.name) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (entity.mediaType == MediaType.DOCUMENT.name) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Type badge on top right
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = entity.mediaType,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entity.title,
                    color = colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = MediaDetector.formatBytes(entity.fileSizeBytes),
                    color = SecondaryText,
                    fontSize = 10.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = SecondaryText,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaDetailsSheet(
    item: DownloadEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.createdTimestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = item.title,
            color = colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("Type", item.mediaType)
                DetailRow("File Name", item.fileName)
                DetailRow("Size", MediaDetector.formatBytes(item.fileSizeBytes))
                DetailRow("Platform", item.platform)
                DetailRow("Saved Date", dateStr)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onShare,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceVariant, contentColor = colorScheme.onSurface),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorColor)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SecondaryText, fontSize = 12.sp)
        Text(
            text = value,
            color = colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun openMedia(context: Context, item: DownloadEntity) {
    if (item.fileLocation.isEmpty()) {
        Toast.makeText(context, "File location not found", Toast.LENGTH_SHORT).show()
        return
    }
    val file = File(item.fileLocation)
    if (!file.exists()) {
        Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType.ifEmpty { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMedia(context: Context, item: DownloadEntity) {
    if (item.fileLocation.isEmpty()) {
        Toast.makeText(context, "File location not found", Toast.LENGTH_SHORT).show()
        return
    }
    val file = File(item.fileLocation)
    if (!file.exists()) {
        Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType.ifEmpty { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Saved with Smart Media Saver by HARIOM")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share media file", Toast.LENGTH_SHORT).show()
    }
}
