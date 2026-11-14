package com.freemind.ai.domain.model

import kotlinx.serialization.Serializable

/** What a stop in a day is for. Declaration order is also the order stops are
 *  scheduled within a day (see [slot]). [CUSTOM] is a place the traveller added
 *  themselves; it is never produced by the planner. */
@Serializable
enum class StopKind(val slot: DaySlot) {
    SIGHT(DaySlot.MORNING),
    CULTURE(DaySlot.MORNING),
    NATURE(DaySlot.MORNING),
    EAT(DaySlot.MIDDAY),
    SWIM(DaySlot.AFTERNOON),
    SHOPPING(DaySlot.AFTERNOON),
    NIGHTLIFE(DaySlot.EVENING),
    CUSTOM(DaySlot.ANYTIME),
}

/** Declaration order is the order a day runs in; [ANYTIME] sorts last. */
@Serializable
enum class DaySlot { MORNING, MIDDAY, AFTERNOON, EVENING, ANYTIME }

@Serializable
data class ItineraryStop(
    val kind: StopKind,
    val place: PlaceResult,
    /** The lead-in word(s) shown before the place name, e.g. "Lunch at" - rotated
     *  across days so a long plan doesn't repeat the same phrasing. Blank for stops
     *  the traveller added. */
    val verb: String,
    /** The traveller's own note for this stop - a booking time, a reference number. */
    val note: String = "",
    /** The time of day the traveller chose for a stop they added. Null for planner
     *  stops (their [kind] decides) and for custom stops saved before this existed. */
    val slot: DaySlot? = null,
) {
    /** When in the day this stop happens. */
    val timeSlot: DaySlot
        get() = slot ?: kind.slot
}

@Serializable
data class ItineraryDay(val day: Int, val stops: List<ItineraryStop>)

/** One line of a trip's packing / to-do list. */
@Serializable
data class ChecklistItem(val text: String, val done: Boolean = false)

/**
 * A trip plan built from real Places results: [days] of stops, plus the destination
 * itself (so the map can recenter there) and the prompt it came from.
 *
 * Everything the traveller edits afterwards lives here too, so a saved plan carries
 * it: the [startDate] the trip begins on, trip-wide [notes], a [checklist], and the
 * [alternatives] - places found for the trip but not used - that "swap" draws on.
 */
@Serializable
data class Itinerary(
    val destinationName: String,
    val prompt: String,
    val days: List<ItineraryDay>,
    val destination: PlaceResult?,
    /** ISO date (yyyy-MM-dd) of day 1; null until the traveller picks one. */
    val startDate: String? = null,
    val notes: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
    /** Unused places found for the trip, keyed by [StopKind] name. */
    val alternatives: Map<String, List<PlaceResult>> = emptyMap(),
) {
    /** Every distinct place in the plan, in visiting order - what the map marks. */
    val places: List<PlaceResult>
        get() = days.flatMap { day -> day.stops.map { it.place } }.distinctBy { it.id }
}

/** "4.6 stars (1,234)" - null when the place has no rating. */
fun PlaceResult.ratingLabel(): String? {
    val r = rating ?: return null
    val count = ratingCount?.let { " (%,d)".format(it) }.orEmpty()
    return "%.1f★$count".format(r)
}

/** "$", "$$"... for Google's 1-4 price levels; null for free/unknown. */
fun PlaceResult.priceLabel(): String? = priceLevel?.takeIf { it in 1..4 }?.let { "$".repeat(it) }
