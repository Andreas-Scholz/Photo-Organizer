package io.github.andreasscholz.photoorganizer.domain

import java.time.LocalTime
import java.time.ZonedDateTime

/** The next moment strictly after [now] at which the wall clock shows [time]. */
fun nextOccurrenceOf(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
    val today = now.with(time)
    return if (today.isAfter(now)) today else now.plusDays(1).with(time)
}
