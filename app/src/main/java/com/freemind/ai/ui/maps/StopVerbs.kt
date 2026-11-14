package com.freemind.ai.ui.maps

import android.content.Context
import com.freemind.ai.R
import com.freemind.ai.data.repository.STOP_VERBS
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.StopKind

private fun StopKind.verbsArray(): Int? = when (this) {
    StopKind.SIGHT -> R.array.verbs_sight
    StopKind.CULTURE -> R.array.verbs_culture
    StopKind.NATURE -> R.array.verbs_nature
    StopKind.EAT -> R.array.verbs_eat
    StopKind.SWIM -> R.array.verbs_swim
    StopKind.SHOPPING -> R.array.verbs_shopping
    StopKind.NIGHTLIFE -> R.array.verbs_nightlife
    StopKind.CUSTOM -> null
}

/**
 * The stop's lead-in phrase ("Lunch at") in the app's language.
 *
 * A plan stores its phrases as plain English so saved plans stay readable and need no
 * migration; the planner's English list and the translated string-arrays line up by
 * position, so the stored phrase's index picks the translation. A phrase that isn't in
 * the list (an older plan, a future change) is shown as stored.
 */
internal fun Context.localizedVerb(stop: ItineraryStop): String {
    if (stop.verb.isBlank()) return ""
    val english = STOP_VERBS[stop.kind] ?: return stop.verb
    val index = english.indexOf(stop.verb)
    val array = stop.kind.verbsArray()
    if (index < 0 || array == null) return stop.verb
    return resources.getStringArray(array).getOrNull(index) ?: stop.verb
}
