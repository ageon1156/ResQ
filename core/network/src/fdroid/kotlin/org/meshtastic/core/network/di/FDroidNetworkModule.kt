

package org.meshtastic.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.meshtastic.core.model.NetworkDeviceHardware
import org.meshtastic.core.model.NetworkFirmwareReleases
import org.meshtastic.core.network.BuildConfig
import org.meshtastic.core.network.service.ApiService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class FDroidNetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            interceptor =
            HttpLoggingInterceptor().apply {
                if (BuildConfig.DEBUG) {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                }
            },
        )
        .build()

    @Provides
    @Singleton
    fun provideApiService(): ApiService = object : ApiService {
        override suspend fun getDeviceHardware(): List<NetworkDeviceHardware> =
            throw NotImplementedError("API calls to getDeviceHardware are not supported on Fdroid builds.")

        override suspend fun getFirmwareReleases(): NetworkFirmwareReleases =
            throw NotImplementedError("API calls to getFirmwareReleases are not supported on Fdroid builds.")
    }
}

