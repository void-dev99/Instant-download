package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.detector.MediaDetector
import com.example.data.model.DownloadStatus
import com.example.ui.MainViewModel
import com.example.ui.browser.BrowserScreen
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.BottomNavBar
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    val urls = MediaDetector.extractUrlsFromText(sharedText)
                    val urlToHandle = urls.firstOrNull() ?: sharedText.trim()
                    if (urlToHandle.isNotEmpty()) {
                        viewModel.handleSharedIntentUrl(urlToHandle)
                    }
                } else {
                    // Shared media stream URI (e.g. video, image)
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uri != null) {
                        viewModel.handleSharedIntentUrl(uri.toString())
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.firstOrNull()?.let { uri ->
                    viewModel.handleSharedIntentUrl(uri.toString())
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    val activeCount = downloads.count {
        it.status == DownloadStatus.DOWNLOADING.name || it.status == DownloadStatus.QUEUED.name
    }

    // Handle back button on sub-screens to return to Home
    if (currentScreen != "home") {
        BackHandler {
            viewModel.navigateTo("home")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomNavBar(
                currentRoute = currentScreen,
                activeDownloadsCount = activeCount,
                onNavigate = { route -> viewModel.navigateTo(route) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(viewModel = viewModel)
                "browser" -> BrowserScreen(viewModel = viewModel)
                "downloads" -> DownloadsScreen(viewModel = viewModel)
                "gallery" -> GalleryScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
                else -> HomeScreen(viewModel = viewModel)
            }
        }
    }
}
