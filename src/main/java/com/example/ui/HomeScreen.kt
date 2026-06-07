package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LiveStreamItem
import com.example.data.MovieStreamItem
import com.example.data.SeriesItem
import com.example.data.XtreamCategory
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val isContentLoading by viewModel.isContentLoading.collectAsState()
    val isLandscapeMode by viewModel.isLandscapeMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = isLandscapeMode || (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    var showSlidingMenu by remember { mutableStateOf(false) }

    // Unified Responsive Layout
    Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 44.dp else 50.dp)
                        .background(Color(0xFF07080A))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showSlidingMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                            )
                        }

                        Text(
                            text = "LOOP LIVE",
                            fontWeight = FontWeight.Black,
                            fontSize = if (isLandscape) 16.sp else 18.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Heart Icon for Favorites in top bar (Right Side)
                        IconButton(
                            onClick = { viewModel.setTab("favorites") }
                        ) {
                            Icon(
                                imageVector = if (activeTab == "favorites") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = if (activeTab == "favorites") MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                            )
                        }

                        val count by viewModel.displayedOnlineUsersCount.collectAsState()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0xFF22C55E).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Text(
                                text = "$count",
                                color = Color(0xFF22C55E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!isLandscape) {
                    val isYoutubeEnabled by viewModel.isYoutubeEnabled.collectAsState()
                    // Ultra professional, high-fidelity modern bottom navigation bar
                    NavigationBar(
                        containerColor = Color(0xFF0F1115),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .border(width = 1.dp, color = Color.White.copy(alpha = 0.04f))
                    ) {
                        val accentColor = MaterialTheme.colorScheme.primary
                        val inactiveColor = Color.Gray

                        // 1. Live TV tab
                        NavigationBarItem(
                            selected = activeTab == "live",
                            alwaysShowLabel = true,
                            onClick = { viewModel.setTab("live") },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == "live") Icons.Filled.LiveTv else Icons.Outlined.LiveTv,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = LocaleHelper.translate("live", appLanguage), 
                                    fontSize = 8.sp, 
                                    maxLines = 1,
                                    fontWeight = if (activeTab == "live") FontWeight.ExtraBold else FontWeight.Medium
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = accentColor,
                                indicatorColor = accentColor,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        // 1.5 Main Channels (Supabase) tab
                        NavigationBarItem(
                            selected = activeTab == "main_channels",
                            alwaysShowLabel = true,
                            onClick = { viewModel.setTab("main_channels") },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == "main_channels") Icons.Filled.Cast else Icons.Outlined.Cast,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = LocaleHelper.translate("main_channels", appLanguage), 
                                    fontSize = 8.sp, 
                                    maxLines = 1,
                                    fontWeight = if (activeTab == "main_channels") FontWeight.ExtraBold else FontWeight.Medium
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = accentColor,
                                indicatorColor = accentColor,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        // 2. Library tab
                        NavigationBarItem(
                            selected = activeTab == "library",
                            alwaysShowLabel = true,
                            onClick = { viewModel.setTab("library") },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == "library") Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = LocaleHelper.translate("library", appLanguage), 
                                    fontSize = 8.sp, 
                                    maxLines = 1,
                                    fontWeight = if (activeTab == "library") FontWeight.ExtraBold else FontWeight.Medium
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = accentColor,
                                indicatorColor = accentColor,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        // 3. YouTube tab
                        if (isYoutubeEnabled) {
                            NavigationBarItem(
                                selected = activeTab == "youtube",
                                alwaysShowLabel = true,
                                onClick = { viewModel.setTab("youtube") },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == "youtube") Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { 
                                    Text(
                                        text = LocaleHelper.translate("youtube", appLanguage), 
                                        fontSize = 8.sp, 
                                        maxLines = 1,
                                        fontWeight = if (activeTab == "youtube") FontWeight.ExtraBold else FontWeight.Medium
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = accentColor,
                                    indicatorColor = accentColor,
                                    unselectedIconColor = inactiveColor,
                                    unselectedTextColor = inactiveColor
                                )
                            )
                        }

                        // 5. Settings Tab
                        NavigationBarItem(
                            selected = activeTab == "settings",
                            alwaysShowLabel = true,
                            onClick = { viewModel.setTab("settings") },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = LocaleHelper.translate("settings", appLanguage), 
                                    fontSize = 8.sp, 
                                    maxLines = 1,
                                    fontWeight = if (activeTab == "settings") FontWeight.ExtraBold else FontWeight.Medium
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = accentColor,
                                indicatorColor = accentColor,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )
                    }
                }
            },
            containerColor = DarkBackground,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLandscape) {
                    SideNavigationRail(
                        viewModel = viewModel,
                        activeTab = activeTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Tab router content
                    when (activeTab) {
                        "live" -> LiveTabContent(viewModel)
                        "main_channels" -> MainChannelsTabContent(viewModel)
                        "library" -> LibraryTabContent(viewModel)
                        "youtube" -> YouTubeTabContent(viewModel)
                        "favorites" -> FavoritesTabContent(viewModel)
                        "settings" -> SettingsTabContent(viewModel)
                        "downloads" -> DownloadsTabContent(viewModel)
                    }

                    // Beautiful Custom Premium Dual-Orbit Loading Spinner
                    if (isContentLoading) {
                        PremiumCircularLoader()
                    }
                }
            }
            
            // Custom Sliding Menu overlay
            if (showSlidingMenu) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showSlidingMenu = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { showSlidingMenu = false }
                        ) {
                        // Sliding Panel
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showSlidingMenu,
                            enter = androidx.compose.animation.slideInHorizontally(
                                initialOffsetX = { -it }
                            ),
                            exit = androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { -it }
                            ),
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(280.dp)
                                .background(Color(0xFF0F1115))
                                .clickable(enabled = false) { }
                                .align(Alignment.CenterStart)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "LOOP LIVE",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = if (appLanguage == "ar") "القائمة المنسدلة للمشغل" else "Player sliding menu",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(onClick = { showSlidingMenu = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Menu list items
                                // 1. Video Downloads
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showSlidingMenu = false
                                            viewModel.setTab("downloads")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (appLanguage == "ar") "تحميل الفيديوهات" else "Video Downloads",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 2. Telegram Support Channel
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showSlidingMenu = false
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jdj_q"))
                                            context.startActivity(intent)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.Telegram,
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (appLanguage == "ar") "قناة تليجرام" else "Telegram Channel",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 3. Logout/Sign out
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showSlidingMenu = false
                                            viewModel.logout()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (appLanguage == "ar") "تسجيل الخروج" else "Sign Out",
                                        color = Color.Red,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = if (appLanguage == "ar") "النسخة 2.5.0 " else "Version 2.5.0 ",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
}

@Composable
fun SideNavigationRail(
    viewModel: MainViewModel,
    activeTab: String,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isYoutubeEnabled by viewModel.isYoutubeEnabled.collectAsState()

    Row(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(Color(0xFF0F1115))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val items = remember(isYoutubeEnabled, activeTab, appLanguage) {
                val list = mutableListOf(
                    Triple("live", LocaleHelper.translate("live", appLanguage), if (activeTab == "live") Icons.Filled.LiveTv else Icons.Outlined.LiveTv),
                    Triple("main_channels", LocaleHelper.translate("main_channels", appLanguage), if (activeTab == "main_channels") Icons.Filled.Cast else Icons.Outlined.Cast),
                    Triple("library", LocaleHelper.translate("library", appLanguage), if (activeTab == "library") Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary)
                )
                if (isYoutubeEnabled) {
                    list.add(Triple("youtube", LocaleHelper.translate("youtube", appLanguage), if (activeTab == "youtube") Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow))
                }
                list.add(Triple("settings", LocaleHelper.translate("settings", appLanguage), if (activeTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings))
                list
            }

            items.forEach { (route, label, icon) ->
                val isSelected = activeTab == route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTab(route) }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) accentColor else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = if (isSelected) accentColor else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Vertical divider line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color(0xFF1B1E26))
        )
    }
}

// ---------------------- PRESET PREMIUM CUSTOM SPINNER ----------------------
@Composable
fun PremiumCircularLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_cycles")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_spin"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Outer rotating ring
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = angle }
                )
                // Inner rotating reversed pulsing ring
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 1.5.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .graphicsLayer {
                            rotationZ = -angle
                            scaleX = scale
                            scaleY = scale
                        }
                )
                // Centered glowing play brand icon symbol
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "جاري الاتصال وسحب البيانات بسرعة البرق...",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "LOOP LIVE NETWORKS",
                color = Color.Gray,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ---------------------- LIVE TAB CONTENT ----------------------
@Composable
fun LiveTabContent(viewModel: MainViewModel) {
    val categories by viewModel.liveCategories.collectAsState()
    val selectedCategory by viewModel.selectedLiveCategoryId.collectAsState()
    val streams by viewModel.liveStreams.collectAsState()
    val favorites by viewModel.favoriteLiveIds.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val gridCols by viewModel.gridColumns.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredStreams = remember(streams, searchQuery) {
        if (searchQuery.isBlank()) {
            streams
        } else {
            streams.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategoriesRow(
            categories = categories,
            selectedCategoryId = selectedCategory,
            appLanguage = appLanguage,
            onSelect = { viewModel.selectLiveCategory(it) }
        )

        // Custom Compact Search Row for Live TV
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = LocaleHelper.translate("search_live", appLanguage),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color(0xFF131519),
                unfocusedContainerColor = Color(0xFF131519),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp)
        )

        if (filteredStreams.isEmpty()) {
            EmptyStatePlaceholder(message = if (searchQuery.isNotBlank()) LocaleHelper.translate("no_results", appLanguage) else LocaleHelper.translate("empty_live", appLanguage))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredStreams) { stream ->
                    val isFav = favorites.contains(stream.streamId.toString())
                    LiveItemCard(
                        stream = stream,
                        isFavorite = isFav,
                        onToggleFavorite = { viewModel.toggleFavoriteLive(stream.streamId.toString()) },
                        onClick = { viewModel.selectLivePlayback(stream.streamId, stream.name) }
                    )
                }
            }
        }
    }
}

