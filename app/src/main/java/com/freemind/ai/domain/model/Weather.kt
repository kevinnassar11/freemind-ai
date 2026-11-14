package com.freemind.ai.domain.model

import java.time.LocalDate

/** One day's forecast for a trip destination. */
data class DayForecast(
    val date: LocalDate,
    val tempMaxC: Int,
    val tempMinC: Int,
    /** Chance of precipitation, 0-100. */
    val rainChance: Int,
    val kind: WeatherKind,
) {
    /** Wet enough that an outdoor stop is a poor bet. */
    val isRainy: Boolean get() = rainChance >= RAINY_THRESHOLD_PERCENT || kind == WeatherKind.STORM
}

const val RAINY_THRESHOLD_PERCENT = 50

enum class WeatherKind(val emoji: String) {
    CLEAR("☀️"),
    PARTLY_CLOUDY("⛅"),
    CLOUDY("☁️"),
    FOG("🌫️"),
    RAIN("🌧️"),
    STORM("⛈️"),
    SNOW("❄️"),
}

/** Maps a WMO weather code (what Open-Meteo reports) to a coarse kind. */
fun weatherKindOf(wmoCode: Int): WeatherKind = when (wmoCode) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.PARTLY_CLOUDY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    in 51..67, in 80..82 -> WeatherKind.RAIN
    in 71..77, 85, 86 -> WeatherKind.SNOW
    in 95..99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

/** The calendar date of [day] (1-based) in this plan, or null while no start date is set. */
fun Itinerary.dateOf(day: Int): LocalDate? =
    startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.plusDays((day - 1).toLong())
