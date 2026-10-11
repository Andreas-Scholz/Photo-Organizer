package io.github.andreasscholz.photoorganizer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val eightPm = LocalTime.of(20, 0)

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, berlin)

    @Test
    fun `reminder later today is scheduled for today`() {
        assertEquals(at(11, 20), nextOccurrenceOf(eightPm, now = at(11, 9, 30)))
    }

    @Test
    fun `reminder time that has passed is scheduled for tomorrow`() {
        assertEquals(at(12, 20), nextOccurrenceOf(eightPm, now = at(11, 21)))
    }

    @Test
    fun `reminder firing right now is scheduled for tomorrow`() {
        assertEquals(at(12, 20), nextOccurrenceOf(eightPm, now = at(11, 20)))
    }
}
