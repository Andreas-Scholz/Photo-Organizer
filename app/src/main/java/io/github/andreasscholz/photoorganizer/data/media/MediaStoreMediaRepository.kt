package io.github.andreasscholz.photoorganizer.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import io.github.andreasscholz.photoorganizer.di.IoDispatcher
import io.github.andreasscholz.photoorganizer.domain.anniversaryRangesOf
import io.github.andreasscholz.photoorganizer.domain.model.Album
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter
import io.github.andreasscholz.photoorganizer.domain.model.MediaFolder
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** The content URI under which the system and other apps know this item. */
val MediaItem.contentUri: Uri
    get() = ContentUris.withAppendedId(
        when (type) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        },
        id,
    )

@Singleton
class MediaStoreMediaRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MediaRepository {

    private val collection: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override suspend fun mediaOnThisDay(
        date: LocalDate,
        folderFilter: FolderFilter,
    ): List<MediaItem> = withContext(ioDispatcher) {
        val folderPaths = (folderFilter as? FolderFilter.Only)?.relativePaths
        if (folderPaths != null && folderPaths.isEmpty()) return@withContext emptyList()

        val zone = ZoneId.systemDefault()
        val ranges = anniversaryRangesOf(date, zone)

        // DATE_TAKEN (milliseconds) is missing for media without usable capture metadata, e.g.
        // screenshots, messenger images or photos lacking a time zone. Those fall back to
        // DATE_MODIFIED (seconds), which unlike the date added survives a move to a new phone.
        val dayClause = "(($DATE_TAKEN >= ? AND $DATE_TAKEN < ?) OR " +
            "($DATE_TAKEN IS NULL AND $DATE_MODIFIED >= ? AND $DATE_MODIFIED < ?))"
        val folderClause = folderPaths
            ?.let { " AND ${FileColumns.RELATIVE_PATH} IN (${it.joinToString { "?" }})" }
            .orEmpty()
        val selection = "$IS_VISUAL_MEDIA AND (${ranges.joinToString(" OR ") { dayClause }})$folderClause"
        val selectionArgs = ranges.flatMap {
            listOf(it.startMillis, it.endMillis, it.startMillis / 1000, it.endMillis / 1000)
        }.map(Long::toString).plus(folderPaths.orEmpty()).toTypedArray()

        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
            FileColumns.DISPLAY_NAME,
            DATE_TAKEN,
            DATE_MODIFIED,
            FileColumns.IS_FAVORITE,
        )

        val items = mutableListOf<MediaItem>()
        contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val takenColumn = cursor.getColumnIndexOrThrow(DATE_TAKEN)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DATE_MODIFIED)
            val favoriteColumn = cursor.getColumnIndexOrThrow(FileColumns.IS_FAVORITE)

            while (cursor.moveToNext()) {
                val capturedMillis =
                    if (cursor.isNull(takenColumn)) cursor.getLong(modifiedColumn) * 1000
                    else cursor.getLong(takenColumn)
                items += MediaItem(
                    id = cursor.getLong(idColumn),
                    type = if (cursor.getInt(typeColumn) == FileColumns.MEDIA_TYPE_VIDEO) {
                        MediaType.VIDEO
                    } else {
                        MediaType.IMAGE
                    },
                    displayName = cursor.getString(nameColumn).orEmpty(),
                    capturedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(capturedMillis), zone),
                    isFavorite = cursor.getInt(favoriteColumn) == 1,
                )
            }
        }
        items.sortedWith(compareByDescending<MediaItem> { it.capturedAt.year }.thenBy { it.capturedAt })
    }

    override suspend fun folders(): List<MediaFolder> = withContext(ioDispatcher) {
        val projection = arrayOf(FileColumns.BUCKET_DISPLAY_NAME, FileColumns.RELATIVE_PATH)
        val names = mutableMapOf<String, String>()
        val counts = mutableMapOf<String, Int>()
        contentResolver.query(collection, projection, IS_VISUAL_MEDIA, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(FileColumns.BUCKET_DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(FileColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val path = cursor.getString(pathColumn) ?: continue
                val name = cursor.getString(nameColumn) ?: continue
                names.getOrPut(path) { name }
                counts[path] = (counts[path] ?: 0) + 1
            }
        }
        names
            .map { (path, name) -> MediaFolder(name, path, counts.getValue(path)) }
            .sortedWith(compareByDescending<MediaFolder> { it.itemCount }.thenBy { it.name.lowercase() })
    }

    override suspend fun albums(): List<Album> = folders()
        .filter { folder -> SHARED_MEDIA_ROOTS.any { folder.relativePath.startsWith(it) } }
        .map { Album(it.name, it.relativePath) }
        .sortedBy { it.name.lowercase() }

    override fun createTrashRequest(items: List<MediaItem>): IntentSender =
        MediaStore.createTrashRequest(contentResolver, items.map { it.contentUri }, true).intentSender

    override fun createFavoriteRequest(items: List<MediaItem>, favorite: Boolean): IntentSender =
        MediaStore.createFavoriteRequest(contentResolver, items.map { it.contentUri }, favorite).intentSender

    override fun createWriteRequest(items: List<MediaItem>): IntentSender =
        MediaStore.createWriteRequest(contentResolver, items.map { it.contentUri }).intentSender

    override suspend fun moveToAlbum(items: List<MediaItem>, album: Album): Int = withContext(ioDispatcher) {
        // Changing the relative path makes MediaStore move the underlying file.
        val values = ContentValues().apply { put(FileColumns.RELATIVE_PATH, album.relativePath) }
        items.count { item ->
            try {
                contentResolver.update(item.contentUri, values, null, null) > 0
            } catch (e: Exception) {
                Log.w(TAG, "Could not move ${item.displayName} to ${album.relativePath}", e)
                false
            }
        }
    }

    private companion object {
        const val TAG = "MediaRepository"

        const val DATE_TAKEN = FileColumns.DATE_TAKEN
        const val DATE_MODIFIED = FileColumns.DATE_MODIFIED

        const val IS_VISUAL_MEDIA = "${FileColumns.MEDIA_TYPE} IN " +
            "(${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO})"

        /** The top-level folders in which Android allows both photos and videos. */
        val SHARED_MEDIA_ROOTS = listOf("DCIM/", "Pictures/")
    }
}
