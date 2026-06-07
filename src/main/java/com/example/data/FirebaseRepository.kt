package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

data class ActivationCredentials(
    val host: String,
    val username: String,
    val password: String,
    val defaultLiveFormat: String = "ts",
    val defaultVodFormat: String = "mp4"
)

class FirebaseRepository(private val context: Context) {

    private fun initializeFirebaseIfNeeded() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBmUJGFkIPMyN2tOVk3LFe4u7ZLky6CKkQ")
                .setApplicationId("1:725987052105:android:0b0f98bbccb6c874bdcd98")
                .setProjectId("loop-7e3d9")
                .setDatabaseUrl("https://loop-7e3d9-default-rtdb.firebaseio.com")
                .build()
            FirebaseApp.initializeApp(context, options)
            Log.d("FirebaseRepository", "Dynamic FirebaseApp initialized for project: loop-7e3d9 with RTDB url")
        }
    }

    private val db: FirebaseFirestore by lazy {
        initializeFirebaseIfNeeded()
        FirebaseFirestore.getInstance()
    }

    private val rtdb: FirebaseDatabase by lazy {
        initializeFirebaseIfNeeded()
        FirebaseDatabase.getInstance("https://loop-7e3d9-default-rtdb.firebaseio.com")
    }

    suspend fun getChannels(): List<SupabaseChannel> = suspendCancellableCoroutine { continuation ->
        try {
            val ref = rtdb.getReference("main_channels")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val channelsList = mutableListOf<SupabaseChannel>()
                        for (child in snapshot.children) {
                            try {
                                val idVal = child.child("id").value
                                val id = when (idVal) {
                                    is Number -> idVal.toInt()
                                    is String -> idVal.toIntOrNull() ?: child.key?.hashCode() ?: 0
                                    else -> child.key?.hashCode() ?: 0
                                }
                                val name = child.child("name").value?.toString() ?: ""
                                val logoUrl = (child.child("logo_url").value ?: child.child("logoUrl").value)?.toString() ?: ""
                                val userAgent = (child.child("user_agent").value ?: child.child("userAgent").value)?.toString()
                                val mainStreamUrl = (child.child("main_stream_url").value ?: child.child("mainStreamUrl").value)?.toString() ?: ""
                                
                                val categoryId = child.child("category_id").value?.toString() ?: "main"
                                val sortOrderVal = child.child("sort_order").value
                                val sortOrder = when (sortOrderVal) {
                                    is Number -> sortOrderVal.toInt()
                                    is String -> sortOrderVal.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                val aspectRatio = child.child("aspect_ratio").value?.toString() ?: "1:1"
                                val cardSize = child.child("card_size").value?.toString() ?: "1x1"
                                val playbackMode = child.child("playback_mode").value?.toString() ?: "auto"
                                val firebaseKey = child.key ?: ""

                                val qualitiesList = mutableListOf<ChannelQuality>()
                                val qualitiesSnap = child.child("qualities")
                                if (qualitiesSnap.exists()) {
                                    for (qSnap in qualitiesSnap.children) {
                                        val qName = qSnap.child("name").value?.toString() ?: ""
                                        val qUrl = qSnap.child("url").value?.toString() ?: ""
                                        qualitiesList.add(ChannelQuality(qName, qUrl))
                                    }
                                }
                                
                                channelsList.add(
                                    SupabaseChannel(
                                        id = id,
                                        name = name,
                                        logoUrl = logoUrl,
                                        userAgent = userAgent,
                                        mainStreamUrl = mainStreamUrl,
                                        qualities = if (qualitiesList.isEmpty()) null else qualitiesList,
                                        categoryId = categoryId,
                                        sortOrder = sortOrder,
                                        aspectRatio = aspectRatio,
                                        cardSize = cardSize,
                                        playbackMode = playbackMode,
                                        firebaseKey = firebaseKey
                                    )
                                )
                            } catch (ex: Exception) {
                                Log.e("FirebaseRepository", "Error parsing channel from RTDB: ${ex.message}")
                            }
                        }
                        if (continuation.isActive) {
                            continuation.resume(channelsList)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseRepository", "Exception in onDataChange: ${e.message}", e)
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(error.toException())
                    }
                }
            })
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun insertChannel(channel: SupabaseChannel): Unit = suspendCancellableCoroutine { continuation ->
        try {
            val ref = rtdb.getReference("main_channels")
            val newChannelRef = ref.push()
            
            val channelMap = hashMapOf<String, Any?>(
                "id" to (channel.id ?: System.currentTimeMillis().toInt()),
                "name" to channel.name,
                "logo_url" to channel.logoUrl,
                "user_agent" to channel.userAgent,
                "main_stream_url" to channel.mainStreamUrl,
                "category_id" to (channel.categoryId ?: "main"),
                "sort_order" to (channel.sortOrder ?: 0),
                "aspect_ratio" to (channel.aspectRatio ?: "1:1"),
                "card_size" to (channel.cardSize ?: "1x1"),
                "playback_mode" to (channel.playbackMode ?: "auto")
            )
            
            if (channel.qualities != null) {
                val qualitiesMapList = channel.qualities.map {
                    hashMapOf(
                        "name" to it.name,
                        "url" to it.url
                    )
                }
                channelMap["qualities"] = qualitiesMapList
            }
            
            newChannelRef.setValue(channelMap)
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun deleteChannel(firebaseKey: String): Unit = suspendCancellableCoroutine { continuation ->
        try {
            if (firebaseKey.isBlank()) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }
            val ref = rtdb.getReference("main_channels").child(firebaseKey)
            ref.removeValue()
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resumeWithException(it)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    suspend fun updateChannel(firebaseKey: String, channel: SupabaseChannel): Unit = suspendCancellableCoroutine { continuation ->
        try {
            if (firebaseKey.isBlank()) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }
            val ref = rtdb.getReference("main_channels").child(firebaseKey)
            
            val channelMap = hashMapOf<String, Any?>(
                "id" to (channel.id ?: System.currentTimeMillis().toInt()),
                "name" to channel.name,
                "logo_url" to channel.logoUrl,
                "user_agent" to channel.userAgent,
                "main_stream_url" to channel.mainStreamUrl,
                "category_id" to (channel.categoryId ?: "main"),
                "sort_order" to (channel.sortOrder ?: 0),
                "aspect_ratio" to (channel.aspectRatio ?: "1:1"),
                "card_size" to (channel.cardSize ?: "1x1"),
                "playback_mode" to (channel.playbackMode ?: "auto")
            )
            
            if (channel.qualities != null) {
                val qualitiesMapList = channel.qualities.map {
                    hashMapOf(
                        "name" to it.name,
                        "url" to it.url
                    )
                }
                channelMap["qualities"] = qualitiesMapList
            }
            
            ref.setValue(channelMap)
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun getCategories(): List<ChannelCategory> = suspendCancellableCoroutine { continuation ->
        try {
            val ref = rtdb.getReference("main_categories")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val categoriesList = mutableListOf<ChannelCategory>()
                        for (child in snapshot.children) {
                            try {
                                val name = child.child("name").value?.toString() ?: ""
                                val logoUrl = child.child("logo_url").value?.toString() ?: ""
                                val id = child.child("id").value?.toString() ?: child.key ?: ""
                                val sortOrderVal = child.child("sort_order").value
                                val sortOrder = when (sortOrderVal) {
                                    is Number -> sortOrderVal.toInt()
                                    is String -> sortOrderVal.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                val placement = child.child("placement").value?.toString() ?: "top"
                                val firebaseKey = child.key ?: ""

                                categoriesList.add(
                                    ChannelCategory(
                                        id = id,
                                        name = name,
                                        logoUrl = logoUrl,
                                        sortOrder = sortOrder,
                                        placement = placement,
                                        firebaseKey = firebaseKey
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("FirebaseRepository", "Error parsing category: ${e.message}")
                            }
                        }
                        if (continuation.isActive) {
                            continuation.resume(categoriesList)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseRepository", "Error loading categories snapshot: ${e.message}")
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (continuation.isActive) continuation.resumeWithException(error.toException())
                }
            })
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    suspend fun insertCategory(category: ChannelCategory): Unit = suspendCancellableCoroutine { continuation ->
        try {
            val ref = rtdb.getReference("main_categories")
            val newRef = ref.push()
            val finalId = category.id ?: newRef.key ?: System.currentTimeMillis().toString()
            val categoryMap = hashMapOf<String, Any?>(
                "id" to finalId,
                "name" to category.name,
                "logo_url" to category.logoUrl,
                "sort_order" to (category.sortOrder ?: 0),
                "placement" to (category.placement ?: "top")
            )
            newRef.setValue(categoryMap)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resumeWithException(it)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    suspend fun deleteCategory(firebaseKey: String): Unit = suspendCancellableCoroutine { continuation ->
        try {
            if (firebaseKey.isBlank()) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }
            val ref = rtdb.getReference("main_categories").child(firebaseKey)
            ref.removeValue()
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resumeWithException(it)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    suspend fun fetchCredentials(code: String): ActivationCredentials? = suspendCancellableCoroutine { continuation ->
        try {
            db.collection("activation_codes").document(code).get()
                .addOnSuccessListener { document ->
                    if (continuation.isActive) {
                        if (document != null && document.exists()) {
                            val host = document.getString("host") ?: ""
                            val username = document.getString("username") ?: ""
                            val password = document.getString("password") ?: ""
                            val defaultLive = document.getString("default_live_format") ?: "ts"
                            val defaultVod = document.getString("default_vod_format") ?: "mp4"
                            
                            Log.d("FirebaseRepository", "Found credentials for code: $code")
                            if (host.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                                continuation.resume(ActivationCredentials(host, username, password, defaultLive, defaultVod))
                            } else {
                                continuation.resume(null)
                            }
                        } else {
                            Log.w("FirebaseRepository", "No collection document matches code: $code")
                            continuation.resume(null)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        Log.e("FirebaseRepository", "FirebaseFirestore retrieval error: ${exception.message}", exception)
                        continuation.resume(null)
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                Log.e("FirebaseRepository", "Fatal exception in FirebaseRepository: ${e.message}", e)
                continuation.resume(null)
            }
        }
    }

    suspend fun addActivationCode(
        codeName: String,
        host: String,
        username: String,
        password: String,
        defaultLiveFormat: String,
        defaultVodFormat: String
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            val data = mapOf(
                "code_name" to codeName,
                "host" to host,
                "username" to username,
                "password" to password,
                "default_live_format" to defaultLiveFormat,
                "default_vod_format" to defaultVodFormat
            )
            initializeFirebaseIfNeeded()
            db.collection("activation_codes").document(codeName).set(data)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(Result.failure(it))
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(Result.failure(e))
        }
    }

    suspend fun getOnlineUsersCount(): Int = suspendCancellableCoroutine { continuation ->
        try {
            db.collection("online_users").count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener { aggregateQuerySnapshot ->
                    if (continuation.isActive) {
                        val count = aggregateQuerySnapshot.count.toInt()
                        continuation.resume(count)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        Log.e("FirebaseRepository", "Error getting online users aggregate count: ${exception.message}", exception)
                        continuation.resume(0)
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                Log.e("FirebaseRepository", "Exception in getOnlineUsersCount: ${e.message}", e)
                continuation.resume(0)
            }
        }
    }

    fun registerOnlineUser(androidId: String) {
        try {
            initializeFirebaseIfNeeded()
            val data = mapOf("status" to "online")
            db.collection("online_users").document(androidId).set(data)
                .addOnSuccessListener {
                    Log.d("FirebaseRepository", "Successfully registered device as online: $androidId")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Failed to register active device: $androidId", e)
                }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Exception during registerOnlineUser: ${e.message}", e)
        }
    }

    fun unregisterOnlineUser(androidId: String) {
        try {
            initializeFirebaseIfNeeded()
            db.collection("online_users").document(androidId).delete()
                .addOnSuccessListener {
                    Log.d("FirebaseRepository", "Successfully unregistered device: $androidId")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseRepository", "Failed to unregister active device: $androidId", e)
                }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Exception during unregisterOnlineUser: ${e.message}", e)
        }
    }

    fun listenOnlineUsersCount(): Flow<Int> = callbackFlow {
        initializeFirebaseIfNeeded()
        val listener = db.collection("online_users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Firestore online count listening error: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val count = snapshot.size()
                    trySend(count)
                }
            }
        awaitClose {
            listener.remove()
        }
    }

    suspend fun getYoutubeSettings(): Pair<Boolean, String> = suspendCancellableCoroutine { continuation ->
        try {
            initializeFirebaseIfNeeded()
            val ref = rtdb.getReference("youtube_settings")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isEnabled = snapshot.child("isEnabled").value as? Boolean ?: true
                    val apiKey = snapshot.child("apiKey").value as? String ?: "AIzaSyAiMHQnWOt9tOtqlpddmIPPgwN0GUbtmAM"
                    if (continuation.isActive) continuation.resume(Pair(isEnabled, apiKey))
                }
                override fun onCancelled(error: DatabaseError) {
                    if (continuation.isActive) continuation.resume(Pair(true, "AIzaSyAiMHQnWOt9tOtqlpddmIPPgwN0GUbtmAM"))
                }
            })
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(Pair(true, "AIzaSyAiMHQnWOt9tOtqlpddmIPPgwN0GUbtmAM"))
        }
    }

    suspend fun saveYoutubeSettings(isEnabled: Boolean, apiKey: String): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            initializeFirebaseIfNeeded()
            val ref = rtdb.getReference("youtube_settings")
            val data = mapOf("isEnabled" to isEnabled, "apiKey" to apiKey)
            ref.setValue(data)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(true) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(false) }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(false)
        }
    }
}
