package com.freemind.ai.domain.model

import com.freemind.ai.R

/**
 * How the Places tab interprets what the user typed.
 *
 * PROMPT passes the text through verbatim (free-form search). The other three
 * constrain the query via the Places API's `type` parameter, so "Paris" in
 * CITIES mode returns the city rather than every business named Paris.
 */
enum class PlaceSearchMode(
    val labelRes: Int,
    /** Places API `type` filter; null means no type constraint. */
    val placeType: String?,
    val hintRes: Int,
) {
    PROMPT(R.string.mode_prompt, null, R.string.maps_hint_prompt),
    CITIES(R.string.mode_cities, "locality", R.string.maps_hint_cities),
    COUNTRIES(R.string.mode_countries, "country", R.string.maps_hint_countries),
    PLACES(R.string.mode_places, "point_of_interest", R.string.maps_hint_places),
    FOOD(R.string.mode_food, "restaurant", R.string.maps_hint_food),
    CULTURE(R.string.mode_culture, "museum", R.string.maps_hint_culture),
    SHOPPING(R.string.mode_shopping, "shopping_mall", R.string.maps_hint_shopping),
}
