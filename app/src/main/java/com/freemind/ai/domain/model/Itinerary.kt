package com.freemind.ai.domain.model

import kotlinx.serialization.Serializable

/** What a stop in a day is for. Declaration order is also the order stops are
 *  scheduled within a day (see [slot]). */
@Serializable
enum class StopKind(val slot: DaySlot) {
    SIGHT(DaySlot.MORNING),
    CULTURE(DaySlot.MORNING),
    NATURE(DaySlot.MORNING),
    EAT(DaySlot.MIDDAY),
    SWIM(DaySlot.AFTERNOON),
    SHOPPING(DaySlot.AFTERNOON),
    NIGHTLIFE(DaySlot.EVENING),
}

enum class DaySlot { MORNING, MIDDAY, AFTERNOON, EVENING }

@Serializable
data class ItineraryStop(
    val kind: StopKind,
    val place: PlaceResult,
    /** The lead-in word(s) shown before the place name, e.g. "Lunch at" - rotated
     *  across days so a long plan doesn't repeat the same phrasing. */
    val verb: String,
)

@Serializable
data class ItineraryDay(val day: Int, val stops: List<ItineraryStop>)

/** A trip plan built from real Places results: [days] of stops, plus the destination
 *  itself (so the map can recenter there) and the prompt it came from. */
@Serializable
data class Itinerary(
    val destinationName: String,
    val prompt: String,
    val days: List<ItineraryDay>,
    val destination: PlaceResult?,
) {
    /** Every distinct place in the plan, in visiting order - what the map marks. */
    val places: List<PlaceResult>
        get() = days.flatMap { day -> day.stops.map { it.place } }.distinctBy { it.id }

    /** Plain-text version for sharing or copying. */
    fun toShareText(): String = buildString {
        append("${days.size}-day plan for $destinationName")
        days.forEach { day ->
            append("\n\nDay ${day.day}")
            day.stops.forEach { stop ->
                append("\n- ${stop.verb} ${stop.place.title}")
                stop.place.ratingLabel()?.let { append(" ($it)") }
                if (stop.place.description.isNotBlank()) append(" - ${stop.place.description}")
            }
        }
        append("\n\nPlanned with FreeMind AI")
    }
}

/** "4.6 stars (1,234)" - null when the place has no rating. */
fun PlaceResult.ratingLabel(): String? {
    val r = rating ?: return null
    val count = ratingCount?.let { " (%,d)".format(it) }.orEmpty()
    return "%.1f★$count".format(r)
}

/** "$", "$$"... for Google's 1-4 price levels; null for free/unknown. */
fun PlaceResult.priceLabel(): String? = priceLevel?.takeIf { it in 1..4 }?.let { "$".repeat(it) }
