package io.github.andreasscholz.photoorganizer.domain.review

import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class PhotoSeriesTest {

    private val noon = LocalDateTime.of(2025, 3, 3, 12, 0)

    private fun photo(id: Long, secondsAfterNoon: Long, year: Int = 2025) = MediaItem(
        id = id,
        type = MediaType.IMAGE,
        displayName = "IMG_$id.jpg",
        capturedAt = noon.withYear(year).plusSeconds(secondsAfterNoon),
        isFavorite = false,
    )

    private fun video(id: Long, secondsAfterNoon: Long) =
        photo(id, secondsAfterNoon).copy(type = MediaType.VIDEO)

    @Test
    fun `photos taken in quick succession form a series`() {
        val first = photo(1, 0)
        val second = photo(2, 10)
        val later = photo(3, 600)

        val series = findSeries(listOf(first, second, later))

        assertEquals(listOf(first, second), series[first.id])
        assertEquals(listOf(first, second), series[second.id])
        assertTrue(later.id !in series)
    }

    @Test
    fun `a series continues as long as each photo follows the previous one closely`() {
        val burst = listOf(photo(1, 0), photo(2, 25), photo(3, 50), photo(4, 75))

        val series = findSeries(burst)

        assertEquals(burst, series[1])
        assertEquals(burst, series[4])
    }

    @Test
    fun `a longer pause starts a new series`() {
        val a1 = photo(1, 0)
        val a2 = photo(2, 5)
        val b1 = photo(3, 100)
        val b2 = photo(4, 105)

        val series = findSeries(listOf(a1, a2, b1, b2))

        assertEquals(listOf(a1, a2), series[a1.id])
        assertEquals(listOf(b1, b2), series[b2.id])
    }

    @Test
    fun `photos exactly the maximum gap apart still belong together`() {
        val photos = listOf(photo(1, 0), photo(2, 30))

        assertEquals(photos, findSeries(photos, maxGap = Duration.ofSeconds(30))[1])
        assertTrue(findSeries(photos, maxGap = Duration.ofSeconds(29)).isEmpty())
    }

    @Test
    fun `single photos form no series`() {
        assertTrue(findSeries(listOf(photo(1, 0), photo(2, 300), photo(3, 900))).isEmpty())
        assertTrue(findSeries(emptyList()).isEmpty())
    }

    @Test
    fun `videos end a series and never belong to one`() {
        val before = photo(1, 0)
        val clip = video(2, 5)
        val after = photo(3, 10)

        assertTrue(findSeries(listOf(before, clip, after)).isEmpty())
    }

    @Test
    fun `photos from different years do not form a series`() {
        // Items are ordered newest year first, so the next year's photos lie in the past.
        val thisYear = photo(1, 0, year = 2025)
        val lastYear = photo(2, 5, year = 2024)

        assertTrue(findSeries(listOf(thisYear, lastYear)).isEmpty())
    }
}
