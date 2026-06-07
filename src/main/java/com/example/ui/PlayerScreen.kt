package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.DarkBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import android.os.Handler
import android.os.Looper

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    streamName: String,
    playerMode: String, // Smart, m3u8, ts, Standard
    appLanguage: String = "ar",
    qualities: List<com.example.data.ChannelQuality> = emptyList(),
    userAgent: String? = null,
    onClose: () -> Unit,
    onPlayerModeChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var baseStreamUrl by remember { mutableStateOf(streamUrl) }
    var activePlayerMode by remember(playerMode) { mutableStateOf(playerMode) }
    var showProtocolSelectorDialog by remember { mutableStateOf(false) }

    val activePlayingUrl = remember(baseStreamUrl, activePlayerMode) {
        if (!baseStreamUrl.contains("/live/") && !baseStreamUrl.contains("live")) {
            baseStreamUrl
        } else {
            var base = baseStreamUrl
            if (base.endsWith(".m3u8", ignoreCase = true)) {
                base = base.substring(0, base.length - 5)
            } else if (base.endsWith(".ts", ignoreCase = true)) {
                base = base.substring(0, base.length - 3)
            }
            when (activePlayerMode.lowercase()) {
                "m3u8" -> "$base.m3u8"
                "ts", "smart" -> "$base.ts"
                else -> base // Smart, Standard, or default: pure stream URL/link with no suffix at all!
            }
        }
    }

    LaunchedEffect(streamUrl) {
        baseStreamUrl = streamUrl
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isExoBuffering by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Controls visibility timer state
    var controlsVisible by remember { mutableStateOf(true) }
    var resetTimerTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(controlsVisible, resetTimerTrigger) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Aspect ratio state inside the controller:
    // 0 = Fit to Screen, 1 = Full Screen, 2 = 16:9
    var aspectRatioMode by remember { mutableStateOf(0) }

    // Auto reconnect counter
    var retryCount by remember { mutableStateOf(0) }

    // Track position states for Movies & Series
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // Detect if content is Live stream or seekable VOD
    val isLive = remember(activePlayingUrl) {
        activePlayingUrl.contains("/live/") || activePlayingUrl.contains("live")
    }

    LaunchedEffect(isExoBuffering, isLive) {
        if (isLive) {
            if (isExoBuffering) {
                // Delay showing the loading spinner to the user by 2500ms to avoid momentary flashing
                delay(2500)
                isBuffering = true
            } else {
                isBuffering = false
            }
        } else {
            isBuffering = isExoBuffering
        }
    }

    // Silently detect and recover live stream stalling instantly
    LaunchedEffect(exoPlayer, isLive, activePlayingUrl, activePlayerMode, userAgent) {
        if (isLive) {
            var lastPosition = -1L
            var consecutiveStallSecs = 0
            var consecutiveBufferSecs = 0
            while (true) {
                delay(1000)
                exoPlayer?.let { player ->
                    if (player.playWhenReady) {
                        val currentPosition = player.currentPosition
                        val state = player.playbackState
                        
                        // Buffer Watchdog
                        if (state == Player.STATE_BUFFERING) {
                            consecutiveBufferSecs++
                        } else {
                            consecutiveBufferSecs = 0
                        }

                        // Playback Freeze Watchdog
                        if (state == Player.STATE_READY && currentPosition == lastPosition) {
                            consecutiveStallSecs++
                        } else {
                            consecutiveStallSecs = 0
                            if (state == Player.STATE_READY) {
                                lastPosition = currentPosition
                            }
                        }

                        // Silent reconnect conditions:
                        // 1. Frozen playback (STATE_READY but position stuck for 4 seconds)
                        // 2. Continuous buffering stuck for 8 seconds
                        // 3. Playback reached ended state or unexpected idle state
                        if (consecutiveStallSecs >= 4 || consecutiveBufferSecs >= 8 || state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                            android.util.Log.d("PlayerScreen", "Silent reconnect watchdog triggered [Stall: $consecutiveStallSecs, Buffer: $consecutiveBufferSecs, State: $state]")
                            try {
                                val mediaSource = createMediaSource(context, activePlayingUrl, activePlayerMode, userAgent)
                                player.setMediaSource(mediaSource)
                                player.prepare()
                                player.play()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            consecutiveStallSecs = 0
                            consecutiveBufferSecs = 0
                            lastPosition = -1L
                        }
                    } else {
                        consecutiveStallSecs = 0
                        consecutiveBufferSecs = 0
                    }
                }
            }
        }
    }

    // MANDATORY FORCE LANDSCAPE ORIENTATION ON ENTER, RESTORE ON LEAVE
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Initialize ExoPlayer clean configuration
    LaunchedEffect(activePlayingUrl, activePlayerMode, userAgent) {
        val player = ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            
            val mediaSource = createMediaSource(context, activePlayingUrl, activePlayerMode, userAgent)
            setMediaSource(mediaSource)
            prepare()
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isExoBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null
                    retryCount = 0
                    duration = player.duration
                }
                
                // If IPTV live stream ends prematurely (after 15s/30s/60s), reconnect immediately
                if (isLive && (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE)) {
                    android.util.Log.d("PlayerScreen", "Live stream ended or went idle. Reconnecting dynamically in 1.5s...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            exoPlayer?.let { p ->
                                val mediaSource = createMediaSource(context, activePlayingUrl, activePlayerMode, userAgent)
                                p.setMediaSource(mediaSource)
                                p.prepare()
                                p.play()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 1500)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isLive) {
                    android.util.Log.e("PlayerScreen", "Live stream error: ${error.message}. Silently reconnecting... in 1.5s")
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            exoPlayer?.let { p ->
                                val mediaSource = createMediaSource(context, activePlayingUrl, activePlayerMode, userAgent)
                                p.setMediaSource(mediaSource)
                                p.prepare()
                                p.play()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 1500)
                } else {
                    errorMessage = if (appLanguage == "ar") "خطأ في تشغيل البث: ${error.message}" else "Playback Error: ${error.message}"
                    if (activePlayerMode == "Smart" && retryCount < 3) {
                        retryCount++
                        errorMessage = if (appLanguage == "ar") "جاري إعادة الاتصال تلقائياً (${retryCount}/3)..." else "Reconnecting automatically (${retryCount}/3)..."
                        scopeLaunchReconnect(player, activePlayingUrl, activePlayerMode, userAgent, context)
                    }
                }
            }
        })

        exoPlayer = player

        try {
            awaitCancellation()
        } finally {
            player.release()
            exoPlayer = null
        }
    }

    // Dynamic timer ticker to update position forseekable slide
    LaunchedEffect(exoPlayer, isPlaying) {
        if (!isLive) {
            while (true) {
                exoPlayer?.let {
                    currentPos = it.currentPosition
                    if (it.duration > 0) {
                        duration = it.duration
                    }
                }
                delay(1000)
            }
        }
    }

    // Pulse animation logic for custom live badge (Blinking)
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                // Clicking anywhere on the player screen toggles controls visibility
                controlsVisible = !controlsVisible
                if (controlsVisible) {
                    resetTimerTrigger++
                }
            }
    ) {
        // Video display view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = when (aspectRatioMode) {
                    0 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = when (aspectRatioMode) {
                2 -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
                else -> Modifier.fillMaxSize()
            }
        )

        // Overlay & Clean TOD Video Player Controller Design (with dynamic AnimatedVisibility)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable {
                        // Clicking on the dim overlay background hides controls immediately
                        controlsVisible = false
                    }
            ) {
                // 1. Top bar controls Column containing Name & Exit AND Quality chips selector below it
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { resetTimerTrigger++ } // consumes click & resets timer
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onClose() },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close Player",
                                tint = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Red.copy(alpha = pulseAlpha))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "مباشر 🔴" else "LIVE 🔴",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = streamName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Qualities row (if available) shown inside Player Controls
                    if (qualities.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = if (appLanguage == "ar") "الجودة والسيرفرات:" else "Server / Quality:",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }

                            // Main stream option
                            item {
                                val isMainSelected = baseStreamUrl == streamUrl
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMainSelected) MaterialTheme.colorScheme.primary else Color(0xFF131519).copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier
                                        .clickable { 
                                            baseStreamUrl = streamUrl 
                                            resetTimerTrigger++
                                        }
                                        .border(
                                            1.dp,
                                            if (isMainSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "الأساسي" else "Main",
                                        color = if (isMainSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Additional qualities list
                            items(qualities) { quality ->
                                val isSelected = baseStreamUrl == (quality.url ?: "")
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF131519).copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier
                                        .clickable { 
                                            baseStreamUrl = quality.url ?: ""
                                            resetTimerTrigger++
                                        }
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Text(
                                        text = quality.name ?: "",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Center Quick Buffering Spinner
                if (isBuffering && errorMessage == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { resetTimerTrigger++ }
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // 3. Error Recover container
                if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                            .clickable { resetTimerTrigger++ }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("إعادة تشغيل البث", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Bottom Controls Controller Block (Video Seekbar timeline & player row)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { resetTimerTrigger++ } // consumes click & resets timer
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Seekbar seek tracks (ONLY FOR VOD/MOVIES/SERIES)
                    if (!isLive && duration > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = formatTime(currentPos),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Slider(
                                value = currentPos.toFloat().coerceIn(0f, duration.toFloat()),
                                onValueChange = { newValue ->
                                    currentPos = newValue.toLong()
                                    exoPlayer?.seekTo(newValue.toLong())
                                    resetTimerTrigger++
                                },
                                valueRange = 0f..duration.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = formatTime(duration),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interaction controls items row (Play, back10, forward10, ratio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect ratio setting menu
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    aspectRatioMode = (aspectRatioMode + 1) % 3
                                    resetTimerTrigger++
                                },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = when (aspectRatioMode) {
                                        0 -> Icons.Default.ZoomOutMap
                                        1 -> Icons.Default.Fullscreen
                                        else -> Icons.Default.AspectRatio
                                    },
                                    contentDescription = "Aspect Ratio",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = when (aspectRatioMode) {
                                    0 -> LocaleHelper.translate("mode_zoom_sides", appLanguage)
                                    1 -> LocaleHelper.translate("mode_stretch_fill", appLanguage)
                                    else -> LocaleHelper.translate("mode_original_16_9", appLanguage)
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Playback actions (Rewind 10s, Play/Pause, Forward 10s)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!isLive) {
                                IconButton(onClick = {
                                    exoPlayer?.let {
                                        val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                                        it.seekTo(newPos)
                                        currentPos = newPos
                                    }
                                    resetTimerTrigger++
                                }) {
                                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }

                            IconButton(
                                onClick = {
                                    exoPlayer?.let {
                                        if (it.isPlaying) {
                                            it.pause()
                                            isPlaying = false
                                        } else {
                                            it.play()
                                            isPlaying = true
                                        }
                                    }
                                    resetTimerTrigger++
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            if (!isLive) {
                                IconButton(onClick = {
                                    exoPlayer?.let {
                                        val newPos = (it.currentPosition + 10000).coerceAtMost(duration)
                                        it.seekTo(newPos)
                                        currentPos = newPos
                                    }
                                    resetTimerTrigger++
                                }) {
                                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        // Clickable, glowing protocol switcher action (now triggering visual selection dialog modal)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .clickable {
                                    showProtocolSelectorDialog = true
                                    resetTimerTrigger++
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Change Protocol Format",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = when (activePlayerMode) {
                                        "m3u8" -> "HLS (M3U8)"
                                        "ts" -> "IPTV (TS)"
                                        "Standard" -> "Standard"
                                        else -> if (appLanguage == "ar") "الوضع الذكي" else "Smart Auto"
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Cyber Audio-Visual Protocol Selector Dialog Modal
    if (showProtocolSelectorDialog) {
        AlertDialog(
            onDismissRequest = { 
                showProtocolSelectorDialog = false 
                resetTimerTrigger++
            },
            title = {
                Text(
                    text = if (appLanguage == "ar") "اختر نوع مشغل بث القناة" else "Select Streaming Format Protocol",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Pair("Smart", if (appLanguage == "ar") "الوضع الذكي التلقائي (اصلي بدون صيغة)" else "Smart Auto (Original stream URL)"),
                        Pair("m3u8", if (appLanguage == "ar") "بروتوكول البث والروابط m3u8 (HLS)" else "HLS protocol Stream (.m3u8)"),
                        Pair("ts", if (appLanguage == "ar") "بروتوكول بث القنوات المرمزة لـ .ts" else "IPTV mpegts Format (.ts)"),
                        Pair("Standard", if (appLanguage == "ar") "التشغيل القياسي (Standard)" else "Standard Player Format")
                    ).forEach { (code, label) ->
                        val isSelected = activePlayerMode == code
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color(0xFF1B1D22)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activePlayerMode = code
                                    onPlayerModeChanged?.invoke(code)
                                    showProtocolSelectorDialog = false
                                    resetTimerTrigger++
                                    
                                    val modeLabel = when (code) {
                                        "m3u8" -> "HLS (M3U8)"
                                        "ts" -> "IPTV (TS)"
                                        "Standard" -> "Standard"
                                        else -> if (appLanguage == "ar") "الوضع الذكي" else "Smart Auto"
                                    }
                                    val toastMsg = if (appLanguage == "ar") {
                                        "تم تغيير مشغل البث ونظام التشغيل إلى: $modeLabel (تم الحفظ لجميع القنوات)"
                                    } else {
                                        "Switched streaming protocol to: $modeLabel"
                                    }
                                    android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showProtocolSelectorDialog = false 
                    resetTimerTrigger++
                }) {
                    Text(
                        text = if (appLanguage == "ar") "إلغاء" else "Cancel",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color(0xFF111317),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

@OptIn(UnstableApi::class)
private fun createMediaSource(
    context: android.content.Context,
    url: String,
    playerMode: String,
    userAgent: String?
): androidx.media3.exoplayer.source.MediaSource {
    val mediaItem = MediaItem.fromUri(url)
    val isLocal = url.startsWith("file:/") || url.startsWith("/")
    val dataSourceFactory: androidx.media3.datasource.DataSource.Factory = if (isLocal) {
        androidx.media3.datasource.DefaultDataSource.Factory(context)
    } else {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        if (!userAgent.isNullOrBlank()) {
            httpFactory.setUserAgent(userAgent)
        }
        httpFactory
    }
    if (isLocal) {
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }
    
    val resolvedMode = when {
        url.contains(".m3u8", ignoreCase = true) -> "m3u8"
        url.contains(".ts", ignoreCase = true) -> "ts"
        else -> playerMode.lowercase()
    }
    
    return when (resolvedMode) {
        "m3u8" -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        "ts", "smart" -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        else -> DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }
}

@OptIn(UnstableApi::class)
private fun scopeLaunchReconnect(
    player: ExoPlayer,
    url: String,
    playerMode: String,
    userAgent: String?,
    context: android.content.Context
) {
    Handler(Looper.getMainLooper()).post {
        try {
            val mediaSource = createMediaSource(context, url, playerMode, userAgent)
            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VodPlayerScreen(
    streamUrl: String,
    streamName: String,
    appLanguage: String = "ar",
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val rtl = appLanguage == "ar"
    
    val speedValue by viewModel.vodPlaybackSpeed.collectAsState()
    val aspectRatioValue by viewModel.vodAspectRatio.collectAsState()
    val repeatModeValue by viewModel.vodRepeatMode.collectAsState()
    val volumeBoostValue by viewModel.vodVolumeBoost.collectAsState()

    var activePlayingUrl by remember { mutableStateOf(streamUrl) }
    LaunchedEffect(streamUrl) {
        activePlayingUrl = streamUrl
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Position and seeker states
    var currentPos by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    // Toggle panels
    var controlsVisible by remember { mutableStateOf(true) }
    var showPlayerSettings by remember { mutableStateOf(false) }

    // Auto fade controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            controlsVisible = false
        }
    }

    // MANDATORY FORCE LANDSCAPE ORIENTATION ON ENTER, RESTORE ON LEAVE
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Initialize ExoPlayer clean configuration for VOD progressive playback (MP4, MKV)
    LaunchedEffect(activePlayingUrl) {
        val player = ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            val mediaItem = MediaItem.fromUri(activePlayingUrl)
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
            val mediaSource = if (activePlayingUrl.contains(".m3u8", ignoreCase = true)) {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
            setMediaSource(mediaSource)
            prepare()
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null
                    totalDuration = player.duration.coerceAtLeast(0L)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                errorMessage = error.localizedMessage ?: "Playback Error"
            }
        })

        exoPlayer = player
    }

    // Apply speed settings to player
    LaunchedEffect(exoPlayer, speedValue) {
        exoPlayer?.let { player ->
            player.playbackParameters = androidx.media3.common.PlaybackParameters(speedValue)
        }
    }

    // Apply repeat mode config
    LaunchedEffect(exoPlayer, repeatModeValue) {
        exoPlayer?.let { player ->
            player.repeatMode = if (repeatModeValue) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        }
    }

    // Apply custom volume booster level
    LaunchedEffect(exoPlayer, volumeBoostValue) {
        exoPlayer?.let { player ->
            val vol = when (volumeBoostValue) {
                "High (+3dB)" -> 1.5f
                "Ultra (+6dB)" -> 2.0f
                else -> 1.0f
            }
            player.volume = vol
        }
    }

    // Update progress state every 300ms
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(300)
            exoPlayer?.let { player ->
                if (player.isPlaying && !isUserSeeking) {
                    currentPos = player.currentPosition.coerceAtLeast(0L)
                    totalDuration = player.duration.coerceAtLeast(0L)
                    if (totalDuration > 0) {
                        sliderValue = currentPos.toFloat() / totalDuration.toFloat()
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible }
    ) {
        // ExoPlayer View center stage
        exoPlayer?.let { player ->
            val resizeMode = when (aspectRatioValue) {
                "Zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                "16:9" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                "4:3" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom beautiful controls overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // 1. Top Bar: Back & Video Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = streamName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    IconButton(
                        onClick = { showPlayerSettings = !showPlayerSettings },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (showPlayerSettings) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                // 2. Play/Pause & Rewind/Forward center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            exoPlayer?.let { player ->
                                val target = (player.currentPosition - 10000).coerceAtLeast(0L)
                                player.seekTo(target)
                                currentPos = target
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            exoPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.play()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(68.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            exoPlayer?.let { player ->
                                val target = player.currentPosition + 10000
                                val duration = player.duration
                                val finalTarget = if (duration > 0 && target > duration) duration else target
                                player.seekTo(finalTarget)
                                currentPos = finalTarget
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // 3. Bottom controls bar: Progress seek slider & timestamps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durationStr = formatTime(totalDuration)
                    val elapsedStr = formatTime(currentPos)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = elapsedStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = durationStr, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            isUserSeeking = true
                            sliderValue = newValue
                            currentPos = (newValue * totalDuration).toLong()
                        },
                        onValueChangeFinished = {
                            isUserSeeking = false
                            exoPlayer?.seekTo(currentPos)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Custom Settings Panel Popup drawer
        if (showPlayerSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showPlayerSettings = false }
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1115)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(320.dp)
                        .clickable(enabled = false) { /* prevent close click */ }
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (rtl) "إعدادات المشغل الخاص" else "Private Player Engine",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            IconButton(onClick = { showPlayerSettings = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.05f))

                        // 1. SPEED
                        Text(
                            text = if (rtl) "سرعة تشغيل الفيديو" else "Playback Speed",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                Button(
                                    onClick = { viewModel.setVodPlaybackSpeed(speed) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (speedValue == speed) MaterialTheme.colorScheme.primary else Color(0xFF1B1E26)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = if (speedValue == speed) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 2. ASPECT RATIO
                        Text(
                            text = if (rtl) "أبعاد واجهة العرض" else "Screen Aspect Ratio",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Fit", "Zoom", "16:9").forEach { ratio ->
                                Button(
                                    onClick = { viewModel.setVodAspectRatio(ratio) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (aspectRatioValue == ratio) MaterialTheme.colorScheme.primary else Color(0xFF1B1E26)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (rtl && ratio == "Fit") "احتواء" else if (rtl && ratio == "Zoom") "ملء الشاشة" else ratio,
                                        color = if (aspectRatioValue == ratio) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. AUDIO EQUALIZER BOOST
                        Text(
                            text = if (rtl) "منظم تضخيم الصوت" else "Decoder Audio Booster",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Standard", "High (+3dB)", "Ultra (+6dB)").forEach { boost ->
                                Button(
                                    onClick = { viewModel.setVodVolumeBoost(boost) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (volumeBoostValue == boost) MaterialTheme.colorScheme.primary else Color(0xFF1B1E26)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (rtl && boost == "Standard") "قياسي" else if (rtl && boost.contains("High")) "مرتفع" else if (rtl && boost.contains("Ultra")) "أقصى" else boost,
                                        color = if (volumeBoostValue == boost) Color.Black else Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 4. AUTO REPEAT SWITCH
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B1E26), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (rtl) "تكرار الفديو تلقائياً" else "Loop Auto Repeat",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = repeatModeValue,
                                onCheckedChange = { viewModel.setVodRepeatMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Buffering loader
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            }
        }

        // Error card
        errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(text = error, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onClose) {
                        Text(text = if (rtl) "إغلاق" else "Close", color = Color.Black)
                    }
                }
            }
        }
    }
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWebViewPlayerScreen(
    videoId: String,
    streamName: String,
    appLanguage: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val rtl = appLanguage == "ar"

    // Enforce cinema-style landscape mode
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: ""
                            return if (url.contains("youtube.com/embed/") || url.contains("youtube.com/watch")) {
                                false
                            } else {
                                true
                            }
                        }

                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val blockBrandingCss = """
                                (function() {
                                    var css = '.ytp-youtube-button, .ytp-watermark, .ytp-share-button, .ytp-title, .ytp-pause-overlay, .ytp-right-controls .ytp-button { display: none !important; opacity: 0 !important; pointer-events: none !important; }';
                                    var head = document.head || document.getElementsByTagName('head')[0];
                                    var style = document.createElement('style');
                                    style.type = 'text/css';
                                    if (style.styleSheet){
                                        style.styleSheet.cssText = css;
                                    } else {
                                        style.appendChild(document.createTextNode(css));
                                    }
                                    head.appendChild(style);
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(blockBrandingCss, null)
                        }
                    }

                    webChromeClient = android.webkit.WebChromeClient()

                    val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0&showinfo=0&iv_load_policy=3&controls=1&fs=0"
                    loadUrl(embedUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close overlay button at top-left
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = if (rtl) "إغلاق" else "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Title indicator overlay at top center
        Text(
            text = streamName,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
