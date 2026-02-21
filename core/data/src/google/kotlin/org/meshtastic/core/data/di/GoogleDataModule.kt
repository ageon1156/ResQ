

package org.meshtastic.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import org.meshtastic.core.data.repository.CustomTileProviderRepository
import org.meshtastic.core.data.repository.CustomTileProviderRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface GoogleDataModule {

    @Binds
    @Singleton
    fun bindCustomTileProviderRepository(impl: CustomTileProviderRepositoryImpl): CustomTileProviderRepository

    companion object {
        @Provides @Singleton
        fun provideJson(): Json = Json { prettyPrint = false }
    }
}

