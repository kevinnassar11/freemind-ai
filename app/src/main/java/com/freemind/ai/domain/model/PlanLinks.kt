package com.freemind.ai.domain.model

import java.util.Locale

/** Google Maps allows at most 9 stops between origin and destination in a directions link. */
private const val MAX_WAYPOINTS = 9

private fun PlaceResult.coordinates(): String? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return String.format(Locale.US, "%.6f,%.6f", lat, lng)
}

/** A link that opens one place in Google Maps. */
fun PlaceResult.mapsLink(): String {
    val where = coordinates()
    return if (where != null) {
        "https://www.google.com/maps/search/?api=1&query=$where&query_place_id=$id"
    } else {
        "https://www.google.com/maps/search/?api=1&query=${java.net.URLEncoder.encode(title, "UTF-8")}"
    }
}

/**
 * A single Google Maps link that lays out a whole day as a walking route through its
 * stops in order - tapping it opens turn-by-turn directions, no app install needed on
 * the receiving end. Null when fewer than one stop has coordinates.
 */
fun ItineraryDay.routeLink(): String? {
    val points = stops.mapNotNull { it.place.coordinates() }
    if (points.isEmpty()) return null
    if (points.size == 1) return "https://www.google.com/maps/search/?api=1&query=${points.first()}"
    val waypoints = points.drop(1).dropLast(1).take(MAX_WAYPOINTS)
    return buildString {
        append("https://www.google.com/maps/dir/?api=1&travelmode=walking")
        append("&origin=").append(points.first())
        append("&destination=").append(points.last())
        if (waypoints.isNotEmpty()) append("&waypoints=").append(waypoints.joinToString("%7C"))
    }
}
