
package org.meshtastic.core.network

import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.NetworkDeviceHardware
import org.meshtastic.core.network.service.ApiService
import javax.inject.Inject

class DeviceHardwareRemoteDataSource
@Inject
constructor(
    private val apiService: ApiService,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend fun getAllDeviceHardware(): List<NetworkDeviceHardware> =
        withContext(dispatchers.io) { apiService.getDeviceHardware() }
}

