package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MovieStreamItem
import com.example.data.SeriesItem
import kotlinx.coroutines.launch

@Composable
fun DownloadsTabContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appLanguage by viewModel.appLanguage.collectAsState()
    
    // Download tasks state
    val tasks by LocalDownloadManager.tasks.collectAsState()
    
    // Potential download suggestions state
    val movies by viewModel.movieStreams.collectAsState()
    val seriesList by viewModel.seriesItems.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.ensureMoviesAndSeriesLoaded()
    }
    
    var selectedMovieForDl by remember { mutableStateOf<MovieStreamItem?>(null) }
    var selectedSeriesForDl by remember { mutableStateOf<SeriesItem?>(null) }
    
    val rtl = appLanguage == "ar"
    
    val titleText = if (rtl) "المحتوى المحمل محلياً" else "Local Offline Downloads"
    val subtitleText = if (rtl) "شاهد أفلامك ومسلسلاتك المفضلة بدون انترنت 100%" else "Watch your movies & series completely offline"
    val emptyText = if (rtl) "لم يتم تنزيل أي محتوى بعد. اضغط على أيقونة التحميل بجانب الأفلام للبدء!" else "No downloaded files yet. Tap load icons on media items!"
    val suggestionsHeader = if (rtl) "فيديوهات مقترحة للتنزيل السريع" else "Suggested Offline Movies & Series"

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07080A))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High Premium Header Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131519))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1B1D26).copy(alpha = 0.5f),
                                        Color(0xFF131519)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = titleText,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = subtitleText,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // List of download tasks
            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                            .background(Color(0xFF0C0E12), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.White.copy(alpha = 0.02f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownloadOff,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = emptyText,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    DownloadTaskRow(
                        task = task,
                        appLanguage = appLanguage,
                        onPlay = {
                            viewModel.playOfflineFile(task.localPath, task.title)
                        },
                        onCancel = {
                            LocalDownloadManager.cancelDownload(task.id)
                        },
                        onDelete = {
                            LocalDownloadManager.deleteDownload(context, task.id)
                        }
                    )
                }
            }
            
            // Separator & Suggested movies/series
            if (movies.isNotEmpty() || seriesList.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                text = suggestionsHeader,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Suggested Horizontal Row (Mix of Movies and Series)
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Max 6 movies
                        items(movies.take(6)) { movie ->
                            SuggestedCard(
                                title = movie.name,
                                imageUrl = movie.streamIcon,
                                typeLabel = if (rtl) "فيلم" else "Movie",
                                onDownloadClick = { selectedMovieForDl = movie }
                            )
                        }
                        // Max 6 series
                        items(seriesList.take(6)) { series ->
                            SuggestedCard(
                                title = series.name,
                                imageUrl = series.cover,
                                typeLabel = if (rtl) "مسلسل" else "Series",
                                onDownloadClick = { selectedSeriesForDl = series }
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        
        // Quality Selector Dialog for Movie
        selectedMovieForDl?.let { movie ->
            val cleanHost = viewModel.activeHostState.trimEnd('/')
            val ext = movie.containerExtension ?: "mp4"
            val streamUrl = "$cleanHost/movie/${viewModel.activeUsernameState}/${viewModel.activePasswordState}/${movie.streamId}.$ext"
            
            DownloadQualityDialog(
                title = movie.name,
                imageUrl = movie.streamIcon,
                streamUrl = streamUrl,
                appLanguage = appLanguage,
                onDismiss = { selectedMovieForDl = null },
                onSelectQuality = { quality ->
                    LocalDownloadManager.startDownload(
                        context = context,
                        id = "movie_${movie.streamId}",
                        title = movie.name,
                        type = "movie",
                        imageUrl = movie.streamIcon,
                        streamUrl = streamUrl,
                        quality = quality
                    )
                    selectedMovieForDl = null
                    Toast.makeText(context, if (rtl) "بدأ تحميل: ${movie.name}" else "Starting download: ${movie.name}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Quality Selector Dialog for Series
        selectedSeriesForDl?.let { series ->
            val cleanHost = viewModel.activeHostState.trimEnd('/')
            val streamUrl = "$cleanHost/series/${viewModel.activeUsernameState}/${viewModel.activePasswordState}/1.mp4"
            
            DownloadQualityDialog(
                title = series.name,
                imageUrl = series.cover,
                streamUrl = streamUrl,
                appLanguage = appLanguage,
                onDismiss = { selectedSeriesForDl = null },
                onSelectQuality = { quality ->
                    LocalDownloadManager.startDownload(
                        context = context,
                        id = "series_${series.seriesId}",
                        title = series.name,
                        type = "series",
                        imageUrl = series.cover,
                        streamUrl = streamUrl,
                        quality = quality
                    )
                    selectedSeriesForDl = null
                    Toast.makeText(context, if (rtl) "بدأ تحميل: ${series.name}" else "Starting download: ${series.name}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun DownloadTaskRow(
    task: LocalDownloadTask,
    appLanguage: String,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val rtl = appLanguage == "ar"
    
    val isDone = task.status == "COMPLETED"
    val isDownloading = task.status == "DOWNLOADING"
    val isFailed = task.status == "FAILED"
    
    val statusLabel = when (task.status) {
        "PENDING" -> if (rtl) "قيد الانتظار في الطابور..." else "Pending in Queue..."
        "DOWNLOADING" -> if (rtl) "جاري التنزيل فائق السرعة..." else "High-speed downloading..."
        "COMPLETED" -> if (rtl) "مكتمل وحفظ بمساحة الهاتف" else "Completed Offline Stream"
        "FAILED" -> if (rtl) "فشل التنزيل (تحقق من الشبكة)" else "Failed (Check Connection)"
        else -> task.status
    }

    // Dynamic glowing border colors based on download status
    val borderColor = when {
        isDone -> Color(0xFF00E676).copy(alpha = 0.25f)
        isDownloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        isFailed -> Color(0xFFFF1744).copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.04f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111317)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item thumbnail
                Box(
                    modifier = Modifier
                        .size(62.dp, 84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B1E26))
                ) {
                    if (!task.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = task.imageUrl,
                            contentDescription = task.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (task.type == "movie") Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
                
                // Content Titles / Progress details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Ultra clean custom Quality badge tags
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.selectedQuality,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Status subtitle with indicator dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dotColor = when {
                            isDone -> Color(0xFF00C853)
                            isFailed -> Color(0xFFFF1744)
                            isDownloading -> MaterialTheme.colorScheme.primary
                            else -> Color.Gray
                        }
                        Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
                        
                        Text(
                            text = statusLabel,
                            color = if (isDone) Color(0xFF00C853) else if (isFailed) Color(0xFFFF1744) else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.1f MB / %.1f MB", task.downloadedSizeMb, task.totalSizeMb),
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = String.format("%.2f MB/s", task.speedMbps),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // Progress bar animated container
            if (isDownloading || task.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { task.progress / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = String.format("%.0f%%", task.progress),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            // Path display
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = (if (rtl) "مسار الحفظ: " else "Local Path: ") + task.localPath,
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (task.errorMessage != null && isFailed) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF1744),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = (if (rtl) "خطأ: " else "Error: ") + task.errorMessage,
                        color = Color(0xFFFF1744),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Divider(color = Color.White.copy(alpha = 0.03f), modifier = Modifier.padding(vertical = 10.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDownloading) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E151B)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                            Text(if (rtl) "إلغاء التحميل" else "Cancel Download", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (isDone) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text(if (rtl) "تشغيل الفيلم محلياً" else "Play Offline Stream", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (!isDownloading) {
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete",
                            tint = Color(0xFFE53935).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedCard(
    title: String,
    imageUrl: String?,
    typeLabel: String,
    onDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
        modifier = Modifier
            .width(114.dp)
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color(0xFF1B1E26))
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                    }
                }
                
                // Top Light Type Label Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDownloadClick() }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "تنزيل الآن",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadQualityDialog(
    title: String,
    imageUrl: String?,
    streamUrl: String,
    appLanguage: String,
    onDismiss: () -> Unit,
    onSelectQuality: (String) -> Unit
) {
    val rtl = appLanguage == "ar"
    var isProbing by remember { mutableStateOf(true) }
    var probedSizeStr by remember { mutableStateOf("") }
    var detectedQuality by remember { mutableStateOf("1080p FHD") }
    
    LaunchedEffect(streamUrl) {
        val result = LocalDownloadManager.probeStreamInfo(streamUrl)
        val fileLengthBytes = result.first
        detectedQuality = result.second
        probedSizeStr = if (fileLengthBytes > 0) {
            val sizeMb = fileLengthBytes / (1024.0 * 1024.0)
            if (sizeMb > 1024) {
                String.format("%.2f GB", sizeMb / 1024.0)
            } else {
                String.format("%.1f MB", sizeMb)
            }
        } else {
            if (rtl) "حجم عائم بالكامل" else "Flexible Dynamic Stream"
        }
        isProbing = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (rtl) "تأكيد جودة وموثوقية البث" else "Verify Stream Specifications",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Thumbnail
                Box(
                    modifier = Modifier
                        .size(100.dp, 136.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E26))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    if (!imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
                
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Divider(color = Color.White.copy(alpha = 0.04f))
                
                if (isProbing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1E26).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (rtl) "جاري استنطاق جودة البث الفعالة..." else "Analyzing active streaming bitrates...",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1E26).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (rtl) "الجودة الحقيقية المكتشفة من السيرفر" else "Real Quality Detected from Stream",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = detectedQuality,
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "•",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = probedSizeStr,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Destination formats
                val options = listOf(
                    "Original SOURCE",
                    "1080p FHD",
                    "720p HD",
                    "480p SD"
                )
                
                options.forEach { opt ->
                    val isRecommend = opt.lowercase().contains(detectedQuality.substringBefore(" ").lowercase()) || 
                                     (opt.contains("Original") && detectedQuality.contains("Original"))
                    Button(
                        onClick = { onSelectQuality(opt) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecommend) MaterialTheme.colorScheme.primary else Color(0xFF1B1E26),
                            contentColor = if (isRecommend) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = opt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (isRecommend) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (rtl) "الأنسب من السيرفر" else "Recommended",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (rtl) "إلغاء التنزيل" else "Cancel",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color(0xFF131519),
        shape = RoundedCornerShape(20.dp)
    )
}
