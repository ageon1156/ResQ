

package org.meshtastic.core.database.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.database.MeshtasticDatabase
import org.meshtastic.core.database.dao.DeviceHardwareDao
import org.meshtastic.core.database.dao.FirmwareReleaseDao
import org.meshtastic.core.database.dao.MeshLogDao
import org.meshtastic.core.database.dao.NodeInfoDao
import org.meshtastic.core.database.dao.PacketDao
import org.meshtastic.core.database.dao.QuickChatActionDao
import org.meshtastic.core.database.dao.TracerouteNodePositionDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(app: Application): MeshtasticDatabase = MeshtasticDatabase.getDatabase(app)

    @Provides fun provideNodeInfoDao(database: MeshtasticDatabase): NodeInfoDao = database.nodeInfoDao()

    @Provides fun providePacketDao(database: MeshtasticDatabase): PacketDao = database.packetDao()

    @Provides fun provideMeshLogDao(database: MeshtasticDatabase): MeshLogDao = database.meshLogDao()

    @Provides
    fun provideQuickChatActionDao(database: MeshtasticDatabase): QuickChatActionDao = database.quickChatActionDao()

    @Provides
    fun provideDeviceHardwareDao(database: MeshtasticDatabase): DeviceHardwareDao = database.deviceHardwareDao()

    @Provides
    fun provideFirmwareReleaseDao(database: MeshtasticDatabase): FirmwareReleaseDao = database.firmwareReleaseDao()

    @Provides
    fun provideTracerouteNodePositionDao(database: MeshtasticDatabase): TracerouteNodePositionDao =
        database.tracerouteNodePositionDao()
}

