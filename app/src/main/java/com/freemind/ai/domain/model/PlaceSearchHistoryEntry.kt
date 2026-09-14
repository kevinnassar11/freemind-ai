package com.freemind.ai.domain.model

import kotlin.time.Instant

data class PlaceSearchHistoryEntry(
    val id: String,
    val query: String,
    val mode: PlaceSearchMode,
    val resultPlaceId: String?,
    val resultTitle: String?,
    val latitude: Double?,
    val longitude: Double?,
    val searchedAt: Instant,
)
