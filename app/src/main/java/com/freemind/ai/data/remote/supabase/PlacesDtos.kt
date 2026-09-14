package com.freemind.ai.data.remote.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchHistoryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val query: String,
    val mode: String,
    @SerialName("result_place_id") val resultPlaceId: String? = null,
    @SerialName("result_title") val resultTitle: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("searched_at") val searchedAt: String,
)

@Serializable
data class NewPlaceSearchHistoryDto(
    @SerialName("user_id") val userId: String,
    val query: String,
    val mode: String,
    @SerialName("result_place_id") val resultPlaceId: String? = null,
    @SerialName("result_title") val resultTitle: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class FavoritePlaceDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("place_id") val placeId: String,
    val title: String,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("favorited_at") val favoritedAt: String,
)

@Serializable
data class NewFavoritePlaceDto(
    @SerialName("user_id") val userId: String,
    @SerialName("place_id") val placeId: String,
    val title: String,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class TrendingSearchDto(
    val query: String,
    @SerialName("search_count") val searchCount: Long,
)

@Serializable
data class TrendingSearchParamsDto(
    @SerialName("result_limit") val resultLimit: Int,
)
