package com.freemind.ai.notifications

import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.ItineraryDay
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TripRemindersTest {

    private fun stop(id: String) = ItineraryStop(
        StopKind.SIGHT, PlaceResult(id, "Place $id", "", 0.0, 0.0), "Visit",
    )

    private val plan = Itinerary(
        destinationName = "Beirut",
        prompt = "p",
        days = listOf(
            ItineraryDay(1, listOf(stop("a"), stop("b"))),
            ItineraryDay(2, emptyList()),
        ),
        destination = null,
        startDate = "2026-12-24",
    )

    private fun remindersAt(now: LocalDateTime, p: Itinerary = plan) = tripReminders(
        plan = p,
        now = now,
        eveTitle = { "Starts tomorrow: $it" },
        dayTitle = { day, dest -> "Day $day in $dest" },
        emptyDayBody = "Nothing planned",
    )

    @Test fun `no start date means no reminders`() {
        assertTrue(remindersAt(LocalDateTime.of(2026, 1, 1, 0, 0), plan.copy(startDate = null)).isEmpty())
    }

    @Test fun `schedules the evening before and each morning`() {
        val result = remindersAt(LocalDateTime.of(2026, 12, 1, 12, 0))
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 12, 23, 18, 0),
                LocalDateTime.of(2026, 12, 24, 8, 0),
                LocalDateTime.of(2026, 12, 25, 8, 0),
            ),
            result.map { it.at },
        )
        assertEquals("Starts tomorrow: Beirut", result[0].title)
        assertEquals("Place a · Place b", result[0].body)
        assertEquals("Day 1 in Beirut", result[1].title)
        assertEquals("Nothing planned", result[2].body)
    }

    @Test fun `reminders already in the past are dropped`() {
        val result = remindersAt(LocalDateTime.of(2026, 12, 24, 9, 0))
        assertEquals(listOf(LocalDateTime.of(2026, 12, 25, 8, 0)), result.map { it.at })
    }

    @Test fun `a long day lists only the first few stops`() {
        val busy = plan.copy(days = listOf(ItineraryDay(1, (1..8).map { stop("s$it") })))
        val body = remindersAt(LocalDateTime.of(2026, 12, 1, 0, 0), busy)[1].body
        assertEquals(4, body.split(" · ").size)
    }
}
