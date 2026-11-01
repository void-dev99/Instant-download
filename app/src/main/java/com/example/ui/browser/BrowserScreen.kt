package com.example.ui.browser

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.MainViewModel
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val initialBrowserUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    var currentUrl by remember { mutableStateOf(initialBrowserUrl) }
    var inputUrlText by remember { mutableStateOf(initialBrowserUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("Browser") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Sniffed media items from current page
    val sniffedMedia = remember { mutableStateListOf<MediaItem>() }
    var showSnifferSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Address bar & Navigation Header
            BrowserTopBar(
                urlText = inputUrlText,
                onUrlTextChange = { inputUrlText = it },
                onNavigate = {
                    var target = inputUrlText.trim()
                    if (!target.startsWith("http://") && !target.startsWith("https://")) {
                        target = if (target.contains(".") && !target.contains(" ")) {
                            "https://$target"
                        } else {
                            "https://www.google.com/search?q=" + Uri.encode(target)
                        }
                    }
                    currentUrl = target
                    inputUrlText = target
                    sniffedMedia.clear()
                    webViewInstance?.loadUrl(target)
                },
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBack = { webViewInstance?.goBack() },
                onForward = { webViewInstance?.goForward() },
                onRefresh = {
                    sniffedMedia.clear()
                    webViewInstance?.reload()
                },
                onOpenExternal = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )

            // Progress Bar
            if (isLoading && pageProgress < 100) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = PrimaryAccent,
                    trackColor = colorScheme.surfaceVariant
                )
            }

            // WebView Container
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            // Security guidelines: prevent access to local files
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 11; SmartMediaSaver/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it
                                }
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it
                                }
                                pageTitle = view?.title ?: "Web Page"
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                request?.url?.toString()?.let { reqUrl ->
                                    sniffMediaUrl(reqUrl, pageTitle, sniffedMedia)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress
                                if (newProgress == 100) isLoading = false
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrEmpty()) pageTitle = title
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )
        }

        // Sniffer Floating Action Button
        AnimatedVisibility(
            visible = sniffedMedia.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showSnifferSheet = true },
                containerColor = PrimaryAccent,
                contentColor = Color.White,
                modifier = Modifier.testTag("sniffer_fab")
            ) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = Success) {
                            Text(sniffedMedia.size.toString(), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Sniffed Media Available"
                    )
                }
            }
        }

        // Bottom Sheet showing detected media on page
        if (showSnifferSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSnifferSheet = false },
                sheetState = sheetState,
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Media Detected on Page",
                                color = colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${sniffedMedia.size} downloadable items found",
                                color = SecondaryText,
                                fontSize = 12.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Active",
                                color = Success,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sniffedMedia) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = item.type.displayName,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.type.displayName} • ${item.originalFilename}",
                                            color = SecondaryText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.startDownload(item)
                                            showSnifferSheet = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryAccent,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserTopBar(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onNavigate: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) colorScheme.onSurface else SecondaryText.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) colorScheme.onSurface else SecondaryText.copy(alpha = 0.4f)
                    )
                }

                // Address input bar
                OutlinedTextField(
                    value = urlText,
                    onValueChange = onUrlTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("browser_address_bar"),
                    singleLine = true,
                    placeholder = {
                        Text("Search or enter web URL...", color = SecondaryText, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (urlText.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                            contentDescription = "Security lock",
                            tint = if (urlText.startsWith("https://")) Success else SecondaryText,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surfaceVariant,
                        unfocusedContainerColor = colorScheme.surfaceVariant,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface
                    )
                )

                IconButton(
                    onClick = onNavigate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Go",
                        tint = PrimaryAccent
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onOpenExternal,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in external browser",
                        tint = SecondaryText
                    )
                }
            }
        }
    }
}

private fun sniffMediaUrl(
    url: String,
    pageTitle: String,
    mediaList: MutableList<MediaItem>
) {
    val clean = url.lowercase().split("?").first()
    val isMedia = clean.endsWith(".mp4") || clean.endsWith(".webm") ||
            clean.endsWith(".mkv") || clean.endsWith(".mov") ||
            clean.endsWith(".mp3") || clean.endsWith(".m4a") ||
            clean.endsWith(".gif") || clean.endsWith(".pdf")

    if (isMedia && mediaList.none { it.url == url }) {
        val ext = clean.substringAfterLast(".", "")
        val type = MediaType.fromMimeOrExtension(null, ext)
        val filename = clean.substringAfterLast("/").ifEmpty { "sniffed_media.$ext" }

        mediaList.add(
            MediaItem(
                url = url,
                title = "$pageTitle (${type.displayName})",
                type = type,
                mimeType = when (type) {
                    MediaType.VIDEO -> "video/$ext"
                    MediaType.AUDIO -> "audio/$ext"
                    MediaType.GIF -> "image/gif"
                    MediaType.DOCUMENT -> "application/pdf"
                    else -> "application/octet-stream"
                },
                platformName = "Webpage Sniffer",
                detectedFromUrl = url,
                originalFilename = filename
            )
        )
    }
}
