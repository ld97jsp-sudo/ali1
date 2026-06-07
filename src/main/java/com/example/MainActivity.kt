package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var firebaseRepo: com.example.data.FirebaseRepository
    private var androidIdStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseRepo = com.example.data.FirebaseRepository(applicationContext)
        androidIdStr = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        
        // Register online immediately
        firebaseRepo.registerOnlineUser(androidIdStr)

        LocalDownloadManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val themeAccent by viewModel.themeAccent.collectAsState()
            val selectedStreamUrl by viewModel.selectedStreamUrl.collectAsState()
            val selectedStreamName by viewModel.selectedStreamName.collectAsState()
            val selectedStreamQualities by viewModel.selectedStreamQualities.collectAsState()
            val playerMode by viewModel.playerMode.collectAsState()
            val isLandscapeMode by viewModel.isLandscapeMode.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()

            val selectedSupabasePlaybackMode by viewModel.selectedSupabasePlaybackMode.collectAsState()
            val selectedSupabaseUserAgent by viewModel.selectedSupabaseUserAgent.collectAsState()

            LaunchedEffect(isLandscapeMode) {
                requestedOrientation = if (isLandscapeMode) {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            val layoutDirection = if (appLanguage == "ar") {
                androidx.compose.ui.unit.LayoutDirection.Rtl
            } else {
                androidx.compose.ui.unit.LayoutDirection.Ltr
            }

            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(themeName = themeAccent) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        if (!isLoggedIn) {
                            LoginScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            HomeScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )

                            // Full Screen video player overlay
                            selectedStreamUrl?.let { url ->
                                if (url.startsWith("youtube_embed:")) {
                                    val videoId = url.substringAfter("youtube_embed:")
                                    YoutubeEmbeddedPlayerScreen(
                                        videoId = videoId,
                                        videoName = selectedStreamName ?: "YouTube Video",
                                        onClose = { viewModel.clearPlayback() }
                                    )
                                } else {
                                    val isVod = url.contains("/movie/") || url.contains("/series/") || url.startsWith("file:/") || url.startsWith("/") || url.endsWith(".mp4") || url.endsWith(".mkv") || url.contains("googlevideo.com") || url.contains("youtube.com") || url.contains("youtu.be") || url.contains("cobalt")
                                    if (isVod) {
                                        VodPlayerScreen(
                                            streamUrl = url,
                                            streamName = selectedStreamName ?: if (appLanguage == "ar") "مشاهدة" else "Play",
                                            appLanguage = appLanguage,
                                            viewModel = viewModel,
                                            onClose = { viewModel.clearPlayback() }
                                        )
                                    } else {
                                        val finalPlayerMode = when (selectedSupabasePlaybackMode) {
                                            "m3u8" -> "m3u8"
                                            "ts" -> "ts"
                                            "normal" -> "Standard"
                                            "auto" -> "Smart"
                                            else -> playerMode
                                        }
                                        PlayerScreen(
                                            streamUrl = url,
                                            streamName = selectedStreamName ?: if (appLanguage == "ar") "بث مباشر" else "Live Stream",
                                            playerMode = finalPlayerMode,
                                            appLanguage = appLanguage,
                                            qualities = selectedStreamQualities,
                                            userAgent = selectedSupabaseUserAgent,
                                            onClose = { viewModel.clearPlayback() },
                                            onPlayerModeChanged = { newMode -> viewModel.setPlayerTypeMode(newMode) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::firebaseRepo.isInitialized && androidIdStr.isNotEmpty()) {
            firebaseRepo.unregisterOnlineUser(androidIdStr)
        }
        super.onDestroy()
    }
}

@Composable
fun YoutubeEmbeddedPlayerScreen(
    videoId: String,
    videoName: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                android.webkit.WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        allowContentAccess = true
                        allowFileAccess = true
                    }
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: ""
                            if (request != null && request.isForMainFrame) {
                                // Allow only loading of the embed player URL
                                if (url.contains("youtube-nocookie.com/embed/") || url.contains("youtube.com/embed/")) {
                                    return false
                                }
                                return true // BLOCK all redirecting out
                            }
                            return false
                        }

                        @Deprecated("Deprecated in Java", ReplaceWith("true"))
                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView?,
                            url: String?
                        ): Boolean {
                            val u = url ?: ""
                            if (u.contains("youtube-nocookie-com/embed/") || u.contains("youtube-nocookie.com/embed/") || u.contains("youtube.com/embed/")) {
                                return false
                            }
                            return true
                        }

                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Clean up YouTube brandings, links, share overlays, and watermarks via active CSS injection
                            view?.evaluateJavascript("""
                                (function() {
                                    var style = document.createElement('style');
                                    style.innerHTML = `
                                        * {
                                            -webkit-user-select: none !important;
                                            -webkit-touch-callout: none !important;
                                            -webkit-tap-highlight-color: transparent !important;
                                        }
                                        .ytp-youtube-button, 
                                        .ytp-watermark, 
                                        .ytp-share-button, 
                                        .ytp-logo, 
                                        .ytp-impression-link, 
                                        .ytp-chrome-top-buttons, 
                                        .ytp-pause-overlay,
                                        .ytp-watch-later-button, 
                                        .ytp-copylink-button, 
                                        .ytp-report-button,
                                        .ytp-title-link, 
                                        .ytp-title-channel-logo,
                                        a, button[aria-label*="Share"], button[aria-label*="share"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            pointer-events: none !important;
                                        }
                                    `;
                                    document.head.appendChild(style);
                                    
                                    // Deep event capturer to block any link tap or redirections out of the secure loop
                                    document.body.addEventListener('click', function(e) {
                                        var target = e.target;
                                        while (target && target !== document.body) {
                                            if (target.tagName === 'A' || target.getAttribute('href') !== null) {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                return false;
                                            }
                                            target = target.parentNode;
                                        }
                                    }, true);
                                })()
                            """.trimIndent(), null)
                        }
                    }
                    webChromeClient = android.webkit.WebChromeClient()
                    
                    val url = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&showinfo=0&iv_load_policy=3&controls=1&fs=1&playsinline=1"
                    loadUrl(url)
                }
            }
        )

        // Title and Back button overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = videoName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
