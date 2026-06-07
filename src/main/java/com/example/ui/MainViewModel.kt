package com.example.ui

import android.content.Intent
import android.net.Uri
import android.app.Application
import android.util.Log
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection

data class YouTubeVideo(
    val id: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val publishedAt: String,
    val channelTitle: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val preferencesManager = PreferencesManager(context)
    private val firebaseRepository = FirebaseRepository(context)

    // YouTube State Fields
    private val _isYoutubeEnabled = MutableStateFlow(true)
    val isYoutubeEnabled: StateFlow<Boolean> = _isYoutubeEnabled

    private val _youtubeApiKey = MutableStateFlow("AIzaSyAiMHQnWOt9tOtqlpddmIPPgwN0GUbtmAM")
    val youtubeApiKey: StateFlow<String> = _youtubeApiKey

    private val _youtubeVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = _youtubeVideos

    private val _isYoutubeLoading = MutableStateFlow(false)
    val isYoutubeLoading: StateFlow<Boolean> = _isYoutubeLoading

    // Authentication States
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    // Configurations state
    val themeAccent: StateFlow<String> = preferencesManager.selectedTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Neon"
    )

    val playerMode: StateFlow<String> = preferencesManager.playerMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Smart"
    )

    val defaultLiveFormat: StateFlow<String> = preferencesManager.defaultLiveFormat.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "ts"
    )

    val defaultVodFormat: StateFlow<String> = preferencesManager.defaultVodFormat.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "mkv"
    )

    val appLanguage: StateFlow<String> = preferencesManager.appLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "ar"
    )

    val gridColumns: StateFlow<Int> = preferencesManager.gridColumns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val isLandscapeMode: StateFlow<Boolean> = preferencesManager.isLandscapeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val vodPlaybackSpeed: StateFlow<Float> = preferencesManager.vodPlaybackSpeed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    val vodAspectRatio: StateFlow<String> = preferencesManager.vodAspectRatio.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Fit"
    )

    val vodRepeatMode: StateFlow<Boolean> = preferencesManager.vodRepeatMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val vodVolumeBoost: StateFlow<String> = preferencesManager.vodVolumeBoost.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Standard"
    )

    fun setVodPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesManager.saveVodPlaybackSpeed(speed)
        }
    }

    fun setVodAspectRatio(ratio: String) {
        viewModelScope.launch {
            preferencesManager.saveVodAspectRatio(ratio)
        }
    }

    fun setVodRepeatMode(repeat: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveVodRepeatMode(repeat)
        }
    }

    fun setVodVolumeBoost(boostMode: String) {
        viewModelScope.launch {
            preferencesManager.saveVodVolumeBoost(boostMode)
        }
    }

    // active playlist details
    private var xtreamService: XtreamService? = null
    var activeHostState = ""
        private set
    var activeUsernameState = ""
        private set
    var activePasswordState = ""
        private set

    // Current Navigation states
    private val _activeTab = MutableStateFlow("live") // live, movies, series, favorites, settings
    val activeTab: StateFlow<String> = _activeTab

    // Video Player state
    private val _selectedStreamUrl = MutableStateFlow<String?>(null)
    val selectedStreamUrl: StateFlow<String?> = _selectedStreamUrl

    private val _selectedStreamName = MutableStateFlow<String?>(null)
    val selectedStreamName: StateFlow<String?> = _selectedStreamName

    private val _selectedStreamQualities = MutableStateFlow<List<ChannelQuality>>(emptyList())
    val selectedStreamQualities: StateFlow<List<ChannelQuality>> = _selectedStreamQualities

    private val _selectedSupabasePlaybackMode = MutableStateFlow<String?>(null)
    val selectedSupabasePlaybackMode: StateFlow<String?> = _selectedSupabasePlaybackMode

    private val _selectedSupabaseUserAgent = MutableStateFlow<String?>(null)
    val selectedSupabaseUserAgent: StateFlow<String?> = _selectedSupabaseUserAgent

    // Supabase Channels (Firebase RTDB main_channels) state
    private val _supabaseChannels = MutableStateFlow<List<SupabaseChannel>>(emptyList())
    val supabaseChannels: StateFlow<List<SupabaseChannel>> = _supabaseChannels

    // Firebase RTDB main_categories state
    private val _supabaseCategories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val supabaseCategories: StateFlow<List<ChannelCategory>> = _supabaseCategories

    private val _supabaseError = MutableStateFlow<String?>(null)
    val supabaseError: StateFlow<String?> = _supabaseError

    private val _publishResult = MutableStateFlow<Result<Unit>?>(null)
    val publishResult: StateFlow<Result<Unit>?> = _publishResult

    // Real-time online users
    val displayedOnlineUsersCount: StateFlow<Int> = firebaseRepository.listenOnlineUsersCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1
        )

    private val _isDeveloperUnlocked = MutableStateFlow(false)
    val isDeveloperUnlocked: StateFlow<Boolean> = _isDeveloperUnlocked

    fun unlockDeveloperSection(password: String): Boolean {
        if (password == "LoopAdmin2026") {
            _isDeveloperUnlocked.value = true
            return true
        }
        return false
    }

    fun registerOnlineUser(androidId: String) {
        firebaseRepository.registerOnlineUser(androidId)
    }

    fun unregisterOnlineUser(androidId: String) {
        firebaseRepository.unregisterOnlineUser(androidId)
    }

    fun clearPublishResult() {
        _publishResult.value = null
    }

    fun fetchSupabaseChannels() {
        _isContentLoading.value = true
        _supabaseError.value = null
        viewModelScope.launch {
            try {
                // Fetch categories
                val cats = withContext(Dispatchers.IO) {
                    firebaseRepository.getCategories()
                }
                _supabaseCategories.value = cats.sortedBy { it.sortOrder }

                // Fetch channels
                val list = withContext(Dispatchers.IO) {
                    firebaseRepository.getChannels()
                }
                _supabaseChannels.value = list.sortedBy { it.sortOrder }
                _supabaseError.value = null
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching Firebase RTDB channels/categories: ${e.message}", e)
                val errMsg = if (e is java.net.UnknownHostException || e.message?.contains("Unable to resolve host") == true) {
                    if (appLanguage.value == "ar") "خطأ في الاتصال بالإنترنت: يرجى التحقق من اتصال جهازك بالشبكة." else "Internet connection error: Please verify your device's network connection."
                } else {
                    e.message ?: e.toString()
                }
                _supabaseError.value = errMsg
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun publishChannel(
        name: String,
        logoUrl: String,
        userAgent: String?,
        mainStreamUrl: String,
        qualitiesList: List<ChannelQuality>,
        categoryId: String = "main",
        sortOrder: Int = 0,
        aspectRatio: String = "1:1",
        cardSize: String = "1x1",
        playbackMode: String = "auto"
    ) {
        viewModelScope.launch {
            _isContentLoading.value = true
            _publishResult.value = null
            try {
                val channel = SupabaseChannel(
                    name = name,
                    logoUrl = logoUrl,
                    userAgent = userAgent?.ifEmpty { null },
                    mainStreamUrl = mainStreamUrl,
                    qualities = qualitiesList.ifEmpty { null },
                    categoryId = categoryId,
                    sortOrder = sortOrder,
                    aspectRatio = aspectRatio,
                    cardSize = cardSize,
                    playbackMode = playbackMode
                )
                withContext(Dispatchers.IO) {
                    firebaseRepository.insertChannel(channel)
                }
                _publishResult.value = Result.success(Unit)
                fetchSupabaseChannels()
            } catch (e: Exception) {
                val readableException = if (e is java.net.UnknownHostException || e.message?.contains("Unable to resolve host") == true) {
                    val errMsg = if (appLanguage.value == "ar") {
                        "خطأ في الاتصال بالشبكة: يرجى التحقق من اتصال وجدار الحماية الخاص بالإنترنت."
                    } else {
                        "Network error: Please check your internet connection of this device."
                    }
                    Exception(errMsg, e)
                } else {
                    e
                }
                _publishResult.value = Result.failure(readableException)
                Log.e("MainViewModel", "Error publishing channel to Firebase: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun publishCategory(
        name: String,
        logoUrl: String,
        sortOrder: Int,
        placement: String = "top"
    ) {
        viewModelScope.launch {
            _isContentLoading.value = true
            _publishResult.value = null
            try {
                val cat = ChannelCategory(
                    id = name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis(),
                    name = name,
                    logoUrl = logoUrl,
                    sortOrder = sortOrder,
                    placement = placement
                )
                withContext(Dispatchers.IO) {
                    firebaseRepository.insertCategory(cat)
                }
                _publishResult.value = Result.success(Unit)
                fetchSupabaseChannels()
            } catch (e: Exception) {
                _publishResult.value = Result.failure(e)
                Log.e("MainViewModel", "Error publishing category to Firebase: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun deleteChannel(firebaseKey: String) {
        viewModelScope.launch {
            _isContentLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    firebaseRepository.deleteChannel(firebaseKey)
                }
                fetchSupabaseChannels()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting channel: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun updateChannel(firebaseKey: String, channel: SupabaseChannel) {
        viewModelScope.launch {
            _isContentLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    firebaseRepository.updateChannel(firebaseKey, channel)
                }
                fetchSupabaseChannels()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating channel: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun deleteCategory(firebaseKey: String) {
        viewModelScope.launch {
            _isContentLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    firebaseRepository.deleteCategory(firebaseKey)
                }
                fetchSupabaseChannels()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting category: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun selectSupabasePlayback(channel: SupabaseChannel) {
        _selectedStreamUrl.value = channel.mainStreamUrl ?: ""
        _selectedStreamName.value = channel.name ?: ""
        _selectedSupabasePlaybackMode.value = channel.playbackMode
        _selectedStreamQualities.value = channel.qualities ?: emptyList()
        _selectedSupabaseUserAgent.value = channel.userAgent
    }

    // Loading indicator for IPTV lists
    private val _isContentLoading = MutableStateFlow(false)
    val isContentLoading: StateFlow<Boolean> = _isContentLoading

    // Dynamic Categories retrieved from Xtream API
    private val _liveCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val liveCategories: StateFlow<List<XtreamCategory>> = _liveCategories

    private val _movieCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val movieCategories: StateFlow<List<XtreamCategory>> = _movieCategories

    private val _seriesCategories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val seriesCategories: StateFlow<List<XtreamCategory>> = _seriesCategories

    // Selected category ID for each source
    private val _selectedLiveCategoryId = MutableStateFlow<String?>(null)
    val selectedLiveCategoryId: StateFlow<String?> = _selectedLiveCategoryId

    private val _selectedMovieCategoryId = MutableStateFlow<String?>(null)
    val selectedMovieCategoryId: StateFlow<String?> = _selectedMovieCategoryId

    private val _selectedSeriesCategoryId = MutableStateFlow<String?>(null)
    val selectedSeriesCategoryId: StateFlow<String?> = _selectedSeriesCategoryId

    // Fetched streams lists
    private val _liveStreams = MutableStateFlow<List<LiveStreamItem>>(emptyList())
    val liveStreams: StateFlow<List<LiveStreamItem>> = _liveStreams

    private val _movieStreams = MutableStateFlow<List<MovieStreamItem>>(emptyList())
    val movieStreams: StateFlow<List<MovieStreamItem>> = _movieStreams

    private val _seriesItems = MutableStateFlow<List<SeriesItem>>(emptyList())
    val seriesItems: StateFlow<List<SeriesItem>> = _seriesItems

    // Star/Favorites
    val favoriteLiveIds: StateFlow<Set<String>> = preferencesManager.favoriteLiveStreams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val favoriteMovieIds: StateFlow<Set<String>> = preferencesManager.favoriteMovies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val favoriteSeriesIds: StateFlow<Set<String>> = preferencesManager.favoriteSeries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    // Series detail state
    private val _activeSeriesEpisodes = MutableStateFlow<List<SeriesEpisode>>(emptyList())
    val activeSeriesEpisodes: StateFlow<List<SeriesEpisode>> = _activeSeriesEpisodes

    private val _selectedSeries = MutableStateFlow<SeriesItem?>(null)
    val selectedSeries: StateFlow<SeriesItem?> = _selectedSeries

    init {
        // Check if there are active saved credentials for automatic logs
        viewModelScope.launch {
            preferencesManager.isLoggedIn.distinctUntilChanged().collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                if (loggedIn) {
                    preferencesManager.credentials.distinctUntilChanged().take(1).collect { triple ->
                        if (triple != null) {
                            activeHostState = triple.first
                            activeUsernameState = triple.second
                            activePasswordState = triple.third
                            xtreamService = XtreamClientBuilder.create(activeHostState)
                            // Load initial live categories
                            fetchTabCategories("live")
                        }
                    }
                }
            }
        }
        fetchSupabaseChannels()
        fetchYoutubeSettings()
        fetchYoutubeVideos("")
    }

    fun fetchYoutubeSettings() {
        viewModelScope.launch {
            try {
                val (enabled, key) = firebaseRepository.getYoutubeSettings()
                _isYoutubeEnabled.value = enabled
                _youtubeApiKey.value = key
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching YouTube settings on init: ${e.message}")
            }
        }
    }

    fun updateYoutubeSettings(enabled: Boolean, apiKey: String) {
        viewModelScope.launch {
            val success = firebaseRepository.saveYoutubeSettings(enabled, apiKey)
            if (success) {
                _isYoutubeEnabled.value = enabled
                _youtubeApiKey.value = apiKey
            }
        }
    }

    fun fetchYoutubeVideos(query: String = "") {
        _isYoutubeLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = _youtubeApiKey.value
                val urlString = if (query.isBlank()) {
                    "https://www.googleapis.com/youtube/v3/videos?part=snippet&chart=mostPopular&maxResults=25&regionCode=SA&key=$apiKey"
                } else {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=25&q=$encodedQuery&type=video&key=$apiKey"
                }
                
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                if (connection.responseCode == 200) {
                    val stream = connection.inputStream
                    val content = stream.bufferedReader().use { it.readText() }
                    val json = JSONObject(content)
                    val items = json.optJSONArray("items")
                    val list = mutableListOf<YouTubeVideo>()
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            val snippet = item.getJSONObject("snippet")
                            
                            val id = if (item.has("id") && item.get("id") is JSONObject) {
                                item.getJSONObject("id").optString("videoId", "")
                            } else {
                                item.optString("id", "")
                            }
                            
                            if (id.isNotEmpty()) {
                                val title = snippet.optString("title", "")
                                val description = snippet.optString("description", "")
                                val channelTitle = snippet.optString("channelTitle", "")
                                val thumbnails = snippet.optJSONObject("thumbnails")
                                val thumbnail = thumbnails?.optJSONObject("high")?.optString("url")
                                    ?: thumbnails?.optJSONObject("medium")?.optString("url")
                                    ?: ""
                                val publishedAt = snippet.optString("publishedAt", "")
                                
                                list.add(
                                    YouTubeVideo(
                                        id = id,
                                        title = title,
                                        description = description,
                                        thumbnail = thumbnail,
                                        publishedAt = publishedAt,
                                        channelTitle = channelTitle
                                    )
                                )
                            }
                        }
                    }
                    _youtubeVideos.value = list
                } else {
                    Log.e("MainViewModel", "YouTube API error response code: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching YouTube videos: ${e.message}")
            } finally {
                _isYoutubeLoading.value = false
            }
        }
    }

    private val cobaltEndpoints = listOf(
        "https://api.cobalt.tools/api/json",
        "https://cobalt.api.ryor.dev/api/json",
        "https://api.cobalt.lol/api/json",
        "https://cobalt.unlimited-space.io/api/json"
    )

    fun downloadYoutubeVideo(context: Context, video: YouTubeVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "جاري تحضير رابط تحميل الفيديو: ${video.title}", android.widget.Toast.LENGTH_SHORT).show()
                }

                var resolvedUrl = ""
                for (endpoint in cobaltEndpoints) {
                    try {
                        val connection = URL(endpoint).openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Accept", "application/json")
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.doOutput = true
                        connection.connectTimeout = 6000
                        connection.readTimeout = 6000

                        val body = JSONObject().apply {
                            put("url", "https://www.youtube.com/watch?v=${video.id}")
                            put("videoQuality", "720")
                            put("downloadMode", "auto")
                        }.toString()

                        connection.outputStream.use { os ->
                            os.write(body.toByteArray(Charsets.UTF_8))
                        }

                        if (connection.responseCode == 200) {
                            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(responseText)
                            val downloadUrl = json.optString("url", "")
                            if (downloadUrl.isNotEmpty()) {
                                resolvedUrl = downloadUrl
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Proxy endpoint failed: $endpoint - ${e.message}")
                    }
                }

                if (resolvedUrl.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        LocalDownloadManager.startDownload(
                            context = context,
                            id = "youtube_${video.id}",
                            title = video.title,
                            type = "movie",
                            imageUrl = video.thumbnail,
                            streamUrl = resolvedUrl,
                            quality = "720p"
                        )
                        android.widget.Toast.makeText(context, "بدأ التحميل محلياً بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "سيرفر التحميل مشغول حالياً، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error resolving YouTube download link: ${e.message}")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "حدث خطأ أثناء رغبة جلب رابط فيديو يوتيوب", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun playYoutubeVideo(context: Context, video: YouTubeVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "جاري تشغيل الفيديو: ${video.title}", android.widget.Toast.LENGTH_SHORT).show()
                }

                var resolvedUrl = ""
                for (endpoint in cobaltEndpoints) {
                    try {
                        val connection = URL(endpoint).openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Accept", "application/json")
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.doOutput = true
                        connection.connectTimeout = 6000
                        connection.readTimeout = 6000

                        val body = JSONObject().apply {
                            put("url", "https://www.youtube.com/watch?v=${video.id}")
                            put("videoQuality", "720")
                            put("downloadMode", "auto")
                        }.toString()

                        connection.outputStream.use { os ->
                            os.write(body.toByteArray(Charsets.UTF_8))
                        }

                        if (connection.responseCode == 200) {
                            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(responseText)
                            val downloadUrl = json.optString("url", "")
                            if (downloadUrl.isNotEmpty()) {
                                resolvedUrl = downloadUrl
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Proxy endpoint failed: $endpoint - ${e.message}")
                    }
                }

                if (resolvedUrl.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _selectedStreamName.value = video.title
                        _selectedStreamUrl.value = resolvedUrl
                        _selectedStreamQualities.value = emptyList()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _selectedStreamName.value = video.title
                        _selectedStreamUrl.value = "youtube_embed:${video.id}"
                        _selectedStreamQualities.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error resolving YouTube streaming link: ${e.message}")
                withContext(Dispatchers.Main) {
                    _selectedStreamName.value = video.title
                    _selectedStreamUrl.value = "youtube_embed:${video.id}"
                    _selectedStreamQualities.value = emptyList()
                }
            }
        }
    }

    fun login(code: String) {
        if (code.trim().isEmpty()) {
            _loginError.value = "الرجاء إدخال كود التفعيل"
            return
        }

        _isLoggingIn.value = true
        _loginError.value = null

        viewModelScope.launch {
            val credentials = firebaseRepository.fetchCredentials(code.trim())
            if (credentials != null) {
                // Save credentials locally
                preferencesManager.saveCredentials(
                    host = credentials.host,
                    username = credentials.username,
                    password = credentials.password
                )
                preferencesManager.saveDefaultLiveFormat(credentials.defaultLiveFormat)
                preferencesManager.saveDefaultVodFormat(credentials.defaultVodFormat)
                
                activeHostState = credentials.host
                activeUsernameState = credentials.username
                activePasswordState = credentials.password
                xtreamService = XtreamClientBuilder.create(activeHostState)
                
                _isLoggedIn.value = true
                _loginError.value = null
                fetchTabCategories("live")
            } else {
                _loginError.value = "كود تفعيل غير صحيح أو منتهي الصلاحية"
            }
            _isLoggingIn.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesManager.clearSession()
            xtreamService = null
            activeHostState = ""
            activeUsernameState = ""
            activePasswordState = ""
            _isLoggedIn.value = false
            _liveCategories.value = emptyList()
            _movieCategories.value = emptyList()
            _seriesCategories.value = emptyList()
            _liveStreams.value = emptyList()
            _movieStreams.value = emptyList()
            _seriesItems.value = emptyList()
            _selectedLiveCategoryId.value = null
            _selectedMovieCategoryId.value = null
            _selectedSeriesCategoryId.value = null
        }
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
        _selectedSeries.value = null
        _activeSeriesEpisodes.value = emptyList()
        viewModelScope.launch {
            if (tab == "library") {
                launch { fetchTabCategories("movies") }
                launch { fetchTabCategories("series") }
            } else if (tab == "youtube") {
                fetchYoutubeSettings()
                fetchYoutubeVideos("")
            } else {
                fetchTabCategories(tab)
            }
            if (tab == "main_channels") {
                fetchSupabaseChannels()
            }
        }
    }

    fun selectLiveCategory(categoryId: String) {
        _selectedLiveCategoryId.value = categoryId
        fetchLiveStreams(categoryId)
    }

    fun selectMovieCategory(categoryId: String) {
        _selectedMovieCategoryId.value = categoryId
        fetchMovieStreams(categoryId)
    }

    fun selectSeriesCategory(categoryId: String) {
        _selectedSeriesCategoryId.value = categoryId
        fetchSeries(categoryId)
    }

    fun setPlayerTheme(accent: String) {
        viewModelScope.launch {
            preferencesManager.saveTheme(accent)
        }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.saveAppLanguage(lang)
        }
    }

    fun setGridColumns(cols: Int) {
        viewModelScope.launch {
            preferencesManager.saveGridColumns(cols)
        }
    }

    fun setPlayerTypeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.savePlayerMode(mode)
        }
    }

    fun setDefaultLiveFormat(format: String) {
        viewModelScope.launch {
            preferencesManager.saveDefaultLiveFormat(format)
        }
    }

    fun setDefaultVodFormat(format: String) {
        viewModelScope.launch {
            preferencesManager.saveDefaultVodFormat(format)
        }
    }

    fun createServerActivationCode(
        codeName: String,
        host: String,
        username: String,
        password: String,
        defaultLiveFormat: String,
        defaultVodFormat: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = firebaseRepository.addActivationCode(
                    codeName = codeName.trim(),
                    host = host.trim(),
                    username = username.trim(),
                    password = password.trim(),
                    defaultLiveFormat = defaultLiveFormat,
                    defaultVodFormat = defaultVodFormat
                )
                onResult(res)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun setLandscapeMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveLandscapeMode(enabled)
        }
    }

    // Toggle Favorites
    fun toggleFavoriteLive(itemId: String) {
        viewModelScope.launch { preferencesManager.toggleFavoriteLive(itemId) }
    }

    fun toggleFavoriteMovie(itemId: String) {
        viewModelScope.launch { preferencesManager.toggleFavoriteMovie(itemId) }
    }

    fun toggleFavoriteSeries(itemId: String) {
        viewModelScope.launch { preferencesManager.toggleFavoriteSeries(itemId) }
    }

    // Load Live Streams of selected category
    private fun fetchLiveStreams(categoryId: String) {
        val service = xtreamService ?: return
        _isContentLoading.value = true
        _liveStreams.value = emptyList()
        viewModelScope.launch {
            try {
                val streams = if (categoryId == "all") {
                    service.getLiveStreams(
                        username = activeUsernameState,
                        password = activePasswordState
                    )
                } else {
                    service.getLiveStreamsByCategory(
                        username = activeUsernameState,
                        password = activePasswordState,
                        categoryId = categoryId
                    )
                }
                _liveStreams.value = streams
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error live streams: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    // Load VOD streams
    private fun fetchMovieStreams(categoryId: String) {
        val service = xtreamService ?: return
        _isContentLoading.value = true
        _movieStreams.value = emptyList()
        viewModelScope.launch {
            try {
                val movies = if (categoryId == "all") {
                    service.getVodStreams(
                        username = activeUsernameState,
                        password = activePasswordState
                    )
                } else {
                    service.getVodStreamsByCategory(
                        username = activeUsernameState,
                        password = activePasswordState,
                        categoryId = categoryId
                    )
                }
                _movieStreams.value = movies
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error movies streams: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    // Load Series
    private fun fetchSeries(categoryId: String) {
        val service = xtreamService ?: return
        _isContentLoading.value = true
        _seriesItems.value = emptyList()
        viewModelScope.launch {
            try {
                val series = if (categoryId == "all") {
                    service.getSeries(
                        username = activeUsernameState,
                        password = activePasswordState
                    )
                } else {
                    service.getSeriesByCategory(
                        username = activeUsernameState,
                        password = activePasswordState,
                        categoryId = categoryId
                    )
                }
                _seriesItems.value = series
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error series: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    // Open Series details and load episodes
    fun selectSeriesDetails(series: SeriesItem) {
        _selectedSeries.value = series
        _isContentLoading.value = true
        _activeSeriesEpisodes.value = emptyList()
        val service = xtreamService ?: return
        viewModelScope.launch {
            try {
                val response = service.getSeriesInfo(
                    username = activeUsernameState,
                    password = activePasswordState,
                    seriesId = series.seriesId
                )
                // Map of season -> episodes list, keeping the season key intact inside SeriesEpisode
                val episodesList = mutableListOf<SeriesEpisode>()
                response.episodes?.forEach { (seasonKey, list) ->
                    list.forEach { ep ->
                        episodesList.add(ep.copy(seasonNum = seasonKey))
                    }
                }
                _activeSeriesEpisodes.value = episodesList
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching series info: ${e.message}")
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun ensureMoviesAndSeriesLoaded() {
        val service = xtreamService ?: return
        viewModelScope.launch {
            if (_movieStreams.value.isEmpty()) {
                try {
                    val movies = service.getVodStreams(activeUsernameState, activePasswordState)
                    _movieStreams.value = movies
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error fetching movies for potentials: ${e.message}")
                }
            }
            if (_seriesItems.value.isEmpty()) {
                try {
                    val series = service.getSeries(activeUsernameState, activePasswordState)
                    _seriesItems.value = series
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error fetching series for potentials: ${e.message}")
                }
            }
        }
    }

    fun closeSeriesDetails() {
        _selectedSeries.value = null
        _activeSeriesEpisodes.value = emptyList()
    }

    // Dynamically retrieve URL format based on play settings (Smart, HLS, TS, etc.)
    fun selectLivePlayback(itemId: Int, itemName: String) {
        val cleanHost = activeHostState.trimEnd('/')
        val format = defaultLiveFormat.value
        val ext = when (format) {
            "m3u8" -> ".m3u8"
            "ts" -> ".ts"
            "Standard" -> ""
            else -> if (format.contains("m3u8")) ".m3u8" else ".ts"
        }
        val url = "$cleanHost/live/$activeUsernameState/$activePasswordState/$itemId$ext"
        _selectedStreamName.value = itemName
        _selectedStreamUrl.value = url
    }

    fun selectMoviePlayback(itemId: Int, container: String?, itemName: String) {
        val cleanHost = activeHostState.trimEnd('/')
        val format = defaultVodFormat.value
        val ext = if (format.isNotEmpty()) format else (container ?: "mp4")
        val url = "$cleanHost/movie/$activeUsernameState/$activePasswordState/$itemId.$ext"
        _selectedStreamName.value = itemName
        _selectedStreamUrl.value = url
    }

    fun selectEpisodePlayback(episodeId: String, container: String?, episodeTitle: String) {
        val cleanHost = activeHostState.trimEnd('/')
        val format = defaultVodFormat.value
        val ext = if (format.isNotEmpty()) format else (container ?: "mp4")
        val url = "$cleanHost/series/$activeUsernameState/$activePasswordState/$episodeId.$ext"
        _selectedStreamName.value = episodeTitle
        _selectedStreamUrl.value = url
    }

    fun playOfflineFile(localPath: String, title: String) {
        val fileUri = "file://$localPath"
        _selectedStreamName.value = title
        _selectedStreamUrl.value = fileUri
        _selectedStreamQualities.value = emptyList()
    }

    fun clearPlayback() {
        _selectedStreamUrl.value = null
        _selectedStreamName.value = null
        _selectedStreamQualities.value = emptyList()
        _selectedSupabasePlaybackMode.value = null
        _selectedSupabaseUserAgent.value = null
    }

    // Fetch horizontal category items for specified tab
    private suspend fun fetchTabCategories(tab: String) {
        val service = xtreamService ?: return
        when (tab) {
            "live" -> {
                if (_liveCategories.value.isNotEmpty()) return
                _isContentLoading.value = true
                try {
                    val cats = service.getLiveCategories(activeUsernameState, activePasswordState)
                    val fullCats = listOf(XtreamCategory("all", "الكل", 0)) + cats
                    _liveCategories.value = fullCats
                    if (fullCats.isNotEmpty() && _selectedLiveCategoryId.value == null) {
                        selectLiveCategory("all")
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error live categories: ${e.message}")
                } finally {
                    _isContentLoading.value = false
                }
            }
            "movies" -> {
                if (_movieCategories.value.isNotEmpty()) return
                _isContentLoading.value = true
                try {
                    val cats = service.getVodCategories(activeUsernameState, activePasswordState)
                    val fullCats = listOf(XtreamCategory("all", "الكل", 0)) + cats
                    _movieCategories.value = fullCats
                    if (fullCats.isNotEmpty() && _selectedMovieCategoryId.value == null) {
                        selectMovieCategory("all")
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error movies categories: ${e.message}")
                } finally {
                    _isContentLoading.value = false
                }
            }
            "series" -> {
                if (_seriesCategories.value.isNotEmpty()) return
                _isContentLoading.value = true
                try {
                    val cats = service.getSeriesCategories(activeUsernameState, activePasswordState)
                    val fullCats = listOf(XtreamCategory("all", "الكل", 0)) + cats
                    _seriesCategories.value = fullCats
                    if (fullCats.isNotEmpty() && _selectedSeriesCategoryId.value == null) {
                        selectSeriesCategory("all")
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error series categories: ${e.message}")
                } finally {
                    _isContentLoading.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
