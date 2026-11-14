package com.freemind.ai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceResult(
    val id: String,
    val title: String,
    val description: String,
    val latitude: Double?,
    val longitude: Double?,
    /** Raw Google Places `types` for this result - used to classify a query
     *  (city vs. country vs. specific place) before it's run in a mode that
     *  only accepts one of those. Empty for results that predate this field
     *  (e.g. an older cached entry). */
    val types: List<String> = emptyList(),
    /** 1.0-5.0 Google rating, when the place has one. Null for results from before
     *  this field existed (an older cached entry) or places nobody has rated. */
    val rating: Double? = null,
    val ratingCount: Int? = null,
    /** Whether the place is open right now, per Google; null when it doesn't say. */
    val openNow: Boolean? = null,
    /** Google's 0 (free) to 4 (very expensive) price band; null when unknown. */
    val priceLevel: Int? = null,
    /** Reference for the Places Photo endpoint; null when the place has no photo. */
    val photoReference: String? = null,
)
