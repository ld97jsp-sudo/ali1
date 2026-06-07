package com.example.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class LocalDownloadTask(
    val id: String,
    val title: String,
    val type: String, // "movie" or "series"
    val imageUrl: String?,
    val streamUrl: String,
    val selectedQuality: String,
    val localPath: String,
    val progress: Float, // 0f to 100f
    val speedMbps: Double, // Speed in MB/s
    val status: String, // "PENDING", "DOWNLOADING", "COMPLETED", "FAILED"
    val totalSizeMb: Float,
    val downloadedSizeMb: Float,
    val errorMessage: String? = null
)

object LocalDownloadManager {
    private const val TAG = "LocalDownloadManager"
    private const val PREFS_NAME = "local_downloads_prefs"
    private const val KEY_METADATA = "downloads_metadata"

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<List<LocalDownloadTask>>(emptyList())
    val tasks = _tasks.asStateFlow()

    fun init(context: Context) {
        loadMetadata(context)
    }

    private fun getStorageDir(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val mediaDir = File(dir, "LoopDownloads")
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        return mediaDir
    }

    @Synchronized
    private fun loadMetadata(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_METADATA, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<LocalDownloadTask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LocalDownloadTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = obj.getString("type"),
                        imageUrl = obj.optString("imageUrl", null),
                        streamUrl = obj.getString("streamUrl"),
                        selectedQuality = obj.getString("selectedQuality"),
                        localPath = obj.getString("localPath"),
                        progress = obj.optDouble("progress", 100.0).toFloat(),
                        speedMbps = 0.0,
                        status = obj.getString("status"),
                        totalSizeMb = obj.optDouble("totalSizeMb", 0.0).toFloat(),
                        downloadedSizeMb = obj.optDouble("downloadedSizeMb", 0.0).toFloat(),
                        errorMessage = obj.optString("errorMessage", null)
                    )
                )
            }
            _tasks.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading download metadata", e)
        }
    }

    @Synchronized
    private fun saveMetadata(context: Context) {
        try {
            val array = JSONArray()
            _tasks.value.forEach { task ->
                // Don't save active temporary speeds
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("type", task.type)
                    put("imageUrl", task.imageUrl ?: "")
                    put("streamUrl", task.streamUrl)
                    put("selectedQuality", task.selectedQuality)
                    put("localPath", task.localPath)
                    put("progress", task.progress.toDouble())
                    put("status", task.status)
                    put("totalSizeMb", task.totalSizeMb.toDouble())
                    put("downloadedSizeMb", task.downloadedSizeMb.toDouble())
                    put("errorMessage", task.errorMessage ?: "")
                }
                array.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_METADATA, array.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving download metadata", e)
        }
    }

    fun startDownload(
        context: Context,
        id: String,
        title: String,
        type: String,
        imageUrl: String?,
        streamUrl: String,
        quality: String
    ) {
        // Cancel existing job with same id if any
        cancelDownload(id)

        val cleanTitle = title.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        val fileExt = if (streamUrl.contains(".m3u8")) "ts" else {
            val lastPart = streamUrl.substringAfterLast("?", "").substringBeforeLast("#", "")
            val ext = if (lastPart.contains(".")) lastPart.substringAfterLast(".") else "mp4"
            if (ext.length in 2..4) ext else "mp4"
        }
        val targetFile = File(getStorageDir(context), "${type}_${id}_${quality}_$cleanTitle.$fileExt")

        val newTask = LocalDownloadTask(
            id = id,
            title = title,
            type = type,
            imageUrl = imageUrl,
            streamUrl = streamUrl,
            selectedQuality = quality,
            localPath = targetFile.absolutePath,
            progress = 0f,
            speedMbps = 0.0,
            status = "PENDING",
            totalSizeMb = 0f,
            downloadedSizeMb = 0f
        )

        updateTaskInList(newTask)
        saveMetadata(context)

        val job = coroutineScope.launch {
            try {
                downloadFileWorker(context, id, streamUrl, targetFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading task: $id", e)
                updateTaskStatus(id, "FAILED", errorMessage = e.localizedMessage ?: "Unknown Error")
                saveMetadata(context)
            }
        }
        activeJobs[id] = job
    }

    private suspend fun downloadFileWorker(
        context: Context,
        taskId: String,
        sourceUrl: String,
        destinationFile: File
    ) = withContext(Dispatchers.IO) {
        updateTaskStatus(taskId, "DOWNLOADING")
        
        Log.d(TAG, "Attempting primary download with Android DownloadManager for task: $taskId")
        val dmResult = downloadWithDownloadManager(context, taskId, sourceUrl, destinationFile)
        
        if (!dmResult) {
            if (!isActive) {
                Log.d(TAG, "Task cancelled, skipping fallback")
                return@withContext
            }
            Log.w(TAG, "Android DownloadManager failed. Fallback to HttpURLConnection with 60-second timeouts... task: $taskId")
            val fallbackResult = downloadWithHttpURLConnectionFallback(context, taskId, sourceUrl, destinationFile)
            if (!fallbackResult) {
                throw Exception("All download backends failed. Please check Server availability.")
            }
        }
    }

    private suspend fun downloadWithDownloadManager(
        context: Context,
        taskId: String,
        sourceUrl: String,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        try {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            
            val request = DownloadManager.Request(Uri.parse(sourceUrl))
                .setTitle(destinationFile.name)
                .setDescription("Downloading file (Loop Live PRO)")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                
            val downloadId = downloadManager.enqueue(request)
            var downloading = true
            var isSuccessful = false
            
            var lastUpdateMillis = System.currentTimeMillis()
            var lastBytes: Long = 0
            
            while (downloading && isActive) {
                delay(500) // Poll progress every 500ms safely
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    
                    val progress = if (bytesTotal > 0) (bytesDownloaded * 100f / bytesTotal) else 0f
                    val currentMillis = System.currentTimeMillis()
                    val duration = currentMillis - lastUpdateMillis
                    val speedMbps = if (duration > 0) {
                        val downloadedSinceLast = bytesDownloaded - lastBytes
                        ((downloadedSinceLast / (1024.0 * 1024.0)) / (duration / 1000.0))
                    } else 0.0
                    
                    lastBytes = bytesDownloaded
                    lastUpdateMillis = currentMillis
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            isSuccessful = true
                            val finalSizeMb = destinationFile.length() / (1024f * 1024f)
                            _tasks.value = _tasks.value.map { task ->
                                if (task.id == taskId) {
                                    task.copy(
                                        progress = 100f,
                                        speedMbps = 0.0,
                                        status = "COMPLETED",
                                        totalSizeMb = finalSizeMb,
                                        downloadedSizeMb = finalSizeMb
                                    )
                                } else task
                            }
                            saveMetadata(context)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            Log.e(TAG, "DownloadManager failed with reason: $reason")
                            downloading = false
                            isSuccessful = false
                        }
                        DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                            // Waiting or paused state
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            _tasks.value = _tasks.value.map { task ->
                                if (task.id == taskId) {
                                    task.copy(
                                        progress = progress,
                                        speedMbps = speedMbps,
                                        totalSizeMb = if (bytesTotal > 0) bytesTotal / (1024f * 1024f) else 0f,
                                        downloadedSizeMb = bytesDownloaded / (1024f * 1024f),
                                        status = "DOWNLOADING"
                                    )
                                } else task
                            }
                        }
                    }
                    cursor.close()
                } else {
                    downloading = false
                }
            }
            
            if (!isActive) {
                downloadManager.remove(downloadId)
                destinationFile.delete()
                isSuccessful = false
            }
            
            return@withContext isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager exception: ${e.message}", e)
            return@withContext false
        }
    }

    private suspend fun downloadWithHttpURLConnectionFallback(
        context: Context,
        taskId: String,
        sourceUrl: String,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: BufferedInputStream? = null
        var outputStream: FileOutputStream? = null
        var isSuccessful = false

        try {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val url = URL(sourceUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 60000 // 60 seconds (Robust time span)
            connection.readTimeout = 60000    // 60 seconds (Robust time span)
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw Exception("HttpURLConnection response code: $responseCode")
            }

            val fileLength = connection.contentLengthLong
            val totalSizeMb = if (fileLength > 0) fileLength / (1024f * 1024f) else 0f

            inputStream = BufferedInputStream(connection.inputStream)
            outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(128 * 1024) // 128KB buffer
            var bytesRead: Int
            var totalBytesDownloaded: Long = 0
            
            var lastUpdateMillis = System.currentTimeMillis()
            var downloadedSinceLastUpdate: Long = 0

            while (isActive) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                outputStream.write(buffer, 0, bytesRead)
                totalBytesDownloaded += bytesRead
                downloadedSinceLastUpdate += bytesRead

                val currentMillis = System.currentTimeMillis()
                val duration = currentMillis - lastUpdateMillis
                
                if (duration >= 400) {
                    val progress = if (fileLength > 0) {
                        (totalBytesDownloaded * 100f / fileLength)
                    } else {
                        0f
                    }
                    
                    val dlMb = totalBytesDownloaded / (1024f * 1024f)
                    val speedMbps = (downloadedSinceLastUpdate / (1024.0 * 1024.0)) / (duration / 1000.0)

                    _tasks.value = _tasks.value.map { task ->
                        if (task.id == taskId) {
                            task.copy(
                                progress = progress,
                                speedMbps = speedMbps,
                                totalSizeMb = totalSizeMb,
                                downloadedSizeMb = dlMb
                            )
                        } else task
                    }

                    downloadedSinceLastUpdate = 0
                    lastUpdateMillis = currentMillis
                }
            }

            if (!isActive) {
                destinationFile.delete()
                updateTaskStatus(taskId, "FAILED", errorMessage = "Cancelled")
            } else {
                val finalSizeMb = destinationFile.length() / (1024f * 1024f)
                _tasks.value = _tasks.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            progress = 100f,
                            speedMbps = 0.0,
                            status = "COMPLETED",
                            totalSizeMb = if (totalSizeMb > 0) totalSizeMb else finalSizeMb,
                            downloadedSizeMb = finalSizeMb
                        )
                    } else task
                }
                saveMetadata(context)
                isSuccessful = true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fallback download error: ${e.message}", e)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            updateTaskStatus(taskId, "FAILED", errorMessage = e.localizedMessage ?: "Unknown Fallback Error")
            isSuccessful = false
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                // ignore
            }
        }
        return@withContext isSuccessful
    }

    fun cancelDownload(id: String) {
        val job = activeJobs.remove(id)
        if (job != null) {
            job.cancel()
        }
        _tasks.value = _tasks.value.map { task ->
            if (task.id == id && task.status != "COMPLETED") {
                task.copy(status = "FAILED", speedMbps = 0.0, errorMessage = "Cancelled")
            } else task
        }
    }

    fun deleteDownload(context: Context, id: String) {
        cancelDownload(id)
        val task = _tasks.value.find { it.id == id }
        if (task != null) {
            val file = File(task.localPath)
            if (file.exists()) {
                file.delete()
            }
        }
        _tasks.value = _tasks.value.filter { it.id != id }
        saveMetadata(context)
    }

    private fun updateTaskInList(task: LocalDownloadTask) {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
        } else {
            current.add(task)
        }
        _tasks.value = current
    }

    private fun updateTaskStatus(id: String, status: String, errorMessage: String? = null) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == id) {
                task.copy(status = status, speedMbps = 0.0, errorMessage = errorMessage)
            } else task
        }
    }

    /**
     * Probes actual URL sizes and qualities in a very safe manner without blocks.
     */
    suspend fun probeStreamInfo(streamUrl: String): Pair<Long, String> = withContext(Dispatchers.IO) {
        var size: Long = -1
        var detectedQuality = "1080p FHD"
        
        val lowerUrl = streamUrl.lowercase()
        var foundQuality = false
        
        when {
            lowerUrl.contains("4k") || lowerUrl.contains("uhd") || lowerUrl.contains("2160p") || lowerUrl.contains("2160") -> {
                detectedQuality = "4K Ultra HD"
                foundQuality = true
            }
            lowerUrl.contains("1080p") || lowerUrl.contains("1080") || lowerUrl.contains("fhd") || lowerUrl.contains("1080i") -> {
                detectedQuality = "1080p FHD"
                foundQuality = true
            }
            lowerUrl.contains("720p") || lowerUrl.contains("720") || lowerUrl.contains("hd") -> {
                detectedQuality = "720p HD"
                foundQuality = true
            }
            lowerUrl.contains("480p") || lowerUrl.contains("sd") || lowerUrl.contains("480") -> {
                detectedQuality = "480p SD"
                foundQuality = true
            }
            lowerUrl.contains("360p") || lowerUrl.contains("360") -> {
                detectedQuality = "360p SD"
                foundQuality = true
            }
        }
        
        try {
            val url = URL(streamUrl)
            var connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connect()
            
            var responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.disconnect()
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connection.connect()
                responseCode = connection.responseCode
            }
            
            if (responseCode in 200..299) {
                size = connection.contentLengthLong
                
                if (!foundQuality && size > 1) {
                    val sizeMb = size / (1024.0 * 1024.0)
                    detectedQuality = when {
                        sizeMb > 3000.0 -> "4K Ultra HD"
                        sizeMb > 1200.0 -> "1080p FHD"
                        sizeMb > 500.0 -> "720p HD"
                        else -> "480p SD"
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error probing stream URL: $streamUrl", e)
        }
        return@withContext Pair(size, detectedQuality)
    }
}
