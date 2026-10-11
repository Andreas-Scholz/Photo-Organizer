package io.github.andreasscholz.photoorganizer.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/**
 * Reviews the day [dateEpochDay] (see [java.time.LocalDate.toEpochDay]). [startItemId] is
 * the media item the review begins with; the first one if null.
 */
@Serializable
data class ReviewRoute(val dateEpochDay: Long, val startItemId: Long? = null)

@Serializable
data object SettingsRoute

@Serializable
data object FolderSelectionRoute
