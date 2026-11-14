package com.freemind.ai.domain.model

import kotlinx.serialization.Serializable

/** Extra detail fetched on demand for one place - not part of a search result. */
@Serializable
data class PlaceDetails(
    val phone: String?,
    /** International format, safe to hand to the dialler. */
    val dialNumber: String?,
    val website: String?,
    /** Google's own page for the place. */
    val mapsUrl: String?,
    /** One line per weekday, e.g. "Monday: 9:00 AM - 5:00 PM". Empty when unknown. */
    val weekdayHours: List<String>,
)
