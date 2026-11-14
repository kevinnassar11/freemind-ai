package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryPlannerTest {

    private fun place(id: String, lat: Double, lng: Double) =
        PlaceResult(id = id, title = "Place $id", description = "", latitude = lat, longitude = lng)

    @Test fun `empty pools plan nothing`() {
        assertTrue(planDays(3, emptyMap()).isEmpty())
        assertTrue(planDays(3, mapOf(StopKind.EAT to emptyList())).isEmpty())
    }

    @Test fun `plans the requested number of days`() {
        val days = planDays(
            4,
            mapOf(
                StopKind.SIGHT to listOf(place("s1", 0.0, 0.0), place("s2", 0.0, 0.1)),
                StopKind.EAT to listOf(place("e1", 0.0, 0.0)),
            ),
        )
        assertEquals(listOf(1, 2, 3, 4), days.map { it.day })
        assertTrue(days.all { it.stops.isNotEmpty() })
    }

    @Test fun `stops are ordered morning to evening`() {
        val days = planDays(
            1,
            mapOf(
                StopKind.NIGHTLIFE to listOf(place("n", 0.0, 0.0)),
                StopKind.SWIM to listOf(place("w", 0.0, 0.0)),
                StopKind.SIGHT to listOf(place("s", 0.0, 0.0)),
                StopKind.EAT to listOf(place("e", 0.0, 0.0)),
            ),
        )
        assertEquals(
            listOf(StopKind.SIGHT, StopKind.EAT, StopKind.SWIM, StopKind.NIGHTLIFE),
            days.single().stops.map { it.kind },
        )
    }

    @Test fun `a day's other stops are the nearest to its anchor`() {
        // Day 1 anchors on "near-sight"; the restaurant picked should be the close one,
        // not the first in Google's order.
        val days = planDays(
            1,
            mapOf(
                StopKind.SIGHT to listOf(place("near-sight", 33.0, 35.0)),
                StopKind.EAT to listOf(place("far-eat", 34.0, 36.0), place("close-eat", 33.001, 35.001)),
            ),
        )
        val eat = days.single().stops.first { it.kind == StopKind.EAT }
        assertEquals("close-eat", eat.place.id)
    }

    @Test fun `places are not repeated until the pool is exhausted`() {
        val sights = (1..3).map { place("s$it", 0.0, it * 0.01) }
        val days = planDays(3, mapOf(StopKind.SIGHT to sights))
        assertEquals(setOf("s1", "s2", "s3"), days.flatMap { it.stops }.map { it.place.id }.toSet())
    }

    @Test fun `a small pool is reused rather than leaving days empty`() {
        val days = planDays(5, mapOf(StopKind.SIGHT to listOf(place("only", 0.0, 0.0))))
        assertEquals(5, days.size)
        assertTrue(days.all { it.stops.size == 1 })
    }

    @Test fun `the same place is never in one day twice`() {
        val shared = place("shared", 0.0, 0.0)
        val days = planDays(
            3,
            mapOf(
                StopKind.SIGHT to listOf(shared, place("s2", 0.0, 0.1)),
                StopKind.CULTURE to listOf(shared, place("c2", 0.0, 0.2)),
            ),
        )
        days.forEach { day ->
            val ids = day.stops.map { it.place.id }
            assertEquals(ids.distinct(), ids)
        }
    }

    @Test fun `verbs rotate across days`() {
        val sights = (1..4).map { place("s$it", 0.0, it * 0.01) }
        val verbs = planDays(4, mapOf(StopKind.SIGHT to sights)).map { it.stops.single().verb }
        assertEquals(verbs.size, verbs.distinct().size)
    }
}