// ---------------------- MOVIES TAB CONTENT (NETFLIX STYLE) ----------------------
@Composable
fun MoviesTabContent(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categories by viewModel.movieCategories.collectAsState()
    val selectedCategory by viewModel.selectedMovieCategoryId.collectAsState()
    val movies by viewModel.movieStreams.collectAsState()
    val favorites by viewModel.favoriteMovieIds.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val gridCols by viewModel.gridColumns.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMovieForDl by remember { mutableStateOf<MovieStreamItem?>(null) }

    val filteredMovies = remember(movies, searchQuery) {
        if (searchQuery.isBlank()) {
            movies
        } else {
            movies.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                CategoriesRow(
                    categories = categories,
                    selectedCategoryId = selectedCategory,
                    appLanguage = appLanguage,
                    onSelect = { viewModel.selectMovieCategory(it) }
                )
            }

            item {
                // Custom Compact Search Row for Movies
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = LocaleHelper.translate("search_movies", appLanguage),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color(0xFF131519),
                        unfocusedContainerColor = Color(0xFF131519),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                )
            }

            if (filteredMovies.isEmpty()) {
                item {
                    EmptyStatePlaceholder(message = if (searchQuery.isNotBlank()) LocaleHelper.translate("no_results", appLanguage) else LocaleHelper.translate("empty_movies", appLanguage))
                }
            } else {
                // Netflix Billboard Banner (Top Featured Slide)
                val featuredMovie = filteredMovies.firstOrNull()
                if (featuredMovie != null && searchQuery.isBlank()) {
                    item {
                        MovieBillboard(
                            title = featuredMovie.name,
                            iconUrl = featuredMovie.streamIcon,
                            isFavorite = favorites.contains(featuredMovie.streamId.toString()),
                            onPlay = { viewModel.selectMoviePlayback(featuredMovie.streamId, featuredMovie.containerExtension, featuredMovie.name) },
                            onToggleFavorite = { viewModel.toggleFavoriteMovie(featuredMovie.streamId.toString()) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Section Title Label
                    Text(
                        text = LocaleHelper.translate("recommended_movies", appLanguage),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                }

                // Netflix vertical content gallery row flow
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(filteredMovies) { movie ->
                            val isFav = favorites.contains(movie.streamId.toString())
                            MovieItemCard(
                                movie = movie,
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleFavoriteMovie(movie.streamId.toString()) },
                                onClick = { viewModel.selectMoviePlayback(movie.streamId, movie.containerExtension, movie.name) },
                                onDownload = { selectedMovieForDl = movie },
                                modifier = Modifier.width(134.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid below of other movies
                    Text(
                        text = LocaleHelper.translate("all_movies", appLanguage),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                }

                // Dynamic grid layout
                val movieChunks = filteredMovies.chunked(gridCols)
                items(movieChunks) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        chunk.forEach { movie ->
                            val isFav = favorites.contains(movie.streamId.toString())
                            MovieItemCard(
                                movie = movie,
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleFavoriteMovie(movie.streamId.toString()) },
                                onClick = { viewModel.selectMoviePlayback(movie.streamId, movie.containerExtension, movie.name) },
                                onDownload = { selectedMovieForDl = movie },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            )
                        }
                        // Fill extra weights if row is incomplete
                        if (chunk.size < gridCols) {
                            repeat(gridCols - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Quality Selector Flow Dialog
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
                    val text = if (appLanguage == "ar") "بدأ تنزيل الفيلم محلياً" else "Starting offline movie download..."
                    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ---------------------- SERIES TAB CONTENT (NETFLIX STYLE) ----------------------
@Composable
fun SeriesTabContent(viewModel: MainViewModel) {
    val categories by viewModel.seriesCategories.collectAsState()
    val selectedCategory by viewModel.selectedSeriesCategoryId.collectAsState()
    val seriesList by viewModel.seriesItems.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()
    val episodes by viewModel.activeSeriesEpisodes.collectAsState()
    val favorites by viewModel.favoriteSeriesIds.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val gridCols by viewModel.gridColumns.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedEpisodeForDl by remember { mutableStateOf<com.example.data.SeriesEpisode?>(null) }

    val filteredSeries = remember(seriesList, searchQuery) {
        if (searchQuery.isBlank()) {
            seriesList
        } else {
            seriesList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                CategoriesRow(
                    categories = categories,
                    selectedCategoryId = selectedCategory,
                    appLanguage = appLanguage,
                    onSelect = { viewModel.selectSeriesCategory(it) }
                )
            }

            item {
                // Custom Compact Search Row for Series
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = LocaleHelper.translate("search_series", appLanguage),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color(0xFF131519),
                        unfocusedContainerColor = Color(0xFF131519),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                )
            }

            if (filteredSeries.isEmpty()) {
                item {
                    EmptyStatePlaceholder(message = if (searchQuery.isNotBlank()) LocaleHelper.translate("no_results", appLanguage) else LocaleHelper.translate("empty_series", appLanguage))
                }
            } else {
                // Netflix Feature billboard slide
                val featuredSeries = filteredSeries.firstOrNull()
                if (featuredSeries != null && searchQuery.isBlank()) {
                    item {
                        MovieBillboard(
                            title = featuredSeries.name,
                            iconUrl = featuredSeries.cover,
                            isFavorite = favorites.contains(featuredSeries.seriesId.toString()),
                            onPlay = { viewModel.selectSeriesDetails(featuredSeries) },
                            onToggleFavorite = { viewModel.toggleFavoriteSeries(featuredSeries.seriesId.toString()) },
                            playLabel = LocaleHelper.translate("explore_series", appLanguage)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = LocaleHelper.translate("recommended_series", appLanguage),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(filteredSeries) { series ->
                            val isFav = favorites.contains(series.seriesId.toString())
                            SeriesItemCard(
                                series = series,
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleFavoriteSeries(series.seriesId.toString()) },
                                onClick = { viewModel.selectSeriesDetails(series) },
                                modifier = Modifier.width(134.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = LocaleHelper.translate("all_series", appLanguage),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                }

                // Grid Layout inside LazyColumn based on user chosen columns list
                val seriesChunks = filteredSeries.chunked(gridCols)
                items(seriesChunks) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        chunk.forEach { series ->
                            val isFav = favorites.contains(series.seriesId.toString())
                            SeriesItemCard(
                                series = series,
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleFavoriteSeries(series.seriesId.toString()) },
                                onClick = { viewModel.selectSeriesDetails(series) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            )
                        }
                        if (chunk.size < gridCols) {
                            repeat(gridCols - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Series Details overlay dialogsheet
        AnimatedVisibility(
            visible = selectedSeries != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedSeries?.let { series ->
                SeriesDetailsOverlay(
                    series = series,
                    episodes = episodes,
                    appLanguage = appLanguage,
                    onClose = { viewModel.closeSeriesDetails() },
                    onEpisodeClick = { episode ->
                        viewModel.selectEpisodePlayback(
                            episode.id,
                            episode.containerExtension,
                            if (appLanguage == "ar") "${series.name} - حلقة ${episode.episodeNum}" else "${series.name} - Ep ${episode.episodeNum}"
                        )
                    },
                    onDownloadEpisodeClick = { episode ->
                        selectedEpisodeForDl = episode
                    }
                )
            }
        }

        // Episode Quality Selector Dialog
        selectedEpisodeForDl?.let { episode ->
            val context = androidx.compose.ui.platform.LocalContext.current
            val cleanHost = viewModel.activeHostState.trimEnd('/')
            val ext = episode.containerExtension ?: "mp4"
            val streamUrl = "$cleanHost/series/${viewModel.activeUsernameState}/${viewModel.activePasswordState}/${episode.id}.$ext"
            val displayTitle = if (appLanguage == "ar") "${selectedSeries?.name ?: ""} - حلقة ${episode.episodeNum}" else "${selectedSeries?.name ?: ""} - Ep ${episode.episodeNum}"
            
            DownloadQualityDialog(
                title = displayTitle,
                imageUrl = selectedSeries?.cover,
                streamUrl = streamUrl,
                appLanguage = appLanguage,
                onDismiss = { selectedEpisodeForDl = null },
                onSelectQuality = { quality ->
                    LocalDownloadManager.startDownload(
                        context = context,
                        id = "episode_${episode.id}",
                        title = displayTitle,
                        type = "series",
                        imageUrl = selectedSeries?.cover,
                        streamUrl = streamUrl,
                        quality = quality
                    )
                    selectedEpisodeForDl = null
                    val text = if (appLanguage == "ar") "بدأ تنزيل الحلقة محلياً" else "Starting offline episode download..."
                    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ---------------------- NETFLIX BILLBOARD COMPOSE BANNER ----------------------
@Composable
fun MovieBillboard(
    title: String,
    iconUrl: String?,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    playLabel: String = "تشغيل الآن"
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .padding(16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster Background Stretch
            if (!iconUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // High gradient Overlay to cover bottom safely with black transparency
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Metadata items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glow badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Red)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("متميز 🔥", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "عالي الجودة UHD",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Play / actions row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(playLabel, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- FAVORITES TAB CONTENT ----------------------
@Composable
fun FavoritesTabContent(viewModel: MainViewModel) {
    val liveStreams by viewModel.liveStreams.collectAsState()
    val favoriteLiveIds by viewModel.favoriteLiveIds.collectAsState()

    val moviesStreams by viewModel.movieStreams.collectAsState()
    val favoriteMovieIds by viewModel.favoriteMovieIds.collectAsState()

    val seriesList by viewModel.seriesItems.collectAsState()
    val favoriteSeriesIds by viewModel.favoriteSeriesIds.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    val favLiveItems = liveStreams.filter { favoriteLiveIds.contains(it.streamId.toString()) }
    val favMovieItems = moviesStreams.filter { favoriteMovieIds.contains(it.streamId.toString()) }
    val favSeriesItems = seriesList.filter { favoriteSeriesIds.contains(it.seriesId.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = LocaleHelper.translate("favorite_channels", appLanguage),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (favLiveItems.isEmpty()) {
            Text(
                text = LocaleHelper.translate("empty_fav_channels", appLanguage),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(favLiveItems) { stream ->
                    LiveItemCard(
                        stream = stream,
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleFavoriteLive(stream.streamId.toString()) },
                        onClick = { viewModel.selectLivePlayback(stream.streamId, stream.name) },
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
        }

        Text(
            text = LocaleHelper.translate("favorite_movies", appLanguage),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (favMovieItems.isEmpty()) {
            Text(
                text = LocaleHelper.translate("empty_fav_movies", appLanguage),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(favMovieItems) { movie ->
                    MovieItemCard(
                        movie = movie,
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleFavoriteMovie(movie.streamId.toString()) },
                        onClick = { viewModel.selectMoviePlayback(movie.streamId, movie.containerExtension, movie.name) },
                        modifier = Modifier.width(134.dp)
                    )
                }
            }
        }

        Text(
            text = LocaleHelper.translate("favorite_series", appLanguage),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (favSeriesItems.isEmpty()) {
            Text(
                text = LocaleHelper.translate("empty_fav_series", appLanguage),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(favSeriesItems) { series ->
                    SeriesItemCard(
                        series = series,
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleFavoriteSeries(series.seriesId.toString()) },
                        onClick = { viewModel.selectSeriesDetails(series) },
                        modifier = Modifier.width(134.dp)
                    )
                }
            }
        }
    }
}

// ---------------------- SETTINGS TAB CONTENT (POLISHED DIALOG SHEETS) ----------------------
@Composable
fun SettingsTabContent(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val activeTheme by viewModel.themeAccent.collectAsState()
    val activePlayerMode by viewModel.playerMode.collectAsState()
    val isLandscapeMode by viewModel.isLandscapeMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()

    val isDeveloperUnlocked by viewModel.isDeveloperUnlocked.collectAsState()
    var showDevAuthDialog by remember { mutableStateOf(false) }
    var devPasswordInput by remember { mutableStateOf("") }
    var devAuthError by remember { mutableStateOf("") }

    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showVodSettingsDialog by remember { mutableStateOf(false) }

    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminAuthError by remember { mutableStateOf("") }
    var showAdminPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = LocaleHelper.translate("general_settings", appLanguage),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        // 1. Appearance selection trigger card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAppearanceDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LocaleHelper.translate("app_theme", appLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = LocaleHelper.translate("current_theme", appLanguage) + activeTheme,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Default Player mode Selection trigger card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPlayerDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LocaleHelper.translate("default_player", appLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = LocaleHelper.translate("player_mode", appLanguage) + activePlayerMode,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PersonalVideo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 3. App Language selection trigger card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLanguageDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LocaleHelper.translate("app_language_title", appLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = when (appLanguage) {
                                "ar" -> "العربية 🇸🇦"
                                "en" -> "English 🇺🇸"
                                else -> "Français 🇫🇷"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 4. Content Grid Display selection trigger card (2x2, 3x3, 4x4)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showGridDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LocaleHelper.translate("grid_columns_title", appLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = LocaleHelper.translate("current_grid", appLanguage) + "${gridColumns}x${gridColumns}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 5. Landscape/TV Mode toggle card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isLandscapeMode,
                    onCheckedChange = { viewModel.setLandscapeMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF1B1E26)
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LocaleHelper.translate("universal_landscape", appLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isLandscapeMode) LocaleHelper.translate("enabled_tv", appLanguage) else LocaleHelper.translate("disabled_tv", appLanguage),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 6. Custom VOD Player Settings trigger card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showVodSettingsDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (appLanguage == "ar") "إعدادات مشغل الأفلام والمسلسلات" else "Movies & Series Player Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (appLanguage == "ar") "ضبط السرعة، الأبعاد والصوت" else "Configure speed, aspect ratio, audio boost",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 6. Admin Control Panel trigger card (Protected by password ali2008#$1)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdminAuthDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (appLanguage == "ar") "لوحة التحكم كـ مدير" else "Admin Control Panel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (appLanguage == "ar") "نشر قنوات وإدارة الجودات" else "Publish Channels & Manage Qualities",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 7. Developer Control Panel Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isDeveloperUnlocked) {
                        android.widget.Toast.makeText(context, if (appLanguage == "ar") "لوحة التحكم للمطور مفعلة بالفعل" else "Developer control panel is already active", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        showDevAuthDialog = true
                    }
                }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (appLanguage == "ar") "لوحة التحكم للمطور" else "Developer Control Panel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isDeveloperUnlocked) {
                                if (appLanguage == "ar") "نشط - نظام إدارة المطور مفعل" else "Active - Developer system enabled"
                            } else {
                                if (appLanguage == "ar") "قسم سري محمي بكلمة مرور للمطورين" else "Secret password-protected developer panel"
                            },
                            color = if (isDeveloperUnlocked) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (isDeveloperUnlocked) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showAdminAuthDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAdminAuthDialog = false 
                adminPasswordInput = ""
                adminAuthError = ""
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPasswordInput == "ali2008#$1" || adminPasswordInput == "ali2008#1") {
                            showAdminAuthDialog = false
                            showAdminPanel = true
                            adminAuthError = ""
                        } else {
                            adminAuthError = if (appLanguage == "ar") "كلمة المرور غير صحيحة" else "Incorrect password"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (appLanguage == "ar") "تأكيد" else "Verify", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAdminAuthDialog = false 
                    adminPasswordInput = ""
                    adminAuthError = ""
                }) {
                    Text(LocaleHelper.translate("cancel", appLanguage), color = Color.White)
                }
            },
            title = {
                Text(
                    text = if (appLanguage == "ar") "التحقق من هوية المدير" else "Admin Authentication",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (appLanguage == "ar") "يرجى كتابة كلمة المرور المخصصة للدخول إلى لوحة إدارة القنوات:" else "Please enter password to access Channels Control Panel:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        placeholder = { Text(if (appLanguage == "ar") "كلمة المرور" else "Password", color = Color.Gray) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF1F222B),
                            focusedContainerColor = Color(0xFF131519),
                            unfocusedContainerColor = Color(0xFF131519),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (adminAuthError.isNotEmpty()) {
                        Text(
                            text = adminAuthError,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            containerColor = Color(0xFF0F1115),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDevAuthDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDevAuthDialog = false 
                devPasswordInput = ""
                devAuthError = ""
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isCorrect = viewModel.unlockDeveloperSection(devPasswordInput)
                        if (isCorrect) {
                            showDevAuthDialog = false
                            devPasswordInput = ""
                            devAuthError = ""
                            android.widget.Toast.makeText(context, if (appLanguage == "ar") "تم تفعيل لوحة التحكم للمطور بنجاح" else "Developer control panel successfully unlocked", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            devAuthError = if (appLanguage == "ar") "نفاذ الصلاحية" else "Unauthorized - Invalid passcode"
                            android.widget.Toast.makeText(context, if (appLanguage == "ar") "نفاذ الصلاحية" else "Unauthorized access", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (appLanguage == "ar") "تأكيد" else "Verify", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDevAuthDialog = false 
                    devPasswordInput = ""
                    devAuthError = ""
                }) {
                    Text(LocaleHelper.translate("cancel", appLanguage), color = Color.White)
                }
            },
            title = {
                Text(
                    text = if (appLanguage == "ar") "لوحة المطور السرية" else "Secret Developer Panel",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (appLanguage == "ar") "أدخل كلمة المرور الخاصة بالمطور لفتح الإحصائيات وبث العداد المباشر:" else "Enter developer password to unlock real-time active users count & metrics:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = devPasswordInput,
                        onValueChange = { devPasswordInput = it },
                        placeholder = { Text(if (appLanguage == "ar") "رمز المرور" else "Passcode", color = Color.Gray) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF1F222B),
                            focusedContainerColor = Color(0xFF131519),
                            unfocusedContainerColor = Color(0xFF131519),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (devAuthError.isNotEmpty()) {
                        Text(
                            text = devAuthError,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            containerColor = Color(0xFF0F1115),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showAdminPanel) {
        AdvancedAdminPanel(
            viewModel = viewModel,
            onClose = {
                showAdminPanel = false
                viewModel.clearPublishResult()
            }
        )
    }

    // Appearance Modal Dialog
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            confirmButton = {
                Button(
                    onClick = { showAppearanceDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(LocaleHelper.translate("save", appLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = LocaleHelper.translate("app_theme", appLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    listOf(
                        Pair("Neon", if (appLanguage == "ar") "نيون سيبربنك (Cyan/Magenta)" else "Cyberpunk Neon"),
                        Pair("Cyan", if (appLanguage == "ar") "الأزرق السماوي (Cyan)" else "Sky Cyan"),
                        Pair("Magenta", if (appLanguage == "ar") "الوردي الفاقع (Magenta)" else "Hot Magenta"),
                        Pair("Amber", if (appLanguage == "ar") "الغروب الدافئ (Amber)" else "Warm Amber")
                    ).forEach { (code, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPlayerTheme(code) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeTheme == code,
                                onClick = { viewModel.setPlayerTheme(code) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = title,
                                color = if (activeTheme == code) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontWeight = if (activeTheme == code) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF131519),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Default Player Type selection Modal Dialog
    if (showPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerDialog = false },
            confirmButton = {
                Button(
                    onClick = { showPlayerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(LocaleHelper.translate("save", appLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = LocaleHelper.translate("default_player", appLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    listOf(
                        Pair("Smart", if (appLanguage == "ar") "الوضع الذكي (تحديد تلقائي TS)" else "Smart Mode (TS Auto)"),
                        Pair("m3u8", if (appLanguage == "ar") "بروتوكول البث والروابط m3u8 (HLS)" else "HLS protocol Stream (.m3u8)"),
                        Pair("ts", if (appLanguage == "ar") "بروتوكول بث القنوات المرمزة لـ .ts" else "IPTV mpegts Format (.ts)"),
                        Pair("Standard", if (appLanguage == "ar") "التشغيل القياسي (رابط عادي بدون صيغة)" else "Standard Player (No extension)")
                    ).forEach { (code, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPlayerTypeMode(code) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activePlayerMode == code,
                                onClick = { viewModel.setPlayerTypeMode(code) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = title,
                                color = if (activePlayerMode == code) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontWeight = if (activePlayerMode == code) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF131519),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Language selection Modal Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            confirmButton = {
                Button(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(LocaleHelper.translate("save", appLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = LocaleHelper.translate("select_language_dialog", appLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    listOf(
                        Pair("ar", "العربية 🇸🇦"),
                        Pair("en", "English 🇺🇸"),
                        Pair("fr", "Français 🇫🇷")
                    ).forEach { (code, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAppLanguage(code) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLanguage == code,
                                onClick = { viewModel.setAppLanguage(code) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = title,
                                color = if (appLanguage == code) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontWeight = if (appLanguage == code) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF131519),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Content Grid columns selection Modal Dialog
    if (showGridDialog) {
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            confirmButton = {
                Button(
                    onClick = { showGridDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(LocaleHelper.translate("save", appLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = LocaleHelper.translate("select_grid_dialog", appLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    listOf(
                        Pair(2, if (appLanguage == "ar") "شبكة 2x2 للمرئيات الكبيرة" else if (appLanguage == "fr") "Grille 2x2" else "2x2 Large Grid"),
                        Pair(3, if (appLanguage == "ar") "شبكة 3x3 متكاملة وافتراضية" else if (appLanguage == "fr") "Grille 3x3 (Défaut)" else "3x3 Balanced Grid (Default)"),
                        Pair(4, if (appLanguage == "ar") "شبكة 4x4 عريضة ومطورة" else if (appLanguage == "fr") "Grille 4x4" else "4x4 Wide Grid")
                    ).forEach { (cols, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setGridColumns(cols) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = gridColumns == cols,
                                onClick = { viewModel.setGridColumns(cols) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = title,
                                color = if (gridColumns == cols) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontWeight = if (gridColumns == cols) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF131519),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showVodSettingsDialog) {
        val speedValue by viewModel.vodPlaybackSpeed.collectAsState()
        val aspectRatioValue by viewModel.vodAspectRatio.collectAsState()
        val repeatModeValue by viewModel.vodRepeatMode.collectAsState()
        val volumeBoostValue by viewModel.vodVolumeBoost.collectAsState()
        val defaultVodFormat by viewModel.defaultVodFormat.collectAsState()

        AlertDialog(
            onDismissRequest = { showVodSettingsDialog = false },
            confirmButton = {
                Button(
                    onClick = { showVodSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(LocaleHelper.translate("save", appLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = if (appLanguage == "ar") "إعدادات مشغل الفيديو والمحتوى" else "VOD Media Player Configuration",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Playback Speed Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "سرعة القراءة الافتراضية" else "Default Playback Speed",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = if (speedValue == speed) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 2. Aspect Ratio Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "أبعاد شاشة العرض افتراضياً" else "Default Screen Aspect Ratio",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar" && ratio == "Fit") "احتواء" else if (appLanguage == "ar" && ratio == "Zoom") "ملء الشاشة" else ratio,
                                        color = if (aspectRatioValue == ratio) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 3. Audio Volume Booster Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (appLanguage == "ar") "تضخيم مخرج الصوت" else "Maximum Audio volume Boost",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar" && boost == "Standard") "افتراضي" else if (appLanguage == "ar" && boost.contains("High")) "مرتفع" else if (appLanguage == "ar" && boost.contains("Ultra")) "أقصى" else boost,
                                        color = if (volumeBoostValue == boost) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 4. Auto Loop Repeat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1E26), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (appLanguage == "ar") "تكرار الفديو تلقائياً" else "Loop Auto Repeat",
                            color = Color.White,
                            fontSize = 12.sp,
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

                    // 5. Default VOD format container selector (MKV or MP4)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (appLanguage == "ar") "صيغة الأفلام والمسلسلات (VOD)" else "Movies/Series Video Container (VOD)",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("mkv", "mp4").forEach { ext ->
                                val isSelected = defaultVodFormat == ext
                                Button(
                                    onClick = { viewModel.setDefaultVodFormat(ext) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1B1E26)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") {
                                            if (ext == "mkv") "الحاوية MKV (الأساسية)" else "الحاوية MP4"
                                        } else {
                                            if (ext == "mkv") "MKV Container (Default)" else "MP4 Container"
                                        },
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF131519),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ---------------------- SHARED SUB COMPOSABLES ----------------------

@Composable
fun CategoriesRow(
    categories: List<XtreamCategory>,
    selectedCategoryId: String?,
    appLanguage: String = "ar",
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF07080A))
    ) {
        items(categories) { cat ->
            val isSelected = cat.categoryId == selectedCategoryId
            val displayName = if (cat.categoryId == "all") {
                LocaleHelper.translate("all_cats", appLanguage)
            } else {
                cat.categoryName
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF131519))
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.05f),
                        CircleShape
                    )
                    .clickable { onSelect(cat.categoryId) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = displayName,
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun LiveItemCard(
    stream: LiveStreamItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E26)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!stream.streamIcon.isNullOrEmpty()) {
                        AsyncImage(
                            model = stream.streamIcon,
                            contentDescription = stream.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stream.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(32.dp)
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MovieItemCard(
    movie: MovieStreamItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDownload: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tall Vertical Aspect Ratio Poster Layout (NETFLIX STYLE)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF1B1E26)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!movie.streamIcon.isNullOrEmpty()) {
                        AsyncImage(
                            model = movie.streamIcon,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Top Right Ultra HD Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ULTRA HD",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Download overlay button if callback is defined
                    if (onDownload != null) {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(26.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Offline",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Text(
                    text = movie.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SeriesItemCard(
    series: SeriesItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tall Vertical Aspect Ratio Series Catalog Cover Layout (NETFLIX STYLE)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF1B1E26)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!series.cover.isNullOrEmpty()) {
                        AsyncImage(
                            model = series.cover,
                            contentDescription = series.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LocalMovies,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Top Right Season/Episodes Count Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SERIES HD",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = series.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SeriesDetailsOverlay(
    series: SeriesItem,
    episodes: List<com.example.data.SeriesEpisode>,
    appLanguage: String = "ar",
    onClose: () -> Unit,
    onEpisodeClick: (com.example.data.SeriesEpisode) -> Unit,
    onDownloadEpisodeClick: ((com.example.data.SeriesEpisode) -> Unit)? = null
) {
    // Group & extract distinct seasons
    val seasons = remember(episodes) {
        episodes.mapNotNull { it.seasonNum }.distinct().sortedBy { it.toIntOrNull() ?: 0 }
    }
    
    var selectedSeasonKey by remember(seasons) {
        mutableStateOf(seasons.firstOrNull() ?: "1")
    }

    val filteredEpisodes = remember(episodes, selectedSeasonKey) {
        episodes.filter { it.seasonNum == selectedSeasonKey }
    }

    val seasonLabelStr = if (appLanguage == "ar") "الموسم $selectedSeasonKey" else "Season $selectedSeasonKey"
    val listTitleStr = if (appLanguage == "ar") "قائمة الحلقات المتوفرة" else "Available Episodes"
    val loaderStr = if (appLanguage == "ar") "جاري سحب رمز وحلقات المسلسل من خادم المزود..." else "Fetching episodes from server..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = series.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seasons Switcher Bar
            if (seasons.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(seasons) { seasonKey ->
                        val isSelected = seasonKey == selectedSeasonKey
                        val label = if (appLanguage == "ar") "الموسم $seasonKey" else "Season $seasonKey"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1B1E26))
                                .clickable { selectedSeasonKey = seasonKey }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "$listTitleStr ($seasonLabelStr)",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(loaderStr, color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredEpisodes) { episode ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).clickable { onEpisodeClick(episode) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (appLanguage == "ar") "$seasonLabelStr - حلقة ${episode.episodeNum}" else "$seasonLabelStr - Ep ${episode.episodeNum}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        if (!episode.title.isNullOrEmpty()) {
                                            Text(
                                                text = episode.title,
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (onDownloadEpisodeClick != null) {
                                    IconButton(
                                        onClick = { onDownloadEpisodeClick(episode) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Episode",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
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
}

@Composable
fun EmptyStatePlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MainChannelsTabContent(viewModel: MainViewModel) {
    val channels by viewModel.supabaseChannels.collectAsState()
    val categories by viewModel.supabaseCategories.collectAsState()
    val supabaseError by viewModel.supabaseError.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val gridCols by viewModel.gridColumns.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("all") }

    val filteredChannels = remember(channels, searchQuery, selectedCategoryId) {
        val catFiltered = if (selectedCategoryId == "all") {
            channels
        } else if (selectedCategoryId == "main") {
            channels.filter { it.categoryId == "main" || it.categoryId.isNullOrBlank() }
        } else {
            channels.filter { it.categoryId == selectedCategoryId }
        }

        if (searchQuery.isBlank()) {
            catFiltered
        } else {
            catFiltered.filter { (it.name ?: "").contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Error message display if any
        supabaseError?.let { errText ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF241416)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFFE57373).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (appLanguage == "ar") "فشل جلب القنوات الرئيسية" else "Failed to fetch Main channels",
                            color = Color(0xFFE57373),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = errText,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { viewModel.fetchSupabaseChannels() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (appLanguage == "ar") "إعادة المحاولة" else "Retry",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Custom Search Row for Main Channels
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (appLanguage == "ar") "البحث عن قناة في القنوات الرئيسية..." else "Search main channels...",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF1F222B),
                focusedContainerColor = Color(0xFF0F1115),
                unfocusedContainerColor = Color(0xFF0F1115),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Horizontal Category Switchers (الشريط العلوي والأرضي للتحكّم متعدد الصفحات والأقسام المتطورة)
        val defaultCategories = listOf(
            com.example.data.ChannelCategory(id = "all", name = if (appLanguage == "ar") "الكل" else "All"),
            com.example.data.ChannelCategory(id = "main", name = if (appLanguage == "ar") "الرئيسية" else "Main")
        )
        val topCategories = defaultCategories + categories.filter { it.placement != "bottom" }
        val bottomCategories = categories.filter { it.placement == "bottom" }

        // الشريط العلوي الأول (الذي تم استعادته للشريط الكبسولي الأفقي الأنيق)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(topCategories) { cat ->
                val isSelected = selectedCategoryId == cat.id
                val activeBg = MaterialTheme.colorScheme.primary
                val inactiveBg = Color(0xFF131519)
                val textColor = if (isSelected) Color.Black else Color.LightGray
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) activeBg else inactiveBg)
                        .border(1.dp, borderColor, RoundedCornerShape(30.dp))
                        .clickable { selectedCategoryId = cat.id ?: "all" }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cat.logoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = cat.logoUrl ?: "",
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).clip(CircleShape)
                        )
                    }
                    Text(
                        text = cat.name ?: "",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // الشريط السفلي الثاني (يظهر اختيارياً بتصميم العمود فائق الترتيب والجمال حيث يظهر الاسم تحت الأيقونة)
        if (bottomCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(bottomCategories) { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    val activeBg = MaterialTheme.colorScheme.primary
                    val inactiveBg = Color(0xFF131519)
                    val textColor = if (isSelected) Color.Black else Color.LightGray
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)

                    Column(
                        modifier = Modifier
                            .widthIn(min = 72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) activeBg.copy(alpha = 0.15f) else inactiveBg)
                            .border(1.5.dp, if (isSelected) activeBg else borderColor, RoundedCornerShape(16.dp))
                            .clickable { selectedCategoryId = cat.id ?: "" }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) activeBg else Color.White.copy(alpha = 0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cat.logoUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = cat.logoUrl ?: "",
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = cat.name ?: "",
                            color = if (isSelected) activeBg else textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (filteredChannels.isEmpty()) {
            EmptyStatePlaceholder(
                message = if (searchQuery.isNotBlank()) {
                    if (appLanguage == "ar") "لا توجد نتائج مطابقة لبحثك" else "No matching channels found"
                } else {
                    if (appLanguage == "ar") "لا توجد قنوات منشورة في هذا القسم بعد" else "No channels published under this category yet"
                }
            )
        } else {
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(gridCols),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_channels_grid")
            ) {
                items(
                    items = filteredChannels,
                    span = { channel ->
                        val spanSize = when (channel.cardSize ?: "1x1") {
                            "2x2" -> 2
                            "3x3" -> 3
                            else -> 1
                        }
                        val finalSpan = spanSize.coerceAtMost(gridCols)
                        androidx.compose.foundation.lazy.grid.GridItemSpan(finalSpan)
                    }
                ) { channel ->
                    SupabaseChannelCard(
                        channel = channel,
                        onClick = { viewModel.selectSupabasePlayback(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun SupabaseChannelCard(
    channel: com.example.data.SupabaseChannel,
    onClick: () -> Unit
) {
    val aspectRatioFloat = if (channel.aspectRatio == "16:9") 1.777f else 1.0f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
            .testTag("supabase_channel_card_${channel.id}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatioFloat)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = channel.logoUrl ?: "",
                    contentDescription = channel.name ?: "",
                    contentScale = if (channel.aspectRatio == "16:9") androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (channel.aspectRatio == "16:9") 0.dp else 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = channel.name ?: "",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
@Composable
fun AdvancedAdminPanel(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val channels by viewModel.supabaseChannels.collectAsState()
    val categories by viewModel.supabaseCategories.collectAsState()

    // Exactly three solid tabs: publish_channel (نشر قناة), add_category (نشر قسم), manage_all (إدارة ومسح)
    var activeSubTab by remember { mutableStateOf("publish_channel") }

    // Form fields for publishing channel
    var chName by remember { mutableStateOf("") }
    var chLogoUrl by remember { mutableStateOf("") }
    var chUserAgent by remember { mutableStateOf("") }
    var chMainUrl by remember { mutableStateOf("") }
    
    // Position target category (either default "main" or a specific section)
    var isPublishUnderCategory by remember { mutableStateOf(false) }
    var chCategoryId by remember { mutableStateOf("main") }
    
    // Playback mode/format: "auto", "m3u8", "ts", "normal"
    var chPlaybackMode by remember { mutableStateOf("auto") }

    var chSortOrder by remember { mutableStateOf("0") }
    var chAspectRatio by remember { mutableStateOf("1:1") } // "1:1" or "16:9"
    var chCardSize by remember { mutableStateOf("1x1") } // "1x1", "2x2", "3x3"

    val extraQualities = remember { mutableStateListOf<com.example.data.ChannelQuality>() }
    var tempQualityName by remember { mutableStateOf("") }
    var tempQualityUrl by remember { mutableStateOf("") }

    // Form fields for adding section/category
    var catName by remember { mutableStateOf("") }
    var catLogoUrl by remember { mutableStateOf("") }
    var catSortOrder by remember { mutableStateOf("0") }
    var catPlacement by remember { mutableStateOf("top") } // "top" or "bottom"

    // Sub-tab under "إدارة ومسح" tab
    var manageSubTab by remember { mutableStateOf("channels") } // "channels" or "categories"
    var showEditChannelDialog by remember { mutableStateOf<com.example.data.SupabaseChannel?>(null) }

    // Form fields for adding/creating smart activation codes
    var actCodeName by remember { mutableStateOf("") }
    var actHost by remember { mutableStateOf("") }
    var actUsername by remember { mutableStateOf("") }
    var actPassword by remember { mutableStateOf("") }
    var actLiveFormat by remember { mutableStateOf("ts") }
    var actVodFormat by remember { mutableStateOf("mp4") }

    // YouTube Controller settings fields
    val isYoutubeInitiallyEnabled by viewModel.isYoutubeEnabled.collectAsState()
    val youtubeInitiallyApiKey by viewModel.youtubeApiKey.collectAsState()

    var ytEnabledState by remember { mutableStateOf(isYoutubeInitiallyEnabled) }
    var ytApiKeyInput by remember { mutableStateOf("") }

    LaunchedEffect(isYoutubeInitiallyEnabled, youtubeInitiallyApiKey) {
        ytEnabledState = isYoutubeInitiallyEnabled
        ytApiKeyInput = youtubeInitiallyApiKey
    }

    val publishResult by viewModel.publishResult.collectAsState()
    var errorMessageInput by remember { mutableStateOf("") }
    var successMessageInput by remember { mutableStateOf("") }

    LaunchedEffect(publishResult) {
        publishResult?.let { res ->
            if (res.isSuccess) {
                // Clear forms
                chName = ""
                chLogoUrl = ""
                chUserAgent = ""
                chMainUrl = ""
                chCategoryId = "main"
                isPublishUnderCategory = false
                chPlaybackMode = "auto"
                chSortOrder = "0"
                chAspectRatio = "1:1"
                chCardSize = "1x1"
                extraQualities.clear()
                tempQualityName = ""
                tempQualityUrl = ""

                catName = ""
                catLogoUrl = ""
                catSortOrder = "0"
                catPlacement = "top"

                errorMessageInput = ""
                successMessageInput = if (appLanguage == "ar") "تمت العملية بنجاح!" else "Operation completed successfully!"
                viewModel.clearPublishResult()
            } else {
                errorMessageInput = res.exceptionOrNull()?.message ?: "Unknown Error"
                successMessageInput = ""
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF07080A)
        ) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFF0F1115))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = if (appLanguage == "ar") "لوحة المطور والتحكم الذكي" else "Advanced Developer Panel",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF0F1115),
                        tonalElevation = 8.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        val activeColor = MaterialTheme.colorScheme.primary
                        val inactiveColor = Color.Gray

                        NavigationBarItem(
                            selected = activeSubTab == "publish_channel",
                            onClick = {
                                activeSubTab = "publish_channel"
                                errorMessageInput = ""
                                successMessageInput = ""
                            },
                            icon = { Icon(imageVector = Icons.Default.Tv, contentDescription = "Add Channel", modifier = Modifier.size(20.dp)) },
                            label = { Text(if (appLanguage == "ar") "نشر قنوات" else "Publish Channels", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        NavigationBarItem(
                            selected = activeSubTab == "add_category",
                            onClick = {
                                activeSubTab = "add_category"
                                errorMessageInput = ""
                                successMessageInput = ""
                            },
                            icon = { Icon(imageVector = Icons.Default.Category, contentDescription = "Add Category", modifier = Modifier.size(20.dp)) },
                            label = { Text(if (appLanguage == "ar") "أقسام" else "Sections", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        NavigationBarItem(
                            selected = activeSubTab == "server_smart_codes",
                            onClick = {
                                activeSubTab = "server_smart_codes"
                                errorMessageInput = ""
                                successMessageInput = ""
                            },
                            icon = { Icon(imageVector = Icons.Default.Dns, contentDescription = "Manage Codes", modifier = Modifier.size(20.dp)) },
                            label = { Text(if (appLanguage == "ar") "سيرفرات" else "Smart Servers", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        NavigationBarItem(
                            selected = activeSubTab == "active_users_section",
                            onClick = {
                                activeSubTab = "active_users_section"
                                errorMessageInput = ""
                                successMessageInput = ""
                            },
                            icon = { Icon(imageVector = Icons.Default.Group, contentDescription = "Active Users", modifier = Modifier.size(20.dp)) },
                            label = { Text(if (appLanguage == "ar") "النشطين" else "Active", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        NavigationBarItem(
                            selected = activeSubTab == "youtube_settings",
                            onClick = {
                                activeSubTab = "youtube_settings"
                                errorMessageInput = ""
                                successMessageInput = ""
                            },
                            icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "YouTube Settings", modifier = Modifier.size(20.dp)) },
                            label = { Text(if (appLanguage == "ar") "يوتيوب" else "YouTube", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )

                        NavigationBarItem(
                            selected = activeSubTab == "manage_all",
                            onClick = {
                                activeSubTab = "manage_all"
                                errorMessageInput = ""
                                successMessageInput = ""
                            },
                            icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Manage All", modifier = Modifier.size(20.dp)) },
                            label = { Text(if (appLanguage == "ar") "إدارة ومسح" else "Manage Content", fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = activeColor,
                                selectedTextColor = activeColor,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor
                            )
                        )
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    if (successMessageInput.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3C2A)),
                            modifier = Modifier.fillMaxWidth().clickable { successMessageInput = "" }
                        ) {
                            Text(
                                text = successMessageInput,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (errorMessageInput.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1F21)),
                            modifier = Modifier.fillMaxWidth().clickable { errorMessageInput = "" }
                        ) {
                            Text(
                                text = errorMessageInput,
                                color = Color(0xFFE57373),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    when (activeSubTab) {
                        "publish_channel" -> {
                            Text(
                                text = if (appLanguage == "ar") "نشر قناة مباشرة جديدة مجانية:" else "Publish New Channel:",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = chName,
                                onValueChange = { chName = it },
                                label = { Text(if (appLanguage == "ar") "اسم القناة *" else "Channel Name *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = chLogoUrl,
                                onValueChange = { chLogoUrl = it },
                                label = { Text(if (appLanguage == "ar") "رابط صورة شعار القناة *" else "Channel Logo URL *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = chMainUrl,
                                onValueChange = { chMainUrl = it },
                                label = { Text(if (appLanguage == "ar") "رابط البث المباشر (m3u8 أو ts) *" else "Channel Main Stream Link *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Playback Mode selection row
                            Text(
                                text = if (appLanguage == "ar") "نمط تشغيل الرابط المفضل (يجبر المشغل على هذا النمط):" else "Forced Playback Mode Selection:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val modes = listOf(
                                    Pair("auto", if (appLanguage == "ar") "مشغل ذكي" else "Smart"),
                                    Pair("m3u8", "m3u8"),
                                    Pair("ts", "ts"),
                                    Pair("normal", if (appLanguage == "ar") "رابط عادي" else "Direct")
                                )
                                modes.forEach { (modeVal, label) ->
                                    val isSelected = chPlaybackMode == modeVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF131519))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                            .clickable { chPlaybackMode = modeVal }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Publish Type Selection
                            Text(
                                text = if (appLanguage == "ar") "أين تريد نشر القناة المباشرة؟" else "Where to publish this channel?",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (!isPublishUnderCategory) MaterialTheme.colorScheme.primary else Color(0xFF131519)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            isPublishUnderCategory = false
                                            chCategoryId = "main"
                                        }
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (appLanguage == "ar") "نشر عادي (الرئيسية)" else "Normal (Main Tab)",
                                            color = if (!isPublishUnderCategory) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isPublishUnderCategory) MaterialTheme.colorScheme.primary else Color(0xFF131519)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            isPublishUnderCategory = true
                                            if (categories.isNotEmpty() && chCategoryId == "main") {
                                                chCategoryId = categories.first().id ?: "main"
                                            }
                                        }
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (appLanguage == "ar") "نشر في قسم مخصص" else "Publish under section",
                                            color = if (isPublishUnderCategory) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (isPublishUnderCategory) {
                                Text(
                                    text = if (appLanguage == "ar") "اختر من الأقسام المتاحة الفعلية للنشر بها:" else "Select target category:",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (categories.isEmpty()) {
                                    Text(
                                        text = if (appLanguage == "ar") "لا توجد أي أقسام منشورة حالياً! يرجى إنشاء قسم أولاً في التبويب الثاني." else "No sections found! Please create a section first under Tab 2.",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(categories) { cat ->
                                            val isSelected = chCategoryId == cat.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(30.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color(0xFF131519))
                                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(30.dp))
                                                    .clickable { chCategoryId = cat.id ?: "main" }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (cat.logoUrl?.isNotEmpty() == true) {
                                                        AsyncImage(
                                                            model = cat.logoUrl ?: "",
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp).clip(CircleShape)
                                                        )
                                                    }
                                                    Text(
                                                        text = cat.name ?: "",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = chUserAgent,
                                onValueChange = { chUserAgent = it },
                                label = { Text(if (appLanguage == "ar") "بيانات الـ User-Agent (اختياري)" else "User-Agent Header (Optional)", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = if (appLanguage == "ar") "أبعاد ومظهر بطاقة العرض في الشبكة:" else "Grid Card Size and Aspect Ratio:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("1:1", "16:9").forEach { ratio ->
                                    val isSelected = chAspectRatio == ratio
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF131519)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { chAspectRatio = ratio }
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.padding(10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(text = ratio, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("1x1", "2x2", "3x3").forEach { sizeOpt ->
                                    val isSelected = chCardSize == sizeOpt
                                    val sizeLabel = when (sizeOpt) {
                                        "1x1" -> if (appLanguage == "ar") "1x1 (بسيط)" else "1x1"
                                        "2x2" -> if (appLanguage == "ar") "2x2 (مزدوج)" else "2x2"
                                        else -> if (appLanguage == "ar") "3x3 (عريض)" else "3x3"
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF131519)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { chCardSize = sizeOpt }
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(text = sizeLabel, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = chSortOrder,
                                onValueChange = { chSortOrder = it },
                                label = { Text(if (appLanguage == "ar") "ترتيب ظهور القناة (مثال: 0)" else "Channel Sort Order Sequence", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = if (appLanguage == "ar") "روابط وبدائل وسيرفرات مخصصة بديلة (اختياري ومع حيز غير محدود):" else "Backup Stream servers (Optional, Unlimited):",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (extraQualities.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        extraQualities.forEachIndexed { index, q ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = q.name ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = q.url ?: "", color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                IconButton(onClick = { extraQualities.removeAt(index) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = tempQualityName,
                                            onValueChange = { tempQualityName = it },
                                            label = { Text(if (appLanguage == "ar") "اسم الجودة / السيرفر" else "Server name", color = Color.Gray, fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = tempQualityUrl,
                                            onValueChange = { tempQualityUrl = it },
                                            label = { Text(if (appLanguage == "ar") "رابط السيرفر" else "Server URL", color = Color.Gray, fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1.5f)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (tempQualityName.isNotBlank() && tempQualityUrl.isNotBlank()) {
                                                extraQualities.add(com.example.data.ChannelQuality(tempQualityName, tempQualityUrl))
                                                tempQualityName = ""
                                                tempQualityUrl = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (appLanguage == "ar") "إضافة كبث وسيرفر بديل" else "Add Server link", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (chName.isBlank() || chLogoUrl.isBlank() || chMainUrl.isBlank()) {
                                        errorMessageInput = if (appLanguage == "ar") "يرجى ملئ الاسم والشعار والروابط المباشرة المساعدة" else "Please complete Name, Logo, and Main Stream URL"
                                    } else {
                                        val orderInt = chSortOrder.toIntOrNull() ?: 0
                                        viewModel.publishChannel(
                                            name = chName,
                                            logoUrl = chLogoUrl,
                                            userAgent = chUserAgent.ifEmpty { null },
                                            mainStreamUrl = chMainUrl,
                                            qualitiesList = extraQualities.toList(),
                                            categoryId = chCategoryId,
                                            sortOrder = orderInt,
                                            aspectRatio = chAspectRatio,
                                            cardSize = chCardSize,
                                            playbackMode = chPlaybackMode
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                             ) {
                                 Text(if (appLanguage == "ar") "نشر وتثبيت القناة المباشرة" else "Publish Stream Channel", color = Color.Black, fontWeight = FontWeight.Bold)
                             }
                        }

                        "add_category" -> {
                            Text(
                                text = if (appLanguage == "ar") "إنشاء ونشر الأقسام الجديدة:" else "Publish New Section:",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = catName,
                                onValueChange = { catName = it },
                                label = { Text(if (appLanguage == "ar") "اسم القسم الجديد * (مثل BeIN Sports)" else "Category Section Name *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = catLogoUrl,
                                onValueChange = { catLogoUrl = it },
                                label = { Text(if (appLanguage == "ar") "رابط شعار وصورة القسم *" else "Category Banner/Logo URL *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Position Display Placement
                            Text(
                                text = if (appLanguage == "ar") "مكان ظهور القسم شريط علوي أول أو شريط أسفل سفلي:" else "Display Position Choice (Top rail bar or Bottom rail bar):",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (catPlacement == "top") MaterialTheme.colorScheme.primary else Color(0xFF131519)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { catPlacement = "top" }
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (appLanguage == "ar") "الشريط العلوي الأول" else "First Rail Bar (Top)",
                                            color = if (catPlacement == "top") Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (catPlacement == "bottom") MaterialTheme.colorScheme.primary else Color(0xFF131519)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { catPlacement = "bottom" }
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (appLanguage == "ar") "الشريط السفلي الثاني" else "Second Rail Bar (Bottom)",
                                            color = if (catPlacement == "bottom") Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = catSortOrder,
                                onValueChange = { catSortOrder = it },
                                label = { Text(if (appLanguage == "ar") "ترتيب ظهور القسم الجديد" else "Sort Order Sequence Weight", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (catName.isBlank() || catLogoUrl.isBlank()) {
                                        errorMessageInput = if (appLanguage == "ar") "يرجى تعبئة اسم وشعار وخلفية القسم لتأكيد النشر" else "Please type name and banner image url"
                                    } else {
                                        val orderInt = catSortOrder.toIntOrNull() ?: 0
                                        viewModel.publishCategory(catName, catLogoUrl, orderInt, catPlacement)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (appLanguage == "ar") "تأكيد نشر وتثبيت القسم" else "Create and Publish Category", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        "server_smart_codes" -> {
                            Text(
                                text = if (appLanguage == "ar") "إنشاء وإدارة سيرفرات التفعيل الذكية:" else "Create & Manage IPTV Keys:",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = actCodeName,
                                onValueChange = { actCodeName = it },
                                label = { Text(if (appLanguage == "ar") "كود التفعيل الذكي الجديد * (مثال: B6)" else "Activation Code Name *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = actHost,
                                onValueChange = { actHost = it },
                                label = { Text(if (appLanguage == "ar") "عنوان السيرفر (رابط الهوست) *" else "Server Host (Base URL) *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = actUsername,
                                onValueChange = { actUsername = it },
                                label = { Text(if (appLanguage == "ar") "اسم مستخدم اشتراك Xtream *" else "Line Username (Xtream) *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = actPassword,
                                onValueChange = { actPassword = it },
                                label = { Text(if (appLanguage == "ar") "كلمة مرور اشتراك Xtream *" else "Line Password (Xtream) *", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (appLanguage == "ar") "صيغة القنوات والـ Live الافتراضية للعميل:" else "Client Default Live Stream Format:",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { actLiveFormat = "ts" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (actLiveFormat == "ts") MaterialTheme.colorScheme.secondary else Color(0xFF131519)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("TS Protocol", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { actLiveFormat = "m3u8" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (actLiveFormat == "m3u8") MaterialTheme.colorScheme.secondary else Color(0xFF131519)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("M3U8 (HLS)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = if (appLanguage == "ar") "صيغة تشغيل الأفلام والمسلسلات (VOD) الافتراضية للعميل:" else "Client Default Movies/Series Format:",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { actVodFormat = "mp4" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (actVodFormat == "mp4") MaterialTheme.colorScheme.secondary else Color(0xFF131519)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("MP4 Container", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { actVodFormat = "mkv" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (actVodFormat == "mkv") MaterialTheme.colorScheme.secondary else Color(0xFF131519)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("MKV Container", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (actCodeName.isBlank() || actHost.isBlank() || actUsername.isBlank() || actPassword.isBlank()) {
                                        errorMessageInput = if (appLanguage == "ar") "الرجاء تعبئة جميع الحقول المطلوبة بنجاح" else "Please fill all fields"
                                    } else {
                                        viewModel.createServerActivationCode(
                                            codeName = actCodeName,
                                            host = actHost,
                                            username = actUsername,
                                            password = actPassword,
                                            defaultLiveFormat = actLiveFormat,
                                            defaultVodFormat = actVodFormat
                                        ) { res ->
                                            if (res.isSuccess) {
                                                successMessageInput = if (appLanguage == "ar") "تم إنشاء كود تفعيل السيرفر الذكي بنجاح!" else "Smart Activation Code Created successfully"
                                                errorMessageInput = ""
                                                actCodeName = ""
                                                actHost = ""
                                                actUsername = ""
                                                actPassword = ""
                                            } else {
                                                errorMessageInput = res.exceptionOrNull()?.message ?: "error creating key"
                                                successMessageInput = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (appLanguage == "ar") "تثبيت وحفظ كود السيرفر بالـ Firestore" else "Save Smart Code to Firestore", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        "active_users_section" -> {
                            val onlineUsersCount by viewModel.displayedOnlineUsersCount.collectAsState()

                            Text(
                                text = if (appLanguage == "ar") "إحصائيات الأجهزة النشطة بالوقت الفعلي:" else "Real-Time Active Devices Insights:",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFA855F7), // Neon Purple
                                                Color(0xFF6366F1), // Royal Blue
                                                Color(0xFFA855F7)
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF0F081D).copy(alpha = 0.8f),
                                                    Color.Black
                                                )
                                            )
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 20.dp, y = (-20).dp)
                                            .background(Color(0xFFA855F7).copy(alpha = 0.15f), CircleShape)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val infiniteTransition = rememberInfiniteTransition(label = "pulse_dev")
                                                val scale by infiniteTransition.animateFloat(
                                                    initialValue = 0.8f,
                                                    targetValue = 1.6f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(1000, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "scale"
                                                )
                                                val alpha by infiniteTransition.animateFloat(
                                                    initialValue = 0.3f,
                                                    targetValue = 0.9f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(1000, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "alpha"
                                                )

                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                                                            .border(2.dp, Color(0xFF22C55E), CircleShape)
                                                    )
                                                }

                                                Text(
                                                    text = if (appLanguage == "ar") "نشط الآن" else "LIVE NOW",
                                                    color = Color(0xFF22C55E),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 1.sp
                                                )
                                            }

                                            Text(
                                                text = if (appLanguage == "ar") "المتصلون بالخدمة حالياً" else "Connected IPTV Users",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )

                                            Text(
                                                text = if (appLanguage == "ar") "الاستعلام يتم بتقنية العد اللاحسابي لتوفير Firestore" else "Zero-read count queries active",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = String.format("%,d", onlineUsersCount),
                                                color = Color.White,
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color(0xFFA855F7).copy(alpha = 0.5f),
                                                        offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                                                        blurRadius = 8f
                                                    )
                                                )
                                            )
                                            Text(
                                                text = if (appLanguage == "ar") "مستخدم نشط" else "active devices",
                                                color = Color(0xFFA855F7),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "youtube_settings" -> {
                            Text(
                                text = if (appLanguage == "ar") "إعدادات وقسم يوتيوب الذكي:" else "Smart YouTube Controls:",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Status Indicator Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (appLanguage == "ar") "حالة قسم يوتيوب الحالي" else "YouTube Section State",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (appLanguage == "ar") {
                                                if (ytEnabledState) "مفعّل ويظهر في الشريط السفلي (5 أقسام)" else "ملغى ومخفي من الشريط السفلي (4 أقسام)"
                                            } else {
                                                if (ytEnabledState) "Enabled (Showing as 5th main layout tab)" else "Disabled (Hidden, 4 main layout tabs showing)"
                                            },
                                            color = if (ytEnabledState) Color(0xFF22C55E) else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(if (ytEnabledState) Color(0xFF22C55E) else Color.Gray, CircleShape)
                                            .border(2.dp, Color.Black, CircleShape)
                                    )
                                }
                            }

                            // Dynamic Switch buttons (Enable / Disable)
                            Text(
                                text = if (appLanguage == "ar") "تفعيل أو إلغاء تفعيل قسم يوتيوب:" else "Activate / Deactivate YouTube Section:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (ytEnabledState) Color(0xFF1B3C2A) else Color(0xFF131519)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { ytEnabledState = true }
                                        .border(
                                            1.dp,
                                            if (ytEnabledState) Color(0xFF22C55E) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (appLanguage == "ar") "تفعيل القسم (عرض)" else "Enable Section",
                                            color = if (ytEnabledState) Color(0xFF81C784) else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!ytEnabledState) Color(0xFF3E1F21) else Color(0xFF131519)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { ytEnabledState = false }
                                        .border(
                                            1.dp,
                                            if (!ytEnabledState) Color(0xFFEF5350) else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (appLanguage == "ar") "إلغاء القسم (إخفاء)" else "Disable Section",
                                            color = if (!ytEnabledState) Color(0xFFE57373) else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // YouTube API Key Input Text Field
                            Text(
                                text = if (appLanguage == "ar") "مفتاح API الخاص بيوتيوب (YouTube API Key):" else "Configure YouTube API Key:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = ytApiKeyInput,
                                onValueChange = { ytApiKeyInput = it },
                                label = { Text(if (appLanguage == "ar") "مفتاح API يوتيوب سحابياً" else "YouTube API Key", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF1F222B),
                                    focusedContainerColor = Color(0xFF131519),
                                    unfocusedContainerColor = Color(0xFF131519),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = if (appLanguage == "ar") {
                                    "تنبيه: سيتم تطبيق هذا التحديث ومزامنته سحابياً مباشرة عبر قاعدة بيانات Firebase لجميع المستخدمين فوراً."
                                } else {
                                    "Note: Saving these settings will update Google Firebase Realtime Database and synchronize content for all active clients dynamically."
                                },
                                color = Color.Gray,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Save button
                            Button(
                                onClick = {
                                    if (ytApiKeyInput.isBlank()) {
                                        errorMessageInput = if (appLanguage == "ar") "يرجى ملء حقل مفتاح API يوتيوب" else "API Key cannot be blank"
                                    } else {
                                        viewModel.updateYoutubeSettings(ytEnabledState, ytApiKeyInput)
                                        successMessageInput = if (appLanguage == "ar") "تم حفظ إعدادات يوتيوب سحابياً ومزامنتها بنجاح!" else "YouTube online settings saved and synced successfully!"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = if (appLanguage == "ar") "حفظ الإعدادات سحابياً والمزامنة" else "Save & Sync YouTube Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        "manage_all" -> {
                            Text(
                                text = if (appLanguage == "ar") "إدارة وحذف المحتوى المنشور مسبقاً:" else "Manage or Delete Published Content:",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Sub tabs underneath
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { manageSubTab = "channels" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (manageSubTab == "channels") MaterialTheme.colorScheme.primary else Color(0xFF131519)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "القنوات المنشورة (${channels.size})" else "Channels (${channels.size})",
                                        color = if (manageSubTab == "channels") Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { manageSubTab = "categories" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (manageSubTab == "categories") MaterialTheme.colorScheme.primary else Color(0xFF131519)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "ar") "الأقسام المضافة (${categories.size})" else "Sections (${categories.size})",
                                        color = if (manageSubTab == "categories") Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (manageSubTab == "channels") {
                                if (channels.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (appLanguage == "ar") "لا توحد أي قنوات منشورة حالياً" else "No published channels currently",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    channels.forEach { ch ->
                                        val catLabel = if (ch.categoryId == "main") (if (appLanguage == "ar") "الرئيسية" else "Main") else {
                                            categories.find { it.id == ch.categoryId }?.name ?: ch.categoryId ?: ""
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.3f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AsyncImage(
                                                            model = ch.logoUrl ?: "",
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                                        )
                                                    }

                                                    Column {
                                                        Text(text = ch.name ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(text = "${if (appLanguage == "ar") "القسم" else "Category"}: $catLabel", color = Color.Gray, fontSize = 10.sp)
                                                        Text(text = "${if (appLanguage == "ar") "الترتيب" else "Order"}: ${ch.sortOrder ?: 0} | ${ch.playbackMode ?: "auto"}", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Edit Button
                                                    IconButton(
                                                        onClick = {
                                                            showEditChannelDialog = ch
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    // Delete Button
                                                    IconButton(
                                                        onClick = {
                                                            val keyObj = ch.firebaseKey ?: ""
                                                            if (keyObj.isNotEmpty()) {
                                                                viewModel.deleteChannel(keyObj)
                                                                successMessageInput = if (appLanguage == "ar") "تم حذف القناة بنجاح!" else "Channel deleted successfully!"
                                                            }
                                                        }
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Display AlertDialog when clicked on edit
                                    showEditChannelDialog?.let { editingChannel ->
                                        var editName by remember(editingChannel) { mutableStateOf(editingChannel.name ?: "") }
                                        var editStreamUrl by remember(editingChannel) { mutableStateOf(editingChannel.mainStreamUrl ?: "") }
                                        var editLogoUrl by remember(editingChannel) { mutableStateOf(editingChannel.logoUrl ?: "") }
                                        var editUserAgent by remember(editingChannel) { mutableStateOf(editingChannel.userAgent ?: "") }
                                        var editPlaybackMode by remember(editingChannel) { mutableStateOf(editingChannel.playbackMode ?: "auto") }
                                        var editSortOrder by remember(editingChannel) { mutableStateOf((editingChannel.sortOrder ?: 0).toString()) }

                                        AlertDialog(
                                            onDismissRequest = { showEditChannelDialog = null },
                                            title = {
                                                Text(
                                                    text = if (appLanguage == "ar") "تعديل بيانات القناة" else "Modify Published Channel",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            },
                                            containerColor = Color(0xFF131519),
                                            text = {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Name Field
                                                    OutlinedTextField(
                                                        value = editName,
                                                        onValueChange = { editName = it },
                                                        label = { Text(if (appLanguage == "ar") "اسم القناة" else "Channel Name", color = Color.Gray) },
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = Color(0xFF1F222B)
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    // Stream URL Field
                                                    OutlinedTextField(
                                                        value = editStreamUrl,
                                                        onValueChange = { editStreamUrl = it },
                                                        label = { Text(if (appLanguage == "ar") "رابط البث (Stream URL)" else "Stream Link URL", color = Color.Gray) },
                                                        singleLine = false,
                                                        maxLines = 3,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = Color(0xFF1F222B)
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    // Logo URL Field
                                                    OutlinedTextField(
                                                        value = editLogoUrl,
                                                        onValueChange = { editLogoUrl = it },
                                                        label = { Text(if (appLanguage == "ar") "رابط الشعار / الصورة" else "Logo / Image URL", color = Color.Gray) },
                                                        singleLine = false,
                                                        maxLines = 3,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = Color(0xFF1F222B)
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    // User Agent Field
                                                    OutlinedTextField(
                                                        value = editUserAgent,
                                                        onValueChange = { editUserAgent = it },
                                                        label = { Text(if (appLanguage == "ar") "الـ User-Agent (اختياري)" else "User-Agent Header (Optional)", color = Color.Gray) },
                                                        singleLine = false,
                                                        maxLines = 3,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = Color(0xFF1F222B)
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    // Playback format selector (مشغل ذكي، m3u8، ts)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        listOf("auto", "m3u8", "ts").forEach { mode ->
                                                            val label = when(mode) {
                                                                "auto" -> if (appLanguage == "ar") "مشغل ذكي" else "Smart Play"
                                                                "m3u8" -> "m3u8"
                                                                else -> "ts"
                                                            }
                                                            val isChosen = editPlaybackMode == mode
                                                            Card(
                                                                colors = CardDefaults.cardColors(containerColor = if (isChosen) MaterialTheme.colorScheme.primary else Color(0xFF0F1115)),
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clickable { editPlaybackMode = mode }
                                                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Box(modifier = Modifier.padding(6.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = label,
                                                                        color = if (isChosen) Color.Black else Color.White,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Sort Order Weight Field
                                                    OutlinedTextField(
                                                        value = editSortOrder,
                                                        onValueChange = { editSortOrder = it },
                                                        label = { Text(if (appLanguage == "ar") "الوزن / الترتيب" else "Sort order priority weight", color = Color.Gray) },
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = Color(0xFF1F222B)
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        if (editName.isNotBlank() && editStreamUrl.isNotBlank()) {
                                                            val updatedChannel = editingChannel.copy(
                                                                name = editName,
                                                                mainStreamUrl = editStreamUrl,
                                                                logoUrl = editLogoUrl,
                                                                userAgent = if (editUserAgent.isBlank()) null else editUserAgent,
                                                                playbackMode = editPlaybackMode,
                                                                sortOrder = editSortOrder.toIntOrNull() ?: 0
                                                            )
                                                            viewModel.updateChannel(editingChannel.firebaseKey ?: "", updatedChannel)
                                                            successMessageInput = if (appLanguage == "ar") "تم تعديل وحفظ بيانات القناة بنجاح!" else "Channel details updated & saved successfully!"
                                                            showEditChannelDialog = null
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text(
                                                        text = if (appLanguage == "ar") "حفظ" else "Save",
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showEditChannelDialog = null }) {
                                                    Text(
                                                        text = if (appLanguage == "ar") "إلغاء" else "Cancel",
                                                        color = Color.LightGray
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                if (categories.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (appLanguage == "ar") "لا توحد أقسام مضافة للتحكّم" else "No sections found to manage",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    categories.forEach { cat ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131519)),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.3f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AsyncImage(
                                                            model = cat.logoUrl ?: "",
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                                        )
                                                    }

                                                    Column {
                                                        Text(text = cat.name ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(text = "${if (appLanguage == "ar") "الترتيب" else "Order"}: ${cat.sortOrder ?: 0} | ${if (cat.placement == "bottom") (if (appLanguage == "ar") "الشريط السفلي" else "Bottom") else (if (appLanguage == "ar") "الشريط العلوي" else "Top")}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val keyObj = cat.firebaseKey ?: ""
                                                        if (keyObj.isNotEmpty()) {
                                                            viewModel.deleteCategory(keyObj)
                                                            successMessageInput = if (appLanguage == "ar") "تم حذف القسم بنجاح!" else "Section deleted successfully!"
                                                        }
                                                    }
                                                ) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
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
        }
    }
}

@Composable
fun LibraryTabContent(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    var selectedSubTab by remember { mutableStateOf("movies") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(Color(0xFF13151B), RoundedCornerShape(24.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedSubTab == "movies") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedSubTab = "movies" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocaleHelper.translate("movies", appLanguage),
                    color = if (selectedSubTab == "movies") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedSubTab == "series") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { selectedSubTab = "series" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocaleHelper.translate("series", appLanguage),
                    color = if (selectedSubTab == "series") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedSubTab == "movies") {
                MoviesTabContent(viewModel)
            } else {
                SeriesTabContent(viewModel)
            }
        }
    }
}

@Composable
fun YouTubeTabContent(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isYoutubeLoading by viewModel.isYoutubeLoading.collectAsState()
    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    var youtubeSearchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = youtubeSearchQuery,
                onValueChange = { youtubeSearchQuery = it },
                placeholder = {
                    Text(
                        text = if (appLanguage == "ar") "ابحث في يوتيوب..." else "Search on YouTube...",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color(0xFF13151D),
                    unfocusedContainerColor = Color(0xFF13151D)
                ),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        if (youtubeSearchQuery.isNotBlank()) {
                            viewModel.fetchYoutubeVideos(youtubeSearchQuery)
                        }
                    }
                )
            )

            Button(
                onClick = {
                    viewModel.fetchYoutubeVideos(youtubeSearchQuery)
                },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = if (appLanguage == "ar") "بحث" else "Search",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (isYoutubeLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PremiumCircularLoader()
            }
        } else if (youtubeVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = if (appLanguage == "ar") "ابحث عن فيديوهات يوتيوب لمشاهدتها فوراً داخل التطبيق" else "Search YouTube videos to watch instantly in-app",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(youtubeVideos.size) { index ->
                    val video = youtubeVideos[index]
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playYoutubeVideo(context, video)
                            }
                    ) {
                        // 1. Thumbnail container with Loop Live styling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            AsyncImage(
                                model = video.thumbnail,
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Central Play Button overlay (Subtle Red YouTube glow)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(54.dp)
                                    .background(Color.Red.copy(alpha = 0.75f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Dynamic HQ overlay badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Loop Live Player",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // 2. Info Row (Avatar + Title/Stats + Action Buttons)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Circular Channel Avatar placeholder with stylized gradient matching YouTube
                            val initial = video.channelTitle.firstOrNull()?.toString()?.uppercase() ?: "Y"
                            val hash = video.channelTitle.hashCode()
                            val absHash = if (hash < 0) -hash else hash
                            val gradientColors = remember(video.channelTitle) {
                                val list = listOf(
                                    listOf(Color(0xFFFF007F), Color(0xFFFF5E62)),
                                    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
                                    listOf(Color(0xFFF9D423), Color(0xFFFF4E50)),
                                    listOf(Color(0xFFE0E0E0), Color(0xFF1F1F1F))
                                )
                                list[absHash % list.size]
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Brush.linearGradient(gradientColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Text details and stats
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = video.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = buildString {
                                        append(video.channelTitle)
                                        append(" • ")
                                        if (appLanguage == "ar") {
                                            append("٢٨٣ ألف مشاهدة • قبل يومين")
                                        } else {
                                            append("283K views • 2 days ago")
                                        }
                                    },
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Clean download action on the right side
                            IconButton(
                                onClick = {
                                    viewModel.downloadYoutubeVideo(context, video)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download locally",
                                    tint = Color.Green,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
