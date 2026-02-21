

package org.meshtastic.core.analytics.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.analytics.platform.FdroidPlatformAnalytics
import org.meshtastic.core.analytics.platform.PlatformAnalytics
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FdroidPlatformAnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindPlatformHelper(fdroidPlatformAnalytics: FdroidPlatformAnalytics): PlatformAnalytics
}

