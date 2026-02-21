

package org.meshtastic.core.data.di

import android.content.Context
import android.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
}

