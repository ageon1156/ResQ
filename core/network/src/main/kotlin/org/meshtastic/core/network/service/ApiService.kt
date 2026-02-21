

package org.meshtastic.core.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.meshtastic.core.model.NetworkDeviceHardware
import org.meshtastic.core.model.NetworkFirmwareReleases
import javax.inject.Inject

interface ApiService {
    suspend fun getDeviceHardware(): List<NetworkDeviceHardware>

    suspend fun getFirmwareReleases(): NetworkFirmwareReleases
}

class ApiServiceImpl @Inject constructor(private val client: HttpClient) : ApiService {
    override suspend fun getDeviceHardware(): List<NetworkDeviceHardware> =
        client.get("https://api.meshtastic.org/resource/deviceHardware").body()

    override suspend fun getFirmwareReleases(): NetworkFirmwareReleases =
        client.get("https://api.meshtastic.org/github/firmware/list").body()
}

