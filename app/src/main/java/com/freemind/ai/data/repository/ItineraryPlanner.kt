package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.ItineraryDay
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.StopKind
import com.freemind.ai.domain.model.distanceMetresTo

private val VERBS: Map<StopKind, List<String>> = mapOf(
    StopKind.SIGHT to listOf("Visit", "Explore", "See", "Take in", "Stroll around"),
    StopKind.CULTURE to listOf("Browse", "Step inside", "Discover", "Tour"),
    StopKind.NATURE to listOf("Wander through", "Unwind at", "Get outdoors at", "Walk around"),
    StopKind.EAT to listOf("Lunch at", "Eat at", "Dine at", "Refuel at", "Taste the local food at"),
    StopKind.SWIM to listOf("Swim at", "Cool off at", "Relax at", "Spend the afternoon at"),
    StopKind.SHOPPING to listOf("Shop at", "Browse", "Pick up souvenirs at", "Wander through"),
    StopKind.NIGHTLIFE to listOf("Evening drinks at", "End the night at", "Head out to", "Nightcap at"),
)

/**
 * Turns pools of real places (one per interest) into day-by-day stops.
 *
 * Each day starts from an "anchor" - the next unused place from the traveller's main
 * interest, in Google's own relevance order - and every other stop that day is the
 * unused place *nearest to that anchor*, so a day stays in one part of the city
 * instead of zig-zagging across it. A place is only repeated once its whole pool has
 * been used, and never twice in one day.
 */
fun planDays(days: Int, pools: Map<StopKind, List<PlaceResult>>): List<ItineraryDay> {
    val usable = pools.filterValues { it.isNotEmpty() }
    if (usable.isEmpty() || days < 1) return emptyList()

    val anchorKind = StopKind.entries.first { it in usable }
    val used = usable.keys.associateWith { mutableSetOf<String>() }

    fun pick(kind: StopKind, near: PlaceResult?, takenToday: Set<String>): PlaceResult? {
        val pool = usable.getValue(kind)
        var candidates = pool.filter { it.id !in used.getValue(kind) && it.id !in takenToday }
        if (candidates.isEmpty()) {
            // Whole pool used up: start it over rather than leave the day empty.
            used.getValue(kind).clear()
            candidates = pool.filter { it.id !in takenToday }
        }
        val nearLat = near?.latitude
        val nearLng = near?.longitude
        val chosen = if (nearLat != null && nearLng != null) {
            val origin = DeviceLocation(nearLat, nearLng)
            candidates.minByOrNull { origin.distanceMetresTo(it.latitude, it.longitude) ?: Double.MAX_VALUE }
        } else {
            candidates.firstOrNull()
        }
        chosen?.let { used.getValue(kind).add(it.id) }
        return chosen
    }

    return (1..days).map { day ->
        val taken = mutableSetOf<String>()
        val anchor = pick(anchorKind, near = null, takenToday = taken)
        anchor?.let { taken += it.id }

        val stops = mutableListOf<ItineraryStop>()
        anchor?.let { stops += stopFor(anchorKind, it, day) }
        usable.keys.filter { it != anchorKind }.forEach { kind ->
            pick(kind, near = anchor, takenToday = taken)?.let { place ->
                taken += place.id
                stops += stopFor(kind, place, day)
            }
        }
        ItineraryDay(day, stops.sortedBy { it.kind.ordinal })
    }
}

private fun stopFor(kind: StopKind, place: PlaceResult, day: Int): ItineraryStop {
    val verbs = VERBS.getValue(kind)
    return ItineraryStop(kind, place, verbs[(day - 1 + kind.ordinal) % verbs.size])
}
