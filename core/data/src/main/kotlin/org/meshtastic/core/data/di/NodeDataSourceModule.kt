

package org.meshtastic.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.data.datasource.NodeInfoReadDataSource
import org.meshtastic.core.data.datasource.NodeInfoWriteDataSource
import org.meshtastic.core.data.datasource.SwitchingNodeInfoReadDataSource
import org.meshtastic.core.data.datasource.SwitchingNodeInfoWriteDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NodeDataSourceModule {
    @Binds @Singleton
    fun bindNodeInfoReadDataSource(impl: SwitchingNodeInfoReadDataSource): NodeInfoReadDataSource

    @Binds @Singleton
    fun bindNodeInfoWriteDataSource(impl: SwitchingNodeInfoWriteDataSource): NodeInfoWriteDataSource
}

