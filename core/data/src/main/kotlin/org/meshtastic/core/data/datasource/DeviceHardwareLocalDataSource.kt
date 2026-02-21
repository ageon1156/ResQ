
package org.meshtastic.core.data.datasource

import dagger.Lazy
import kotlinx.coroutines.withContext
import org.meshtastic.core.database.dao.DeviceHardwareDao
import org.meshtastic.core.database.entity.DeviceHardwareEntity
import org.meshtastic.core.database.entity.asEntity
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.NetworkDeviceHardware
import javax.inject.Inject

class DeviceHardwareLocalDataSource
@Inject
constructor(
    private val deviceHardwareDaoLazy: Lazy<DeviceHardwareDao>,
    private val dispatchers: CoroutineDispatchers,
) {
    private val deviceHardwareDao by lazy { deviceHardwareDaoLazy.get() }

    suspend fun insertAllDeviceHardware(deviceHardware: List<NetworkDeviceHardware>) =
        withContext(dispatchers.io) { deviceHardwareDao.insertAll(deviceHardware.map { it.asEntity() }) }

    suspend fun deleteAllDeviceHardware() = withContext(dispatchers.io) { deviceHardwareDao.deleteAll() }

    suspend fun getByHwModel(hwModel: Int): List<DeviceHardwareEntity> =
        withContext(dispatchers.io) { deviceHardwareDao.getByHwModel(hwModel) }

    suspend fun getByTarget(target: String): DeviceHardwareEntity? =
        withContext(dispatchers.io) { deviceHardwareDao.getByTarget(target) }

    suspend fun getByModelAndTarget(hwModel: Int, target: String): DeviceHardwareEntity? =
        withContext(dispatchers.io) { deviceHardwareDao.getByModelAndTarget(hwModel, target) }
}

