package io.github.andreasscholz.photoorganizer.domain.model

/** Restricts the review to media from certain folders, e.g. only the camera folder. */
sealed interface FolderFilter {

    fun includes(relativePath: String): Boolean

    data object AllFolders : FolderFilter {
        override fun includes(relativePath: String) = true
    }

    /** Only folders whose path is in [relativePaths]; subfolders are not included implicitly. */
    data class Only(val relativePaths: Set<String>) : FolderFilter {
        override fun includes(relativePath: String) = relativePath in relativePaths

        fun with(relativePath: String, selected: Boolean): Only =
            Only(if (selected) relativePaths + relativePath else relativePaths - relativePath)
    }
}
