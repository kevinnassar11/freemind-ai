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
    /** The list this favorite is filed under, or null if it isn't in one. */
    val listId: String? = null,
)

/** A named group of favorites, e.g. "Beirut food". */
data class FavoriteList(val id: String, val name: String)
