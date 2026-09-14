package com.freemind.ai.domain.model

import kotlin.time.Instant

data class FavoritePlace(
    val id: String,
    val placeId: String,
    val title: String,
    val description: String,
    val latitude: Double?,
    val longitude: Double?,
    val favoritedAt: Instant,
)
