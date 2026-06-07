package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loop_live_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_accent")
        val KEY_PLAYER_MODE = stringPreferencesKey("player_mode")
        val KEY_HOST = stringPreferencesKey("host")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_FAVORITES_LIVE = stringPreferencesKey("favorites_live") // Store as JSON Array of IDs
        val KEY_FAVORITES_MOVIES = stringPreferencesKey("favorites_movies")
        val KEY_FAVORITES_SERIES = stringPreferencesKey("favorites_series")
        val KEY_LANDSCAPE_MODE = booleanPreferencesKey("is_landscape_mode")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        // VOD Specific Settings
        val KEY_VOD_PLAYBACK_SPEED = floatPreferencesKey("vod_playback_speed")
        val KEY_VOD_ASPECT_RATIO = stringPreferencesKey("vod_aspect_ratio")
        val KEY_VOD_REPEAT_MODE = booleanPreferencesKey("vod_repeat_mode")
        val KEY_VOD_VOLUME_BOOST = stringPreferencesKey("vod_volume_boost")
        
        // Smart Server format overrides via admin activation codes
        val KEY_DEFAULT_LIVE_FORMAT = stringPreferencesKey("default_live_format")
        val KEY_DEFAULT_VOD_FORMAT = stringPreferencesKey("default_vod_format")
    }

    val selectedTheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "Cyan"
    }

    val defaultLiveFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_LIVE_FORMAT] ?: "ts"
    }

    val defaultVodFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_VOD_FORMAT] ?: "mkv"
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "ar"
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_GRID_COLUMNS] ?: 3
    }

    val vodPlaybackSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_VOD_PLAYBACK_SPEED] ?: 1.0f
    }

    val vodAspectRatio: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_VOD_ASPECT_RATIO] ?: "Fit"
    }

    val vodRepeatMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_VOD_REPEAT_MODE] ?: false
    }

    val vodVolumeBoost: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_VOD_VOLUME_BOOST] ?: "Standard"
    }

    val isLandscapeMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANDSCAPE_MODE] ?: false
    }

    val playerMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PLAYER_MODE] ?: "Smart"
    }

    val credentials: Flow<Triple<String, String, String>?> = context.dataStore.data.map { preferences ->
        val host = preferences[KEY_HOST]
        val username = preferences[KEY_USERNAME]
        val password = preferences[KEY_PASSWORD]
        if (!host.isNullOrEmpty() && !username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            Triple(host, username, password)
        } else {
            null
        }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun saveLandscapeMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANDSCAPE_MODE] = enabled
        }
    }

    suspend fun savePlayerMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAYER_MODE] = mode
        }
    }

    suspend fun saveAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = lang
        }
    }

    suspend fun saveGridColumns(cols: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRID_COLUMNS] = cols
        }
    }

    suspend fun saveVodPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOD_PLAYBACK_SPEED] = speed
        }
    }

    suspend fun saveVodAspectRatio(ratio: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOD_ASPECT_RATIO] = ratio
        }
    }

    suspend fun saveVodRepeatMode(repeat: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOD_REPEAT_MODE] = repeat
        }
    }

    suspend fun saveVodVolumeBoost(boostMode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOD_VOLUME_BOOST] = boostMode
        }
    }

    suspend fun saveDefaultLiveFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_LIVE_FORMAT] = format
        }
    }

    suspend fun saveDefaultVodFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_VOD_FORMAT] = format
        }
    }

    suspend fun saveCredentials(host: String, username: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOST] = host
            preferences[KEY_USERNAME] = username
            preferences[KEY_PASSWORD] = password
            preferences[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOST] = ""
            preferences[KEY_USERNAME] = ""
            preferences[KEY_PASSWORD] = ""
            preferences[KEY_IS_LOGGED_IN] = false
        }
    }

    val favoriteLiveStreams: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        deserializeSet(preferences[KEY_FAVORITES_LIVE])
    }

    val favoriteMovies: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        deserializeSet(preferences[KEY_FAVORITES_MOVIES])
    }

    val favoriteSeries: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        deserializeSet(preferences[KEY_FAVORITES_SERIES])
    }

    suspend fun toggleFavoriteLive(id: String) {
        context.dataStore.edit { preferences ->
            val current = deserializeSet(preferences[KEY_FAVORITES_LIVE]).toMutableSet()
            if (current.contains(id)) current.remove(id) else current.add(id)
            preferences[KEY_FAVORITES_LIVE] = serializeSet(current)
        }
    }

    suspend fun toggleFavoriteMovie(id: String) {
        context.dataStore.edit { preferences ->
            val current = deserializeSet(preferences[KEY_FAVORITES_MOVIES]).toMutableSet()
            if (current.contains(id)) current.remove(id) else current.add(id)
            preferences[KEY_FAVORITES_MOVIES] = serializeSet(current)
        }
    }

    suspend fun toggleFavoriteSeries(id: String) {
        context.dataStore.edit { preferences ->
            val current = deserializeSet(preferences[KEY_FAVORITES_SERIES]).toMutableSet()
            if (current.contains(id)) current.remove(id) else current.add(id)
            preferences[KEY_FAVORITES_SERIES] = serializeSet(current)
        }
    }

    private fun serializeSet(set: Set<String>): String {
        val array = JSONArray()
        set.forEach { array.put(it) }
        return array.toString()
    }

    private fun deserializeSet(json: String?): Set<String> {
        if (json.isNullOrEmpty()) return emptySet()
        val set = mutableSetOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return set
    }
}
