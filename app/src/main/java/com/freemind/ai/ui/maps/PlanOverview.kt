package com.freemind.ai.ui.maps

import android.content.Context
import android.icu.text.ListFormatter
import com.freemind.ai.R
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.StopKind
import java.util.Locale

/** The words an overview needs, resolved up front so [overview] stays plain Kotlin. */
internal class OverviewLabels(
    /** "A 3-day trip to Beirut, with a focus on food and sights." - [themes] is null when none. */
    val intro: (days: Int, destination: String, themes: String?) -> String,
    val day: (day: Int, places: String) -> String,
    val topRated: (place: String, rating: String) -> String,
    val kind: (StopKind) -> String,
    /** "A, B and C" in the app's language. */
    val join: (List<String>) -> String,
)

/** A kind needs at least this rating before its best place is called out as "top rated". */
private const val TOP_RATED_MIN = 4.5

/**
 * A short written summary of the whole plan: what the trip leans towards, what each day
 * covers, and the best-rated place. Built from the plan itself (no network, no AI), so it
 * always matches what is listed below it and updates as the plan is edited.
 */
internal fun Itinerary.overview(labels: OverviewLabels): String {
    if (days.all { it.stops.isEmpty() }) return ""

    val themes = days.flatMap { it.stops }
        .map { it.kind }
        .filter { it != StopKind.CUSTOM }
        .groupingBy { it }.eachCount()
        .entries
        // Most common first; enum order breaks ties so the wording is stable.
        .sortedWith(compareByDescending<Map.Entry<StopKind, Int>> { it.value }.thenBy { it.key.ordinal })
        .take(2)
        .map { labels.kind(it.key) }
        .takeIf { it.isNotEmpty() }
        ?.let(labels.join)

    val parts = mutableListOf(labels.intro(days.size, destinationName, themes))

    days.filter { it.stops.isNotEmpty() }.forEach { day ->
        parts += labels.day(day.day, labels.join(day.stops.map { it.place.title }))
    }

    places.filter { (it.rating ?: 0.0) >= TOP_RATED_MIN }
        .maxByOrNull { it.rating ?: 0.0 }
        ?.let { parts += labels.topRated(it.title, "%.1f★".format(Locale.getDefault(), it.rating)) }

    return parts.joinToString(" ")
}

internal fun Context.overviewLabels(): OverviewLabels {
    val listFormatter = ListFormatter.getInstance(Locale.getDefault())
    return OverviewLabels(
        intro = { days, destination, themes ->
            if (themes == null) {
                getString(R.string.overview_intro, days, destination)
            } else {
                getString(R.string.overview_intro_themed, days, destination, themes)
            }
        },
        day = { day, places -> getString(R.string.overview_day, day, places) },
        topRated = { place, rating -> getString(R.string.overview_top_rated, place, rating) },
        kind = {
            getString(
                when (it) {
                    StopKind.SIGHT -> R.string.kind_sight
                    StopKind.CULTURE -> R.string.kind_culture
                    StopKind.NATURE -> R.string.kind_nature
                    StopKind.EAT -> R.string.kind_eat
                    StopKind.SWIM -> R.string.kind_swim
                    StopKind.SHOPPING -> R.string.kind_shopping
                    StopKind.NIGHTLIFE -> R.string.kind_nightlife
                    StopKind.CUSTOM -> R.string.kind_sight
                },
            )
        },
        join = { listFormatter.format(it) },
    )
}
