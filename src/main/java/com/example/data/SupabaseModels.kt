package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseChannel(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = "",
    @Json(name = "logo_url") val logoUrl: String? = "",
    @Json(name = "user_agent") val userAgent: String? = null,
    @Json(name = "main_stream_url") val mainStreamUrl: String? = "",
    @Json(name = "qualities") val qualities: List<ChannelQuality>? = null,
    @Json(name = "category_id") val categoryId: String? = "main",
    @Json(name = "sort_order") val sortOrder: Int? = 0,
    @Json(name = "aspect_ratio") val aspectRatio: String? = "1:1",
    @Json(name = "card_size") val cardSize: String? = "1x1",
    @Json(name = "playback_mode") val playbackMode: String? = "auto",
    @Json(name = "firebase_key") val firebaseKey: String? = ""
)

@JsonClass(generateAdapter = true)
data class ChannelQuality(
    @Json(name = "name") val name: String? = "",
    @Json(name = "url") val url: String? = ""
)

@JsonClass(generateAdapter = true)
data class ChannelCategory(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = "",
    @Json(name = "logo_url") val logoUrl: String? = "",
    @Json(name = "sort_order") val sortOrder: Int? = 0,
    @Json(name = "placement") val placement: String? = "top",
    @Json(name = "firebase_key") val firebaseKey: String? = ""
)
