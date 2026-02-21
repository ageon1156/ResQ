
package org.meshtastic.core.network

import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.NetworkFirmwareReleases
import org.meshtastic.core.network.service.ApiService
import javax.inject.Inject

class FirmwareReleaseRemoteDataSource
@Inject
constructor(
    private val apiService: ApiService,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend fun getFirmwareReleases(): NetworkFirmwareReleases =
        withContext(dispatchers.io) { apiService.getFirmwareReleases() }
}

