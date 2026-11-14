package com.freemind.ai.domain.model

/*
 * Pure edits over an [Itinerary]. Every function returns a new plan - or the same
 * instance when the edit doesn't apply - so the ViewModel can keep the previous
 * plan around for "Undo" and tell whether anything changed with a reference check.
 */

/** Numbers the days 1..n in their current order. */
fun Itinerary.renumbered(): Itinerary =
    copy(days = days.mapIndexed { index, day -> day.copy(day = index + 1) })

private fun Itinerary.withDay(day: Int, change: (ItineraryDay) -> ItineraryDay): Itinerary =
    copy(days = days.map { if (it.day == day) change(it) else it })

private fun Itinerary.dayOrNull(day: Int): ItineraryDay? = days.firstOrNull { it.day == day }

/** The stop at [index] of [day], if both exist. */
fun Itinerary.stopAt(day: Int, index: Int): ItineraryStop? = dayOrNull(day)?.stops?.getOrNull(index)

private fun Map<String, List<PlaceResult>>.plus(kind: StopKind, place: PlaceResult): Map<String, List<PlaceResult>> {
    val existing = this[kind.name].orEmpty()
    if (existing.any { it.id == place.id }) return this
    return this + (kind.name to existing + place)
}

private fun Map<String, List<PlaceResult>>.minus(kind: StopKind, placeId: String): Map<String, List<PlaceResult>> {
    val existing = this[kind.name] ?: return this
    return this + (kind.name to existing.filterNot { it.id == placeId })
}

/** Drops a stop. A place the planner chose goes back into [Itinerary.alternatives] (unless it
 *  is still used on another day) so it can be swapped back in. */
fun Itinerary.removeStop(day: Int, index: Int): Itinerary {
    val stop = stopAt(day, index) ?: return this
    val without = withDay(day) { d -> d.copy(stops = d.stops.filterIndexed { i, _ -> i != index }) }
    val stillUsed = without.places.any { it.id == stop.place.id }
    val alternatives = if (stop.kind == StopKind.CUSTOM || stillUsed) {
        without.alternatives
    } else {
        without.alternatives.plus(stop.kind, stop.place)
    }
    return without.copy(alternatives = alternatives)
}

/** Moves a stop one place earlier ([delta] -1) or later (+1) within its day. */
fun Itinerary.moveStop(day: Int, index: Int, delta: Int): Itinerary {
    val d = dayOrNull(day) ?: return this
    val target = index + delta
    if (index !in d.stops.indices || target !in d.stops.indices) return this
    val reordered = d.stops.toMutableList().apply { add(target, removeAt(index)) }
    return withDay(day) { it.copy(stops = reordered) }
}

/** Moves a stop to the end of another day. A day can't hold the same place twice, so this
 *  does nothing if the destination day already has it. */
fun Itinerary.moveStopToDay(day: Int, index: Int, toDay: Int): Itinerary {
    if (day == toDay) return this
    val stop = stopAt(day, index) ?: return this
    val target = dayOrNull(toDay) ?: return this
    if (target.stops.any { it.place.id == stop.place.id }) return this
    return copy(
        days = days.map { d ->
            when (d.day) {
                day -> d.copy(stops = d.stops.filterIndexed { i, _ -> i != index })
                toDay -> d.copy(stops = d.stops + stop)
                else -> d
            }
        },
    )
}

/** Whether there is another place of this stop's kind to swap in. */
fun Itinerary.canSwap(day: Int, index: Int): Boolean {
    val stop = stopAt(day, index) ?: return false
    val inDay = dayOrNull(day)?.stops?.map { it.place.id }.orEmpty().toSet()
    return alternatives[stop.kind.name].orEmpty().any { it.id !in inDay }
}

/**
 * Replaces a stop with the nearest unused place of the same kind, and puts the old
 * one back into the alternatives. Null when there is nothing to swap in.
 */
