

package org.meshtastic.core.network.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import coil3.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.meshtastic.core.network.BuildConfig
import javax.inject.Singleton

private const val DISK_CACHE_PERCENT = 0.02
private const val MEMORY_CACHE_PERCENT = 0.25

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideImageLoader(okHttpClient: OkHttpClient, @ApplicationContext application: Context): ImageLoader {
        val sharedOkHttp = okHttpClient.newBuilder().build()
        return ImageLoader.Builder(context = application)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { sharedOkHttp }))
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(context = application, percent = MEMORY_CACHE_PERCENT).build()
            }
            .diskCache { DiskCache.Builder().maxSizePercent(percent = DISK_CACHE_PERCENT).build() }
            .logger(logger = if (BuildConfig.DEBUG) DebugLogger(minLevel = Logger.Level.Verbose) else null)
            .crossfade(enable = true)
            .build()
    }
}

