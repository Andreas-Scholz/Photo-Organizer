package io.github.andreasscholz.photoorganizer.di

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.andreasscholz.photoorganizer.data.media.MediaRepository
import io.github.andreasscholz.photoorganizer.data.media.MediaStoreMediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** A scope that lives as long as the process, for work that must outlive any screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindMediaRepository(repository: MediaStoreMediaRepository): MediaRepository

    companion object {
        @Provides
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
