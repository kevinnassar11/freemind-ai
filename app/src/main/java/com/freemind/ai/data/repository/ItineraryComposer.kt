package com.freemind.ai.data.repository

import com.freemind.ai.data.remote.places.PlacesDataSource
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.StopKind
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/** The Places query (and optional type filter) used to find each kind of stop. */
private fun searchFor(kind: StopKind, destination: String): Pair<String, String?> = when (kind) {
    StopKind.SIGHT -> "tourist attractions in $destination" to "tourist_attraction"
    StopKind.CULTURE -> "museums and galleries in $destination" to "museum"
    StopKind.NATURE -> "parks and nature in $destination" to "park"
    StopKind.SWIM -> "beaches in $destination" to null
    StopKind.EAT -> "restaurants in $destination" to "restaurant"
    StopKind.SHOPPING -> "shopping and markets in $destination" to null
    StopKind.NIGHTLIFE -> "bars and nightlife in $destination" to null
}

/**
 * Builds a real day-by-day plan from live Places results, entirely on the client -
 * no AI service involved. The destination, trip length and interests are parsed from
 * the prompt ([parseTripPrompt]), each interest is searched in parallel, and
 * [planDays] schedules the results so each day stays geographically compact.
 */
@Singleton
class ItineraryComposer @Inject constructor(
    private val placesDataSource: PlacesDataSource,
) {
    suspend fun compose(prompt: String): AppResult<Itinerary> {
        val request = parseTripPrompt(prompt)
            ?: return AppResult.Failure(
                AppError.Unknown("Tell me where you're headed - e.g. \"10 days in Istanbul, I love swimming, eating, and sights\"."),
            )

        val (destinationResult, poolResults) = coroutineScope {
            val destination = async { fetch(request.destination, null) }
            val pools = request.interests.associateWith { kind ->
                val (query, type) = searchFor(kind, request.destination)
                async { fetch(query, type) }
            }
            destination.await() to pools.mapValues { it.value.await() }
        }

        val pools = poolResults.mapValues { (it.value as? AppResult.Success)?.data.orEmpty() }
        if (pools.values.all { it.isEmpty() }) {
            // Distinguish "couldn't reach Google" (retryable) from "found nothing there".
            val error = poolResults.values.firstNotNullOfOrNull { (it as? AppResult.Failure)?.error }
            return AppResult.Failure(
                error ?: AppError.Unknown("I couldn't find anything to plan in \"${request.destination}\". Check the spelling or try a bigger city."),
            )
        }

        val destinationPlace = (destinationResult as? AppResult.Success)?.data?.firstOrNull()
        return AppResult.Success(
            Itinerary(
                destinationName = request.destination,
                prompt = prompt,
                days = planDays(request.days, pools),
                destination = destinationPlace,
            ),
        )
    }

    private suspend fun fetch(query: String, placeType: String?): AppResult<List<PlaceResult>> =
        placesDataSource.searchPlaces(query, placeType)
}
