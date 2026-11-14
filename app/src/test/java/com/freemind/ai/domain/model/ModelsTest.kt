package com.freemind.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private fun place(
        id: String = "p",
        rating: Double? = null,
        openNow: Boolean? = null,
        price: Int? = null,
    ) = PlaceResult(
        id = id, title = "T$id", description = "addr", latitude = 1.0, longitude = 2.0,
        rating = rating, openNow = openNow, priceLevel = price,
    )

    @Test fun `inactive filters keep everything`() {
        val all = listOf(place("a"), place("b", rating = 1.0))
        assertFalse(PlaceFilters().isActive)
        assertEquals(all, PlaceFilters().apply(all))
    }

    @Test fun `open now excludes closed and unknown`() {
        val f = PlaceFilters(openNow = true)
        assertTrue(f.matches(place(openNow = true)))
        assertFalse(f.matches(place(openNow = false)))
        assertFalse(f.matches(place(openNow = null)))
    }

    @Test fun `min rating excludes lower and unrated`() {
        val f = PlaceFilters(minRating = 4.0)
        assertTrue(f.matches(place(rating = 4.0)))
        assertTrue(f.matches(place(rating = 4.8)))
        assertFalse(f.matches(place(rating = 3.9)))
        assertFalse(f.matches(place(rating = null)))
    }

    @Test fun `max price excludes pricier and unknown`() {
        val f = PlaceFilters(maxPriceLevel = 2)
        assertTrue(f.matches(place(price = 1)))
        assertTrue(f.matches(place(price = 2)))
        assertFalse(f.matches(place(price = 3)))
        assertFalse(f.matches(place(price = null)))
    }

    @Test fun `filters combine`() {
        val f = PlaceFilters(openNow = true, minRating = 4.0)
        val result = f.apply(
            listOf(
                place("ok", rating = 4.5, openNow = true),
                place("closed", rating = 4.5, openNow = false),
                place("low", rating = 3.0, openNow = true),
            ),
        )
        assertEquals(listOf("ok"), result.map { it.id })
    }

    @Test fun `rating and price labels`() {
        assertEquals("4.6★ (1,234)", place(rating = 4.6).copy(ratingCount = 1234).ratingLabel())
        assertEquals("4.0★", place(rating = 4.0).ratingLabel())
        assertNull(place().ratingLabel())
        assertEquals("$$", place(price = 2).priceLabel())
        assertNull(place(price = 0).priceLabel())
        assertNull(place().priceLabel())
    }

    @Test fun `itinerary places are distinct and share text lists every day`() {
        val a = place("a", rating = 4.5)
        val b = place("b")
        val plan = Itinerary(
            destinationName = "Beirut",
            prompt = "2 days in Beirut",
            days = listOf(
                ItineraryDay(1, listOf(ItineraryStop(StopKind.SIGHT, a, "Visit"), ItineraryStop(StopKind.EAT, b, "Lunch at"))),
                ItineraryDay(2, listOf(ItineraryStop(StopKind.SIGHT, a, "Explore"))),
            ),
            destination = null,
        )
        assertEquals(listOf("a", "b"), plan.places.map { it.id })
        val text = plan.toShareText()
        assertTrue(text.startsWith("2-day plan for Beirut"))
        assertTrue("Day 1" in text && "Day 2" in text)
        assertTrue("Visit Ta" in text && "Lunch at Tb" in text)
        assertTrue("4.5★" in text)
    }

    @Test fun `distance is zero at the same point and sensible between cities`() {
        val beirut = DeviceLocation(33.8938, 35.5018)
        assertEquals(0.0, beirut.distanceMetresTo(33.8938, 35.5018)!!, 0.5)
        // Beirut - Damascus is roughly 90 km in a straight line.
        val km = beirut.distanceMetresTo(33.5138, 36.2765)!! / 1000
        assertTrue("was $km", km in 80.0..100.0)
        assertNull(beirut.distanceMetresTo(null, 1.0))
    }
}
