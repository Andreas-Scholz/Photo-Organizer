package io.github.andreasscholz.photoorganizer.domain.review

import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.model.MediaType
import java.time.Duration

/** Photos taken at most this long after the previous one are assumed to show the same scene. */
val DEFAULT_SERIES_GAP: Duration = Duration.ofSeconds(30)

/**
 * Finds series of photos taken in quick succession, which usually are attempts at the same
 * picture. Returns the series of every photo that belongs to one, by item id.
 *
 * [items] must be in the order they were captured. A photo continues a series if it was taken
 * within [maxGap] of the photo before it, so a long burst forms a single series even if its
 * first and last photo are further apart. Videos never belong to a series and end one.
 */
fun findSeries(
    items: List<MediaItem>,
    maxGap: Duration = DEFAULT_SERIES_GAP,
): Map<Long, List<MediaItem>> {
    val seriesByItemId = mutableMapOf<Long, List<MediaItem>>()
    var run = mutableListOf<MediaItem>()

    fun closeRun() {
        if (run.size > 1) {
            val series = run.toList()
            series.forEach { seriesByItemId[it.id] = series }
        }
        run = mutableListOf()
    }

    for (item in items) {
        val previous = run.lastOrNull()
        val continuesRun = previous != null && item.type == MediaType.IMAGE &&
            Duration.between(previous.capturedAt, item.capturedAt).let { !it.isNegative && it <= maxGap }
        if (!continuesRun) closeRun()
        if (item.type == MediaType.IMAGE) run += item
    }
    closeRun()

    return seriesByItemId
}
