package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "parent_id") val parentId: Int? = 0
)

@JsonClass(generateAdapter = true)
data class LiveStreamItem(
    @Json(name = "num") val num: Int? = 0,
    @Json(name = "name") val name: String,
    @Json(name = "stream_id") val streamId: Int,
    @Json(name = "stream_icon") val streamIcon: String? = "",
    @Json(name = "category_id") val categoryId: String? = ""
)

@JsonClass(generateAdapter = true)
data class MovieStreamItem(
    @Json(name = "num") val num: Int? = 0,
    @Json(name = "name") val name: String,
    @Json(name = "stream_id") val streamId: Int,
    @Json(name = "stream_icon") val streamIcon: String? = "",
    @Json(name = "category_id") val categoryId: String? = "",
    @Json(name = "container_extension") val containerExtension: String? = "mp4"
)

@JsonClass(generateAdapter = true)
data class SeriesItem(
    @Json(name = "num") val num: Int? = 0,
    @Json(name = "name") val name: String,
    @Json(name = "series_id") val seriesId: Int,
    @Json(name = "cover") val cover: String? = "",
    @Json(name = "category_id") val categoryId: String? = ""
)

// Series details query has a complex response containing seasons and episodes
@JsonClass(generateAdapter = true)
data class SeriesEpisode(
    @Json(name = "id") val id: String,
    @Json(name = "episode_num") val episodeNum: String? = "1",
    @Json(name = "title") val title: String? = "",
    @Json(name = "container_extension") val containerExtension: String? = "mp4",
    val seasonNum: String? = "1"
)

// Wrapper response for series detail (simplified)
@JsonClass(generateAdapter = true)
data class SeriesInfoResponse(
    @Json(name = "episodes") val episodes: Map<String, List<SeriesEpisode>>? = emptyMap()
)
