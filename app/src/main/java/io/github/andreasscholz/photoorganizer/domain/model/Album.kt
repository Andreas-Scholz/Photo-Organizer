package io.github.andreasscholz.photoorganizer.domain.model

/** A folder of the shared media storage, e.g. name "Holiday" at "Pictures/Holiday/". */
data class Album(
    val name: String,
    /** Path relative to the storage root, always ending in a slash. */
    val relativePath: String,
) {
    companion object {
        private const val NEW_ALBUM_ROOT = "Pictures/"
        private val forbiddenChars = "/\\:*?\"<>|".toSet()

        fun isValidName(name: String): Boolean {
            val trimmed = name.trim()
            return trimmed.isNotEmpty() && trimmed != "." && trimmed != ".." &&
                trimmed.none { it in forbiddenChars || it.isISOControl() }
        }

        /** Albums created by this app live in Pictures/, which accepts both photos and videos. */
        fun create(name: String): Album {
            require(isValidName(name)) { "Invalid album name: $name" }
            val trimmed = name.trim()
            return Album(name = trimmed, relativePath = "$NEW_ALBUM_ROOT$trimmed/")
        }
    }
}
