package io.github.andreasscholz.photoorganizer.domain

import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

/** Photos older than this are not looked up; it only bounds the size of the media query. */
const val EARLIEST_YEAR = 1990

/** Half-open range of epoch milliseconds: [startMillis, endMillis). */
data class TimeRange(val startMillis: Long, val endMillis: Long)

/**
 * The given date and the same calendar day in every earlier year, newest first.
 * February 29th only has anniversaries in leap years.
 */
fun anniversariesOf(date: LocalDate, earliestYear: Int = EARLIEST_YEAR): List<LocalDate> {
    val monthDay = MonthDay.from(date)
    return (date.year downTo earliestYear)
        .filter { monthDay.isValidYear(it) }
        .map { monthDay.atYear(it) }
}

/**
 * The review day for an arbitrary [date]: the same calendar day in the year of [today], so
 * that its anniversaries reach up to the present no matter which year [date] is in.
 * February 29th falls back to the latest leap year.
 */
fun reviewDayOf(date: LocalDate, today: LocalDate): LocalDate {
    val monthDay = MonthDay.from(date)
    val year = (today.year downTo today.year - 7).first { monthDay.isValidYear(it) }
    return monthDay.atYear(year)
}

/** The local-time extent of each anniversary of [date]; days are not always 24 hours long. */
fun anniversaryRangesOf(
    date: LocalDate,
    zone: ZoneId,
    earliestYear: Int = EARLIEST_YEAR,
): List<TimeRange> = anniversariesOf(date, earliestYear).map { day ->
    TimeRange(
        startMillis = day.atStartOfDay(zone).toInstant().toEpochMilli(),
        endMillis = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