fun Itinerary.swapStop(day: Int, index: Int): Itinerary? {
    val stop = stopAt(day, index) ?: return null
    val inDay = dayOrNull(day)?.stops?.map { it.place.id }.orEmpty().toSet()
    val candidates = alternatives[stop.kind.name].orEmpty().filter { it.id !in inDay }
    if (candidates.isEmpty()) return null

    val lat = stop.place.latitude
    val lng = stop.place.longitude
    val chosen = if (lat != null && lng != null) {
        val origin = DeviceLocation(lat, lng)
        candidates.minByOrNull { origin.distanceMetresTo(it.latitude, it.longitude) ?: Double.MAX_VALUE }
    } else {
        candidates.first()
    } ?: return null

    val swapped = withDay(day) { d ->
        d.copy(stops = d.stops.mapIndexed { i, s -> if (i == index) s.copy(place = chosen, note = "") else s })
    }
    val stillUsed = swapped.places.any { it.id == stop.place.id }
    var alternatives = swapped.alternatives.minus(stop.kind, chosen.id)
    if (!stillUsed) alternatives = alternatives.plus(stop.kind, stop.place)
    return swapped.copy(alternatives = alternatives)
}

/**
 * Adds a place the traveller picked to [day]. With a [slot] it goes in at that time of
 * day - after every stop already scheduled at or before it - otherwise at the end.
 * No-op if that day already has the place.
 */
fun Itinerary.addStop(day: Int, place: PlaceResult, slot: DaySlot? = null): Itinerary {
    val d = dayOrNull(day) ?: return this
    if (d.stops.any { it.place.id == place.id }) return this
    val stop = ItineraryStop(StopKind.CUSTOM, place, verb = "", slot = slot)
    val at = if (slot == null) d.stops.size else d.stops.indexOfLast { it.timeSlot <= slot } + 1
    return withDay(day) { it.copy(stops = it.stops.toMutableList().apply { add(at, stop) }) }
}

fun Itinerary.setStopNote(day: Int, index: Int, note: String): Itinerary {
    if (stopAt(day, index) == null) return this
    return withDay(day) { d ->
        d.copy(stops = d.stops.mapIndexed { i, s -> if (i == index) s.copy(note = note.trim()) else s })
    }
}

// ---- Trip-level extras -------------------------------------------------------

fun Itinerary.withStartDate(isoDate: String?): Itinerary = copy(startDate = isoDate)

fun Itinerary.withNotes(text: String): Itinerary = copy(notes = text)

fun Itinerary.addChecklistItem(text: String): Itinerary {
    val clean = text.trim()
    if (clean.isEmpty() || checklist.any { it.text.equals(clean, ignoreCase = true) }) return this
    return copy(checklist = checklist + ChecklistItem(clean))
}

fun Itinerary.toggleChecklistItem(index: Int): Itinerary {
    if (index !in checklist.indices) return this
    return copy(checklist = checklist.mapIndexed { i, item -> if (i == index) item.copy(done = !item.done) else item })
}

fun Itinerary.removeChecklistItem(index: Int): Itinerary {
    if (index !in checklist.indices) return this
    return copy(checklist = checklist.filterIndexed { i, _ -> i != index })
}

// ---- Weather -----------------------------------------------------------------

/** How exposed to the weather a stop is: a beach day is ruined by rain, a museum isn't. */
private fun StopKind.outdoorWeight(): Int = when (this) {
    StopKind.SWIM -> 3
    StopKind.NATURE -> 2
    StopKind.SIGHT -> 1
    else -> 0
}

/** True for stops that are spoiled by rain. */
fun StopKind.isOutdoor(): Boolean = outdoorWeight() > 0

private fun ItineraryDay.outdoorScore(): Int = stops.sumOf { it.kind.outdoorWeight() }

/**
 * Reassigns whole days to dates so the most outdoor days fall on the driest ones.
 * [rainChance] is the forecast precipitation probability (0-100) for each day *slot*
 * (key = day number, i.e. the date it currently sits on); slots with no forecast keep
 * the day they have. Day contents never change, only which date they land on, so each
 * day stays a geographically compact outing. Ties keep their current order, so a plan
 * that is already well arranged comes back equal to the one passed in.
 */
fun Itinerary.rearrangedForWeather(rainChance: Map<Int, Int>): Itinerary {
    val slotsWithForecast = days.map { it.day }.filter { it in rainChance }
    if (slotsWithForecast.size < 2) return this

    val daysInSlots = slotsWithForecast.mapNotNull { dayOrNull(it) }
    val driestSlotsFirst = slotsWithForecast.sortedWith(compareBy({ rainChance.getValue(it) }, { it }))
    val mostOutdoorFirst = daysInSlots.sortedWith(compareByDescending<ItineraryDay> { it.outdoorScore() }.thenBy { it.day })

    val assigned = driestSlotsFirst.zip(mostOutdoorFirst).toMap() // slot -> day content
    val rearranged = days.map { d -> assigned[d.day]?.copy(day = d.day) ?: d }
    return copy(days = rearranged)
}
