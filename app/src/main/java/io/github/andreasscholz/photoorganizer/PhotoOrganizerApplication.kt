package io.github.andreasscholz.photoorganizer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import io.github.andreasscholz.photoorganizer.di.ApplicationScope
import io.github.andreasscholz.photoorganizer.reminder.DailyReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PhotoOrganizerApplication : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var dailyReminder: DailyReminder

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Covers the first launch and force stops, after which no alarm is pending.
        applicationScope.launch { dailyReminder.sync() }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            // Lets image composables show a frame of a video as its thumbnail.
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
