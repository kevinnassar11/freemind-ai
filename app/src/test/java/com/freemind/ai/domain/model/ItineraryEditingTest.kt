package com.freemind.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryEditingTest {

    private fun place(id: String, lat: Double = 0.0, lng: Double = 0.0) =
        PlaceResult(id = id, title = "Place $id", description = "", latitude = lat, longitude = lng)

    private fun stop(kind: StopKind, id: String, lat: Double = 0.0, lng: Double = 0.0) =
        ItineraryStop(kind, place(id, lat, lng), verb = "Visit")

    private fun plan(
        days: List<ItineraryDay>,
        alternatives: Map<String, List<PlaceResult>> = emptyMap(),
    ) = Itinerary("Beirut", "3 days in Beirut", days, destination = null, alternatives = alternatives)

    private val twoDays = plan(
        listOf(
            ItineraryDay(1, listOf(stop(StopKind.SIGHT, "a"), stop(StopKind.EAT, "b"), stop(StopKind.SWIM, "c"))),
            ItineraryDay(2, listOf(stop(StopKind.CULTURE, "d"))),
        ),
    )

    private fun ids(plan: Itinerary, day: Int) = plan.days.first { it.day == day }.stops.map { it.place.id }

    @Test fun `removing a stop drops it and keeps the planner pick as an alternative`() {
        val result = twoDays.removeStop(1, 1)
        assertEquals(listOf("a", "c"), ids(result, 1))
        assertEquals(listOf("b"), result.alternatives[StopKind.EAT.name].orEmpty().map { it.id })
    }

    @Test fun `removing a stop that is still used on another day does not add an alternative`() {
        val shared = plan(
            listOf(
                ItineraryDay(1, listOf(stop(StopKind.EAT, "x"))),
                ItineraryDay(2, listOf(stop(StopKind.EAT, "x"))),
            ),
        )
        assertTrue(shared.removeStop(1, 0).alternatives.isEmpty())
    }

    @Test fun `removing a custom stop does not make it an alternative`() {
        val withCustom = twoDays.addStop(2, place("mine"))
        val result = withCustom.removeStop(2, 1)
        assertTrue(result.alternatives.isEmpty())
    }

    @Test fun `removing a stop that does not exist changes nothing`() {
        assertSame(twoDays, twoDays.removeStop(1, 9))
        assertSame(twoDays, twoDays.removeStop(7, 0))
    }

    @Test fun `moving a stop reorders within the day and stops at the edges`() {
        assertEquals(listOf("b", "a", "c"), ids(twoDays.moveStop(1, 1, -1), 1))
        assertEquals(listOf("a", "c", "b"), ids(twoDays.moveStop(1, 1, +1), 1))
        assertSame(twoDays, twoDays.moveStop(1, 0, -1))
        assertSame(twoDays, twoDays.moveStop(1, 2, +1))
    }

    @Test fun `moving a stop to another day appends it there`() {
        val result = twoDays.moveStopToDay(1, 0, 2)
        assertEquals(listOf("b", "c"), ids(result, 1))
        assertEquals(listOf("d", "a"), ids(result, 2))
    }

    @Test fun `a day cannot receive a place it already has`() {
        val dup = plan(
            listOf(
                ItineraryDay(1, listOf(stop(StopKind.SIGHT, "a"))),
                ItineraryDay(2, listOf(stop(StopKind.SIGHT, "a"))),
            ),
        )
        assertSame(dup, dup.moveStopToDay(1, 0, 2))
        assertSame(dup, dup.moveStopToDay(1, 0, 1))
    }

    @Test fun `swapping picks the nearest alternative of the same kind and returns the old one`() {
        val base = plan(
            listOf(ItineraryDay(1, listOf(stop(StopKind.EAT, "old", lat = 0.0, lng = 0.0)))),
            alternatives = mapOf(
                StopKind.EAT.name to listOf(place("far", 0.0, 1.0), place("near", 0.0, 0.01)),
                StopKind.SIGHT.name to listOf(place("other-kind", 0.0, 0.0)),
            ),
        )
        assertTrue(base.canSwap(1, 0))
        val swapped = base.swapStop(1, 0)!!
        assertEquals(listOf("near"), ids(swapped, 1))
        assertEquals(listOf("far", "old"), swapped.alternatives[StopKind.EAT.name].orEmpty().map { it.id })
        // The other kind's pool is untouched.
        assertEquals(listOf("other-kind"), swapped.alternatives[StopKind.SIGHT.name].orEmpty().map { it.id })
    }

    @Test fun `swapping with no alternatives is a no-op`() {
        assertNull(twoDays.swapStop(1, 0))
        assertTrue(!twoDays.canSwap(1, 0))
    }

    @Test fun `a swap clears the old stop note`() {
        val base = plan(
            listOf(ItineraryDay(1, listOf(stop(StopKind.EAT, "old")))),
            alternatives = mapOf(StopKind.EAT.name to listOf(place("new"))),
        ).setStopNote(1, 0, "booked 7pm")
        assertEquals("", base.swapStop(1, 0)!!.stopAt(1, 0)!!.note)
    }

    @Test fun `adding a stop appends a custom stop once`() {
        val added = twoDays.addStop(2, place("mine"))
        assertEquals(listOf("d", "mine"), ids(added, 2))
        assertEquals(StopKind.CUSTOM, added.stopAt(2, 1)!!.kind)
        assertEquals("", added.stopAt(2, 1)!!.verb)
        assertSame(added, added.addStop(2, place("mine")))
    }

    @Test fun `a stop added for a time of day lands after the stops at or before it`() {
        // Day 1 is SIGHT (morning), EAT (midday), SWIM (afternoon).
        val evening = twoDays.addStop(1, place("late"), DaySlot.EVENING)
        assertEquals(listOf("a", "b", "c", "late"), ids(evening, 1))
        assertEquals(DaySlot.EVENING, evening.stopAt(1, 3)!!.timeSlot)

        val morning = twoDays.addStop(1, place("early"), DaySlot.MORNING)
        assertEquals(listOf("a", "early", "b", "c"), ids(morning, 1))

        val midday = twoDays.addStop(1, place("lunch"), DaySlot.MIDDAY)
        assertEquals(listOf("a", "b", "lunch", "c"), ids(midday, 1))
    }

    @Test fun `a custom stop with no chosen time falls back to anytime`() {
        assertEquals(DaySlot.ANYTIME, twoDays.addStop(2, place("mine")).stopAt(2, 1)!!.timeSlot)
        assertEquals(DaySlot.MORNING, twoDays.stopAt(1, 0)!!.timeSlot)
    }

    @Test fun `stop notes are trimmed and stored`() {
        val noted = twoDays.setStopNote(1, 0, "  booked 7pm  ")
        assertEquals("booked 7pm", noted.stopAt(1, 0)!!.note)
        assertSame(twoDays, twoDays.setStopNote(1, 9, "x"))
    }

    @Test fun `checklist items are added once, toggled and removed`() {
        val one = twoDays.addChecklistItem("  Passport ")
        assertEquals(listOf("Passport"), one.checklist.map { it.text })
        assertSame(one, one.addChecklistItem("passport"))
        assertSame(one, one.addChecklistItem("   "))
        val done = one.toggleChecklistItem(0)
        assertTrue(done.checklist[0].done)
        assertTrue(done.removeChecklistItem(0).checklist.isEmpty())
        assertSame(one, one.toggleChecklistItem(5))
    }

    @Test fun `start date maps to a date per day`() {
        val dated = twoDays.withStartDate("2026-12-24")
        assertEquals("2026-12-24", dated.dateOf(1).toString())
        assertEquals("2026-12-25", dated.dateOf(2).toString())
        assertNull(twoDays.dateOf(1))
        assertNull(twoDays.withStartDate("not a date").dateOf(1))
    }

    @Test fun `outdoor days move to the driest dates`() {
        val trip = plan(
            listOf(
                ItineraryDay(1, listOf(stop(StopKind.SWIM, "beach"), stop(StopKind.NATURE, "park"))), // very outdoor
                ItineraryDay(2, listOf(stop(StopKind.CULTURE, "museum"))), // indoor
                ItineraryDay(3, listOf(stop(StopKind.SIGHT, "tower"))), // a bit outdoor
            ),
        )
        // Day 1 (the beach day) currently sits on the wettest date.
        val rain = mapOf(1 to 90, 2 to 40, 3 to 10)
        val arranged = trip.rearrangedForWeather(rain)
        assertEquals(listOf(1, 2, 3), arranged.days.map { it.day })
        assertEquals(listOf("museum"), ids(arranged, 1)) // wettest date: the indoor day
        assertEquals(listOf("tower"), ids(arranged, 2))
        assertEquals(listOf("beach", "park"), ids(arranged, 3)) // driest date: the beach day
    }

    @Test fun `weather arrangement is stable when already optimal or without enough forecast`() {
        val trip = plan(
            listOf(
                ItineraryDay(1, listOf(stop(StopKind.CULTURE, "museum"))),
                ItineraryDay(2, listOf(stop(StopKind.SWIM, "beach"))),
            ),
        )
        val rain = mapOf(1 to 80, 2 to 5)
        assertEquals(trip, trip.rearrangedForWeather(rain))
        assertSame(trip, trip.rearrangedForWeather(mapOf(1 to 80))) // only one date has a forecast
        assertSame(trip, trip.rearrangedForWeather(emptyMap()))
    }

    @Test fun `weather arrangement leaves days without a forecast where they are`() {
        val trip = plan(
            listOf(
                ItineraryDay(1, listOf(stop(StopKind.SWIM, "beach"))),
                ItineraryDay(2, listOf(stop(StopKind.CULTURE, "museum"))),
                ItineraryDay(3, listOf(stop(StopKind.NATURE, "park"))), // no forecast for day 3
            ),
        )
        val arranged = trip.rearrangedForWeather(mapOf(1 to 90, 2 to 0))
        assertEquals(listOf("museum"), ids(arranged, 1))
        assertEquals(listOf("beach"), ids(arranged, 2))
        assertEquals(listOf("park"), ids(arranged, 3))
        assertNotNull(arranged.stopAt(3, 0))
    }

    @Test fun `weather codes map to coarse kinds`() {
        assertEquals(WeatherKind.CLEAR, weatherKindOf(0))
        assertEquals(WeatherKind.PARTLY_CLOUDY, weatherKindOf(2))
        assertEquals(WeatherKind.CLOUDY, weatherKindOf(3))
        assertEquals(WeatherKind.FOG, weatherKindOf(45))
        assertEquals(WeatherKind.RAIN, weatherKindOf(63))
        assertEquals(WeatherKind.RAIN, weatherKindOf(81))
        assertEquals(WeatherKind.SNOW, weatherKindOf(75))
        assertEquals(WeatherKind.STORM, weatherKindOf(95))
        assertEquals(WeatherKind.CLOUDY, weatherKindOf(1234))
    }

    @Test fun `route link visits stops in order`() {
        val day = ItineraryDay(
            1,
            listOf(stop(StopKind.SIGHT, "a", 1.0, 2.0), stop(StopKind.EAT, "b", 3.0, 4.0), stop(StopKind.SWIM, "c", 5.0, 6.0)),
        )
        val link = day.routeLink()!!
        assertTrue("origin=1.000000,2.000000" in link)
        assertTrue("destination=5.000000,6.000000" in link)
        assertTrue("waypoints=3.000000,4.000000" in link)
    }

    @Test fun `route link for one stop is a search and for none is null`() {
        val one = ItineraryDay(1, listOf(stop(StopKind.SIGHT, "a", 1.0, 2.0)))
        assertTrue(one.routeLink()!!.contains("search"))
        assertNull(ItineraryDay(1, emptyList()).routeLink())
    }

    @Test fun `route link caps waypoints at nine`() {
        val many = ItineraryDay(1, (0 until 15).map { stop(StopKind.SIGHT, "p$it", it.toDouble(), it.toDouble()) })
        val waypoints = many.routeLink()!!.substringAfter("waypoints=").split("%7C")
        assertEquals(9, waypoints.size)
    }
}
