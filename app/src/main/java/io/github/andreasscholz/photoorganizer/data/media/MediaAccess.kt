package io.github.andreasscholz.photoorganizer.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** How much of the media library the user lets the app see. */
enum class MediaAccess {
    FULL,

    /** Android 14+: only the photos the user picked. Not enough to find a whole day. */
    PARTIAL,
    NONE;

    val canRead: Boolean get() = this != NONE

    companion object {
        val permissions: List<String> = buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }

        fun of(context: Context): MediaAccess {
            fun granted(permission: String) =
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

            return when {
                granted(Manifest.permission.READ_MEDIA_IMAGES) &&
                    granted(Manifest.permission.READ_MEDIA_VIDEO) -> FULL

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> PARTIAL

                else -> NONE
            }
        }
    }
}
