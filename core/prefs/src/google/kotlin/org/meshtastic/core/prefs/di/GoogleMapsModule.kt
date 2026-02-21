

package org.meshtastic.core.prefs.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.prefs.map.GoogleMapsPrefs
import org.meshtastic.core.prefs.map.GoogleMapsPrefsImpl
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class GoogleMapsSharedPreferences

@InstallIn(SingletonComponent::class)
@Module
interface GoogleMapsModule {

    @Binds fun bindGoogleMapsPrefs(googleMapsPrefsImpl: GoogleMapsPrefsImpl): GoogleMapsPrefs

    companion object {

        @Provides
        @Singleton
        @GoogleMapsSharedPreferences
        fun provideGoogleMapsSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("google_maps_prefs", Context.MODE_PRIVATE)
    }
}

