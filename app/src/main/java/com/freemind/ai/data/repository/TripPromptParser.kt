package com.freemind.ai.data.repository

import com.freemind.ai.domain.model.StopKind

/** What a free-form trip prompt asks for, once parsed. */
data class TripRequest(
    val destination: String,
    val days: Int,
    val interests: Set<StopKind>,
)

const val MAX_TRIP_DAYS = 14
const val DEFAULT_TRIP_DAYS = 3

// The name is captured inside a lookahead so the match itself is zero-width after the
// preposition: findAll then also sees a preposition that sits inside an earlier
// candidate's words ("in restaurants when visiting Beirut" -> "visiting Beirut" too).
private val DESTINATION_REGEX = Regex(
    """\b(?:in|to|at|around|near|visit(?:ing)?|explore|exploring|toward(?:s)?)\s+(?=(\p{L}[\p{L}'.\-]*(?:\s+\p{L}[\p{L}'.\-]*){0,4}))""",
    RegexOption.IGNORE_CASE,
)

/** Words that end a destination phrase ("San Francisco AND I love...") or mean the
 *  phrase after the preposition wasn't a place at all ("swimming IN THE sea"). */
private val STOP_WORDS = setOf(
    "a", "an", "the", "and", "or", "but", "for", "with", "while", "because", "since", "so", "then",
    "i", "im", "i'm", "ill", "i'll", "ive", "i've", "we", "my", "our", "me", "us", "you", "it", "its",
    "is", "are", "was", "be", "been", "am", "will", "would", "want", "wanna", "love", "like", "enjoy",
    "need", "plan", "planning", "staying", "stay", "going", "go", "travel", "travelling", "traveling",
    "trip", "visit", "visiting", "see", "eat", "eating", "swim", "swimming", "explore", "exploring",
    "relax", "relaxing", "do", "doing", "have", "having", "try", "trying", "spend", "spending",
    "days", "day", "weeks", "week", "weekend", "nights", "night", "months", "month",
    "next", "this", "last", "from", "during", "over", "until", "till", "every", "each",
    "summer", "winter", "spring", "autumn", "fall", "january", "february", "march", "april", "may",
    "june", "july", "august", "september", "october", "november", "december",
    "also", "too", "where", "when", "which", "that", "who", "all", "some", "any", "good", "best",
    "great", "nice", "cheap", "free", "there", "here", "around", "near", "about", "of", "on", "at", "in", "to",
)

/** Lowercase connectors that belong inside a place name ("Rio de Janeiro", "Bay of Islands"). */
private val NAME_CONNECTORS = setOf("de", "of", "la", "le", "el", "al", "du", "da", "del", "di")

private val NUMBER_WORDS = mapOf(
    "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
    "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12, "fourteen" to 14,
)
private const val NUM = """(\d{1,3}|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|fourteen)"""
private val DAYS_REGEX = Regex("""\b$NUM\s*[- ]?\s*days?\b""", RegexOption.IGNORE_CASE)
private val NIGHTS_REGEX = Regex("""\b$NUM\s*[- ]?\s*nights?\b""", RegexOption.IGNORE_CASE)
private val WEEKS_REGEX = Regex("""\b(?:$NUM|a)\s*[- ]?\s*weeks?\b""", RegexOption.IGNORE_CASE)
private val WEEKEND_REGEX = Regex("""\bweekend\b""", RegexOption.IGNORE_CASE)

private val INTEREST_PATTERNS: Map<StopKind, Regex> = mapOf(
    StopKind.SWIM to Regex("""\b(swim|swimming|beach|beaches|sea|snorkel\w*|diving|surf\w*)\b""", RegexOption.IGNORE_CASE),
    StopKind.EAT to Regex("""\b(eat|eating|food|foodie|dining|dine|restaurants?|cuisine|lunch|dinner|breakfast|cafes?|coffee)\b""", RegexOption.IGNORE_CASE),
    StopKind.SIGHT to Regex("""\b(sights?|sightseeing|see|landmarks?|attractions?|tours?|explore|exploring|monuments?)\b""", RegexOption.IGNORE_CASE),
    StopKind.CULTURE to Regex("""\b(museums?|art|galleries|gallery|culture|cultural|history|historic|historical)\b""", RegexOption.IGNORE_CASE),
    StopKind.NATURE to Regex("""\b(hike|hiking|nature|parks?|trek\w*|mountains?|outdoors?|gardens?)\b""", RegexOption.IGNORE_CASE),
    StopKind.SHOPPING to Regex("""\b(shop|shopping|markets?|souks?|malls?)\b""", RegexOption.IGNORE_CASE),
    StopKind.NIGHTLIFE to Regex("""\b(nightlife|bars?|clubs?|party|pubs?)\b""", RegexOption.IGNORE_CASE),
)

