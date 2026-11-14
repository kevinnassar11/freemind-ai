package com.freemind.ai.ui.maps

import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.ItineraryDay
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PlanOverviewTest {

    private val labels = OverviewLabels(
        intro = { days, dest, themes -> "A $days-day trip to $dest" + (themes?.let { ", built around $it" } ?: "") + "." },
        day = { day, places -> "Day $day covers $places." },
        topRated = { place, rating -> "Best: $place ($rating)." },
        kind = { it.name.lowercase() },
        join = { it.joinToString(" and ") },
    )

    private fun stop(kind: StopKind, id: String, rating: Double? = null) = ItineraryStop(
        kind, PlaceResult(id, "Place $id", "", 0.0, 0.0, rating = rating), "Visit",
    )

    private fun plan(vararg days: List<ItineraryStop>) = Itinerary(
        destinationName = "Beirut",
        prompt = "p",
        days = days.mapIndexed { i, stops -> ItineraryDay(i + 1, stops) },
        destination = null,
    )

    @Test fun `summarises the trip, each day and the best place`() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val text = plan(
                listOf(stop(StopKind.SIGHT, "a", 4.2), stop(StopKind.EAT, "b", 4.8)),
                listOf(stop(StopKind.EAT, "c"), stop(StopKind.EAT, "d")),
            ).overview(labels)
            assertEquals(
                "A 2-day trip to Beirut, built around eat and sight. " +
                    "Day 1 covers Place a and Place b. Day 2 covers Place c and Place d. " +
                    "Best: Place b (4.8★).",
                text,
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `no top-rated line when nothing is rated highly enough`() {
        val text = plan(listOf(stop(StopKind.SIGHT, "a", 4.4))).overview(labels)
        assertFalse("Best:" in text)
    }

    @Test fun `a stop the traveller added is listed but is not a theme`() {
        val text = plan(listOf(stop(StopKind.CUSTOM, "x"))).overview(labels)
        assertTrue(text.startsWith("A 1-day trip to Beirut."))
        assertTrue("Place x" in text)
    }

    @Test fun `empty days are skipped and an empty plan has no overview`() {
        val text = plan(emptyList(), listOf(stop(StopKind.SIGHT, "a"))).overview(labels)
        assertFalse("Day 1" in text)
        assertTrue("Day 2 covers Place a." in text)
        assertEquals("", plan(emptyList()).overview(labels))
    }
}
