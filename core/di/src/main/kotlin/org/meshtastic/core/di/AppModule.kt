

package org.meshtastic.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideCoroutineDispatchers(): CoroutineDispatchers =
        CoroutineDispatchers(io = Dispatchers.IO, main = Dispatchers.Main, default = Dispatchers.Default)
}