/** Parses a free-form prompt; null when no destination can be found in it. */
fun parseTripPrompt(prompt: String): TripRequest? {
    val destination = extractDestination(prompt) ?: return null
    return TripRequest(destination, extractDays(prompt), extractInterests(prompt))
}

/**
 * The place the trip is to. Takes the last "in/to/at/..." phrase that yields a name,
 * so "travelling to Turkey, staying in Istanbul" plans Istanbul. Capitalised names
 * beat lowercase ones, because "eating in restaurants" isn't a place but
 * "eating in Beirut" is.
 */
internal fun extractDestination(prompt: String): String? {
    val candidates = DESTINATION_REGEX.findAll(prompt).mapNotNull { match ->
        val words = mutableListOf<String>()
        for (raw in match.groupValues[1].split(Regex("\\s+"))) {
            val word = raw.trim('.', '-', '\'')
            if (word.isEmpty()) break
            val lower = word.lowercase()
            if (lower in STOP_WORDS) break
            if (lower in NAME_CONNECTORS && words.isEmpty()) break
            words += word
        }
        // A trailing connector ("Bay of") is a dangling word, not part of the name.
        while (words.isNotEmpty() && words.last().lowercase() in NAME_CONNECTORS) words.removeAt(words.lastIndex)
        // An unrecognised lowercase tail ("istanbul food") would be junk; cap an
        // all-lowercase phrase at three words, like "rio de janeiro".
        if (words.isNotEmpty() && words.none { it.first().isUpperCase() }) {
            while (words.size > 3) words.removeAt(words.lastIndex)
        }
        if (words.isEmpty()) {
            null
        } else {
            words.joinToString(" ") { w ->
                if (w.lowercase() in NAME_CONNECTORS) w.lowercase() else w.replaceFirstChar(Char::uppercase)
            } to words.first().first().isUpperCase()
        }
    }.toList()
    return (candidates.lastOrNull { it.second } ?: candidates.lastOrNull())?.first
}

/** Trip length in days from "10 days", "ten-day", "2 weeks", "a week", "5 nights" or
 *  "weekend"; [DEFAULT_TRIP_DAYS] when none is given, capped at [MAX_TRIP_DAYS]. */
internal fun extractDays(prompt: String): Int {
    fun number(token: String): Int? = token.toIntOrNull() ?: NUMBER_WORDS[token.lowercase()]

    DAYS_REGEX.find(prompt)?.groupValues?.get(1)?.let(::number)?.let { return it.coerceIn(1, MAX_TRIP_DAYS) }
    NIGHTS_REGEX.find(prompt)?.groupValues?.get(1)?.let(::number)?.let { return (it + 1).coerceIn(1, MAX_TRIP_DAYS) }
    WEEKS_REGEX.find(prompt)?.let { m ->
        val weeks = m.groupValues[1].takeIf { it.isNotEmpty() }?.let(::number) ?: 1
        return (weeks * 7).coerceIn(1, MAX_TRIP_DAYS)
    }
    if (WEEKEND_REGEX.containsMatchIn(prompt)) return 2
    return DEFAULT_TRIP_DAYS
}

/** What the traveller says they enjoy; food plus sights when they don't say. */
internal fun extractInterests(prompt: String): Set<StopKind> {
    val found = INTEREST_PATTERNS.filterValues { it.containsMatchIn(prompt) }.keys
    return found.ifEmpty { setOf(StopKind.EAT, StopKind.SIGHT) }
}
