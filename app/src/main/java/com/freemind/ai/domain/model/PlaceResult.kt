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
)
