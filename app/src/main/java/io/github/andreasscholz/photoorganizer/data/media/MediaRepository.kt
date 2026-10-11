package io.github.andreasscholz.photoorganizer.data.media

import android.content.IntentSender
import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import java.time.LocalDate

/**
 * Access to the photos and videos on the device.
 *
 * Android only lets an app change media it did not create after the user has confirmed a
 * system dialog. The `create…Request` functions return the [IntentSender] that shows it.
 */
interface MediaRepository {

    /** Media captured on [date] or on the same day of an earlier year, newest year first. */
    suspend fun mediaOnThisDay(date: LocalDate): List<MediaItem>

    /** Existing albums that can hold both photos and videos, sorted by name. */
    suspend fun albums(): List<Album>

    /** Confirming the request moves [items] to the system trash. */
    fun createTrashRequest(items: List<MediaItem>): IntentSender

    /** Confirming the request sets the favorite state of [items]. */
    fun createFavoriteRequest(items: List<MediaItem>, favorite: Boolean): IntentSender

    /** Confirming the request allows [moveToAlbum] for [items]. */
    fun createWriteRequest(items: List<MediaItem>): IntentSender

    /** Moves [items] into [album] and returns how many could be moved. */
    suspend fun moveToAlbum(items: List<MediaItem>, album: Album): Int
}
