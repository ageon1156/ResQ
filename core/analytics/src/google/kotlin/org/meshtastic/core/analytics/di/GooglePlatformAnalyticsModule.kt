

package org.meshtastic.core.analytics.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.analytics.platform.GooglePlatformAnalytics
import org.meshtastic.core.analytics.platform.PlatformAnalytics
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GooglePlatformAnalyticsModule {

    @Binds @Singleton
    abstract fun bindPlatformHelper(googlePlatformHelper: GooglePlatformAnalytics): PlatformAnalytics
}

