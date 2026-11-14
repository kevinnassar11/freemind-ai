package com.freemind.ai.domain.model

/**
 * Client-side filters over a search's results. Places Text Search can't filter on
 * these itself, and applying them locally means toggling one costs no API call and
 * works on offline cached results too.
 *
 * A place that doesn't report the attribute being filtered on (no rating, no hours)
 * is excluded while that filter is on - "open now" should never include a place
 * Google can't say is open.
 */
data class PlaceFilters(
    val openNow: Boolean = false,
    val minRating: Double? = null,
    /** Highest acceptable Google price level (0-4); null means no price filter. */
    val maxPriceLevel: Int? = null,
) {
    val isActive: Boolean get() = openNow || minRating != null || maxPriceLevel != null

    fun matches(place: PlaceResult): Boolean {
        if (openNow && place.openNow != true) return false
        if (minRating != null && (place.rating ?: 0.0) < minRating) return false
        if (maxPriceLevel != null) {
            val price = place.priceLevel ?: return false
            if (price > maxPriceLevel) return false
        }
        return true
    }

    fun apply(places: List<PlaceResult>): List<PlaceResult> =
        if (isActive) places.filter(::matches) else places
}
