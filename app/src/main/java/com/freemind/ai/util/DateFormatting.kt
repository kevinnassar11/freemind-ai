package com.freemind.ai.util

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * "5m ago" style label, in the app's current language (ICU does the wording, so
 * Arabic and French get their own plural and abbreviation rules).
 */
fun Instant.toRelativeTimeString(now: Instant = Clock.System.now()): String {
    val diff = now - this
    val formatter = RelativeDateTimeFormatter.getInstance(
        android.icu.util.ULocale.forLocale(Locale.getDefault()),
        null,
        RelativeDateTimeFormatter.Style.SHORT,
        android.icu.text.DisplayContext.CAPITALIZATION_NONE,
    )
    return when {
        diff < 1.minutes -> formatter.format(
            RelativeDateTimeFormatter.Direction.THIS,
            RelativeDateTimeFormatter.AbsoluteUnit.NOW,
        )
        diff < 1.hours -> formatter.format(diff.inWholeMinutes.toDouble(), Direction.LAST, RelativeUnit.MINUTES)
        diff < 1.days -> formatter.format(diff.inWholeHours.toDouble(), Direction.LAST, RelativeUnit.HOURS)
        diff < 7.days -> formatter.format(diff.inWholeDays.toDouble(), Direction.LAST, RelativeUnit.DAYS)
        else -> "${this}".substringBefore("T")
    }
}
