package com.freemind.ai.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Where the device is, used to bias searches and to measure how far results are. */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
)

private const val EARTH_RADIUS_METRES = 6_371_000.0

/**
 * Straight-line distance in metres from [this] to the given point, or null when the
 * place has no coordinates. Haversine - a walking/driving distance would need a
 * routing call per result, and this is only used to sort and to label rows.
 */
fun DeviceLocation.distanceMetresTo(latitude: Double?, longitude: Double?): Double? {
    if (latitude == null || longitude == null) return null
    val dLat = Math.toRadians(latitude - this.latitude)
    val dLon = Math.toRadians(longitude - this.longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(this.latitude)) * cos(Math.toRadians(latitude)) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METRES * asin(sqrt(a))
}
