package io.github.andreasscholz.photoorganizer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class OnThisDayTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun `anniversaries cover the same day of every year, newest first`() {
        val anniversaries = anniversariesOf(LocalDate.of(2026, 3, 3), earliestYear = 2023)

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2025, 3, 3),
                LocalDate.of(2024, 3, 3),
                LocalDate.of(2023, 3, 3),
            ),
            anniversaries,
        )
    }

    @Test
    fun `february 29th only has anniversaries in leap years`() {
        val anniversaries = anniversariesOf(LocalDate.of(2024, 2, 29), earliestYear = 2015)

        assertEquals(
            listOf(LocalDate.of(2024, 2, 29), LocalDate.of(2020, 2, 29), LocalDate.of(2016, 2, 29)),
            anniversaries,
        )
    }

    @Test
    fun `review day is the picked calendar day in the current year`() {
        val today = LocalDate.of(2026, 10, 11)

        assertEquals(LocalDate.of(2026, 3, 3), reviewDayOf(LocalDate.of(2019, 3, 3), today))
        assertEquals(LocalDate.of(2026, 12, 24), reviewDayOf(LocalDate.of(2026, 12, 24), today))
    }

    @Test
    fun `paging across new year stays in the current year`() {
        val today = LocalDate.of(2026, 10, 11)

        assertEquals(LocalDate.of(2026, 1, 1), reviewDayOf(LocalDate.of(2026, 12, 31).plusDays(1), today))
        assertEquals(LocalDate.of(2026, 12, 31), reviewDayOf(LocalDate.of(2026, 1, 1).minusDays(1), today))
    }

    @Test
    fun `review day for february 29th is in the latest leap year`() {
        val today = LocalDate.of(2026, 10, 11)

        assertEquals(LocalDate.of(2024, 2, 29), reviewDayOf(LocalDate.of(2020, 2, 29), today))
    }

    @Test
    fun `ranges span the local day`() {
        val range = anniversaryRangesOf(LocalDate.of(2026, 7, 1), berlin, earliestYear = 2026).single()

        assertEquals(
            LocalDate.of(2026, 7, 1).atStartOfDay(berlin).toInstant().toEpochMilli(),
            range.startMillis,
        )
        assertEquals(Duration.ofHours(24).toMillis(), range.endMillis - range.startMillis)
    }

    @Test
    fun `ranges follow daylight saving changes`() {
        // Clocks go forward on 2025-03-30 in Berlin, so that day has only 23 hours.
        val range = anniversaryRangesOf(LocalDate.of(2025, 3, 30), berlin, earliestYear = 2025).single()

        assertEquals(Duration.ofHours(23).toMillis(), range.endMillis - range.startMillis)
    }
}
