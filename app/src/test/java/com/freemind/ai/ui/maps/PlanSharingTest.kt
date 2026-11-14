package com.freemind.ai.ui.maps

import com.freemind.ai.domain.model.ChecklistItem
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

class PlanSharingTest {

    private val labels = PlanShareLabels(
        title = "2-day plan for Beirut",
        dayHeader = { "Day $it" },
        dateLabel = { it.toString() },
        slot = { it.name },
        verb = { it.verb },
        routeLabel = "Route",
        notesHeader = "Notes",
        checklistHeader = "Checklist",
        footer = "Planned with FreeMind AI",
    )

    private fun place(id: String, rating: Double? = null) = PlaceResult(
        id = id, title = "Place $id", description = "Main St", latitude = 33.0, longitude = 35.0, rating = rating,
    )

    private val plan = Itinerary(
        destinationName = "Beirut",
        prompt = "2 days in Beirut",
        days = listOf(
            ItineraryDay(
                1,
                listOf(
                    ItineraryStop(StopKind.SIGHT, place("a", rating = 4.5), "Visit"),
                    ItineraryStop(StopKind.EAT, place("b"), "Lunch at", note = "booked 7pm"),
                ),
            ),
            ItineraryDay(2, listOf(ItineraryStop(StopKind.CUSTOM, place("c"), verb = ""))),
        ),
        destination = null,
    )

    @Test fun `share text lists every day, stop, note and route`() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val text = plan.toShareText(labels)
            assertTrue(text.startsWith("2-day plan for Beirut"))
            assertTrue("Day 1" in text && "Day 2" in text)
            assertTrue("- Visit Place a (4.5★) - Main St" in text)
            assertTrue("- Lunch at Place b" in text)
            assertTrue("booked 7pm" in text)
            assertTrue("Route: https://www.google.com/maps/dir/" in text)
            assertTrue(text.endsWith("Planned with FreeMind AI"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `a stop the traveller added shows just its name`() {
        assertTrue("- Place c" in plan.toShareText(labels))
    }

    @Test fun `dates, notes and checklist appear when set`() {
        val full = plan.copy(
            startDate = "2026-12-24",
            notes = "Bring cash",
            checklist = listOf(ChecklistItem("Passport", done = true), ChecklistItem("Charger")),
        )
        val text = full.toShareText(labels)
        assertTrue("(2026-12-24 - 2026-12-25)" in text)
        assertTrue("Day 2 · 2026-12-25" in text)
        assertTrue("Notes\nBring cash" in text)
        assertTrue("[x] Passport" in text && "[ ] Charger" in text)
    }

    @Test fun `no dates, notes or checklist means none of those sections`() {
        val text = plan.toShareText(labels)
        assertFalse("Notes" in text)
        assertFalse("Checklist" in text)
        assertEquals(1, Regex("Planned with FreeMind AI").findAll(text).count())
    }
}
