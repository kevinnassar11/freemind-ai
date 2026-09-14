package com.freemind.ai.data.remote.supabase

import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.domain.model.PlaceSearchMode
import kotlin.time.Instant

fun PlaceSearchHistoryDto.toDomain() = PlaceSearchHistoryEntry(
    id = id,
    query = query,
    mode = PlaceSearchMode.entries.firstOrNull { it.name.equals(mode, ignoreCase = true) } ?: PlaceSearchMode.PROMPT,
    resultPlaceId = resultPlaceId,
    resultTitle = resultTitle,
    latitude = latitude,
    longitude = longitude,
    searchedAt = Instant.parse(searchedAt),
)

fun FavoritePlaceDto.toDomain() = FavoritePlace(
    id = id,
    placeId = placeId,
    title = title,
    description = description ?: "",
    latitude = latitude,
    longitude = longitude,
    favoritedAt = Instant.parse(favoritedAt),
)
