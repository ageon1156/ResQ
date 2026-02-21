
package org.meshtastic.core.data.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.meshtastic.core.data.datasource.BootloaderOtaQuirksJsonDataSource
import org.meshtastic.core.data.datasource.DeviceHardwareJsonDataSource
import org.meshtastic.core.data.datasource.DeviceHardwareLocalDataSource
import org.meshtastic.core.database.entity.DeviceHardwareEntity
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.network.DeviceHardwareRemoteDataSource

class DeviceHardwareRepositoryTest {

    private val remoteDataSource: DeviceHardwareRemoteDataSource = mockk()
    private val localDataSource: DeviceHardwareLocalDataSource = mockk()
    private val jsonDataSource: DeviceHardwareJsonDataSource = mockk()
    private val bootloaderOtaQuirksJsonDataSource: BootloaderOtaQuirksJsonDataSource = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher, default = testDispatcher)

    private val repository =
        DeviceHardwareRepository(
            remoteDataSource,
            localDataSource,
            jsonDataSource,
            bootloaderOtaQuirksJsonDataSource,
            dispatchers,
        )

    @Test
    fun `getDeviceHardwareByModel uses target for disambiguation`() = runTest(testDispatcher) {
        val hwModel = 50 
        val target = "tdeck-pro"
        val entities =
            listOf(createEntity(hwModel, "t-deck", "T-Deck"), createEntity(hwModel, "tdeck-pro", "T-Deck Pro"))

        coEvery { localDataSource.getByHwModel(hwModel) } returns entities
        every { bootloaderOtaQuirksJsonDataSource.loadBootloaderOtaQuirksFromJsonAsset() } returns emptyList()

        val result = repository.getDeviceHardwareByModel(hwModel, target).getOrNull()

        assertEquals("T-Deck Pro", result?.displayName)
        assertEquals("tdeck-pro", result?.platformioTarget)
    }

    @Test
    fun `getDeviceHardwareByModel falls back to first entity when target not found`() = runTest(testDispatcher) {
        val hwModel = 50
        val target = "unknown-variant"
        val entities =
            listOf(createEntity(hwModel, "t-deck", "T-Deck"), createEntity(hwModel, "t-deck-tft", "T-Deck TFT"))

        coEvery { localDataSource.getByHwModel(hwModel) } returns entities
        every { bootloaderOtaQuirksJsonDataSource.loadBootloaderOtaQuirksFromJsonAsset() } returns emptyList()

        val result = repository.getDeviceHardwareByModel(hwModel, target).getOrNull()

        assertEquals("T-Deck", result?.displayName)
    }

    @Test
    fun `getDeviceHardwareByModel falls back to target lookup when hwModel not found`() = runTest(testDispatcher) {
        val hwModel = 0 
        val target = "tdeck-pro"
        val entity = createEntity(102, "tdeck-pro", "T-Deck Pro")

        coEvery { localDataSource.getByHwModel(hwModel) } returns emptyList()
        coEvery { localDataSource.getByTarget(target) } returns entity
        every { bootloaderOtaQuirksJsonDataSource.loadBootloaderOtaQuirksFromJsonAsset() } returns emptyList()

        val result = repository.getDeviceHardwareByModel(hwModel, target).getOrNull()

        assertEquals("T-Deck Pro", result?.displayName)
        assertEquals("tdeck-pro", result?.platformioTarget)
    }

    @Test
    fun `getDeviceHardwareByModel correctly sets isEsp32Arc for ESP32 devices`() = runTest(testDispatcher) {
        val hwModel = 50
        val entities = listOf(createEntity(hwModel, "t-deck", "T-Deck").copy(architecture = "esp32-s3"))

        coEvery { localDataSource.getByHwModel(hwModel) } returns entities
        every { bootloaderOtaQuirksJsonDataSource.loadBootloaderOtaQuirksFromJsonAsset() } returns emptyList()

        val result = repository.getDeviceHardwareByModel(hwModel).getOrNull()

        assertEquals(true, result?.isEsp32Arc)
    }

    private fun createEntity(hwModel: Int, target: String, displayName: String) = DeviceHardwareEntity(
        activelySupported = true,
        architecture = "esp32-s3",
        displayName = displayName,
        hwModel = hwModel,
        hwModelSlug = "T_DECK",
        images = listOf("image.svg"), 
        platformioTarget = target,
        requiresDfu = false,
        supportLevel = 0,
        tags = emptyList(),
        lastUpdated = System.currentTimeMillis(),
    )
}

