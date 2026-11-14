package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripPromptParserTest {

    @Test fun `multi-word destination is kept whole`() {
        assertEquals("San Francisco", extractDestination("10 days in San Francisco and I love swimming"))
        assertEquals("Abu Dhabi", extractDestination("a trip to Abu Dhabi for 4 days"))
        assertEquals("New York", extractDestination("I'm travelling to New York"))
    }

    @Test fun `last place wins so a city beats its country`() {
        assertEquals(
            "Istanbul",
            extractDestination("I'm travelling for 10 days to Turkey, staying in Istanbul, I love swimming and eating"),
        )
    }

    @Test fun `lowercase destination is capitalised`() {
        assertEquals("Istanbul", extractDestination("trip to istanbul for 3 days"))
        assertEquals("San Francisco", extractDestination("planning a trip to san francisco next summer"))
    }

    @Test fun `connectors inside a name are kept`() {
        assertEquals("Rio de Janeiro", extractDestination("5 days in Rio de Janeiro"))
    }

    @Test fun `capitalised place beats a lowercase noun after in`() {
        assertEquals("Beirut", extractDestination("I love eating in restaurants when visiting Beirut"))
    }

    @Test fun `no destination gives null`() {
        assertNull(extractDestination("I want to relax"))
        assertNull(parseTripPrompt("plan something nice"))
    }

    @Test fun `duration after in is not a destination`() {
        assertEquals("Paris", extractDestination("trip in 10 days to Paris"))
    }

    @Test fun `day counts`() {
        assertEquals(10, extractDays("10 days in Istanbul"))
        assertEquals(10, extractDays("a 10-day trip"))
        assertEquals(7, extractDays("a week in Rome"))
        assertEquals(14, extractDays("two weeks in Japan"))
        assertEquals(14, extractDays("6 weeks in Japan")) // capped
        assertEquals(5, extractDays("four nights in Lisbon"))
        assertEquals(2, extractDays("a weekend in Prague"))
        assertEquals(DEFAULT_TRIP_DAYS, extractDays("trip to Cairo"))
        assertEquals(MAX_TRIP_DAYS, extractDays("100 days in Cairo"))
    }

    @Test fun `interests are detected`() {
        assertEquals(
            setOf(StopKind.SWIM, StopKind.EAT, StopKind.SIGHT),
            extractInterests("I love swimming, eating, and sights"),
        )
        assertEquals(
            setOf(StopKind.CULTURE, StopKind.NIGHTLIFE, StopKind.SHOPPING, StopKind.NATURE),
            extractInterests("museums, bars, shopping and hiking"),
        )
    }

    @Test fun `default interests are food and sights`() {
        assertEquals(setOf(StopKind.EAT, StopKind.SIGHT), extractInterests("trip to Cairo"))
    }

    @Test fun `full prompt parses`() {
        val request = parseTripPrompt("10 days in Istanbul, I love swimming, eating, and sights")!!
        assertEquals("Istanbul", request.destination)
        assertEquals(10, request.days)
        assertEquals(setOf(StopKind.SWIM, StopKind.EAT, StopKind.SIGHT), request.interests)
    }
}
