package com.freemind.ai.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

fun Instant.toRelativeTimeString(now: Instant = Clock.System.now()): String {
    val diff = now - this
    return when {
        diff < 1.minutes -> "Just now"
        diff < 1.hours -> "${diff.inWholeMinutes}m ago"
        diff < 1.days -> "${diff.inWholeHours}h ago"
        diff < 7.days -> "${diff.inWholeDays}d ago"
        else -> "${this}".substringBefore("T")
    }
}
