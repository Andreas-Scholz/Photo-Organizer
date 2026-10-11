package io.github.andreasscholz.photoorganizer.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** [startItemId] is the media item the review begins with; the first one if null. */
@Serializable
data class ReviewRoute(val startItemId: Long? = null)

@Serializable
data object SettingsRoute

@Serializable
data object FolderSelectionRoute
