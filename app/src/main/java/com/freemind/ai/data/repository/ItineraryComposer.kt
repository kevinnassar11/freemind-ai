package com.freemind.ai.data.repository

import com.freemind.ai.data.remote.places.PlacesDataSource
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.util.AppError
import com.freemind.ai.util.AppResult
import javax.inject.Inject
import javax.inject.Singleton

private val DAY_COUNT_REGEX = Regex("""\b(\d{1,3})\s*[- ]?day""", RegexOption.IGNORE_CASE)
private val DESTINATION_REGEX = Regex("""\b(?:in|to)\s+([\p{L}][\p{L}'-]*)""", RegexOption.IGNORE_CASE)
private val SWIM_REGEX = Regex("""\bswim""", RegexOption.IGNORE_CASE)
private val EAT_REGEX = Regex("""\b(eat|food|dining|restaurant)""", RegexOption.IGNORE_CASE)
private val SIGHT_REGEX = Regex("""\b(sight|see|explore|museum|landmark|tour)""", RegexOption.IGNORE_CASE)
private const val MAX_DAYS = 14
private const val DEFAULT_DAYS = 3

/**
 * Builds a real day-by-day itinerary paragraph from live Places results, entirely on
 * the client. This exists because `generate-itinerary` (see supabase/functions) needs
 * an Anthropic API key that isn't configured - PlacesRepositoryImpl tries that Edge
 * Function first and falls back to this whenever it's unavailable, so the feature
 * still produces a real, working plan today, and automatically starts using genuine
 * Claude-generated text the moment that key is added - no code change needed then.
 */
@Singleton
class ItineraryComposer @Inject constructor(
    private val placesDataSource: PlacesDataSource,
) {
    suspend fun compose(prompt: String): AppResult<ItineraryResult> {
        val destination = extractDestination(prompt)
            ?: return AppResult.Failure(
                AppError.Unknown("Tell me where you're headed - e.g. \"10 days in Istanbul, I love swimming, eating, and sights\"."),
            )
        val days = DAY_COUNT_REGEX.find(prompt)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, MAX_DAYS) ?: DEFAULT_DAYS

        val wantsSwimming = SWIM_REGEX.containsMatchIn(prompt)
        val wantsEating = EAT_REGEX.containsMatchIn(prompt) || !SWIM_REGEX.containsMatchIn(prompt) && !SIGHT_REGEX.containsMatchIn(prompt)
        val wantsSights = SIGHT_REGEX.containsMatchIn(prompt) || !SWIM_REGEX.containsMatchIn(prompt) && !EAT_REGEX.containsMatchIn(prompt)

        val destinationPlace = fetch(destination, null).firstOrNull()
        val restaurants = if (wantsEating) fetch("restaurants in $destination", "restaurant") else emptyList()
        val sights = if (wantsSights) fetch("tourist attractions in $destination", "tourist_attraction") else emptyList()
        val swimSpots = if (wantsSwimming) fetch("beaches in $destination", null) else emptyList()

        val requestedCategoriesAllEmpty = listOf(
            wantsEating to restaurants,
            wantsSwimming to swimSpots,
            wantsSights to sights,
        ).filter { (wanted, _) -> wanted }.all { (_, results) -> results.isEmpty() }
        if (requestedCategoriesAllEmpty) return AppResult.Failure(AppError.Network)

        return AppResult.Success(
            ItineraryResult(
                text = composeParagraph(days, destination, restaurants, sights, swimSpots, wantsEating, wantsSwimming, wantsSights),
                places = restaurants + sights + swimSpots,
                destination = destinationPlace,
            ),
        )
    }

    private suspend fun fetch(query: String, placeType: String?): List<PlaceResult> =
        (placesDataSource.searchPlaces(query, placeType) as? AppResult.Success)?.data.orEmpty()

    private fun extractDestination(prompt: String): String? =
        DESTINATION_REGEX.findAll(prompt).lastOrNull()?.groupValues?.get(1)?.replaceFirstChar { it.uppercase() }

    private fun composeParagraph(
        days: Int,
        destination: String,
        restaurants: List<PlaceResult>,
        sights: List<PlaceResult>,
        swimSpots: List<PlaceResult>,
        wantsEating: Boolean,
        wantsSwimming: Boolean,
        wantsSights: Boolean,
    ): String {
        val sb = StringBuilder("Here's a $days-day plan for $destination.")
        for (day in 1..days) {
            val stops = mutableListOf<String>()
            if (wantsEating && restaurants.isNotEmpty()) {
                stops += "grab a meal at ${restaurants[(day - 1) % restaurants.size].title}"
            }
            if (wantsSwimming && swimSpots.isNotEmpty()) {
                stops += "cool off at ${swimSpots[(day - 1) % swimSpots.size].title}"
            }
            if (wantsSights && sights.isNotEmpty()) {
                stops += "spend time at ${sights[(day - 1) % sights.size].title}"
            }
            sb.append(" On day $day, ")
            sb.append(if (stops.isEmpty()) "explore $destination at your own pace." else stops.joinToString(", then ") + ".")
        }
        return sb.toString()
    }
}
