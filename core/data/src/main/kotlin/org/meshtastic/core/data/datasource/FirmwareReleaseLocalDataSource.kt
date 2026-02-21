
package org.meshtastic.core.data.datasource

import dagger.Lazy
import kotlinx.coroutines.withContext
import org.meshtastic.core.database.dao.FirmwareReleaseDao
import org.meshtastic.core.database.entity.FirmwareReleaseEntity
import org.meshtastic.core.database.entity.FirmwareReleaseType
import org.meshtastic.core.database.entity.asDeviceVersion
import org.meshtastic.core.database.entity.asEntity
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.NetworkFirmwareRelease
import javax.inject.Inject

class FirmwareReleaseLocalDataSource
@Inject
constructor(
    private val firmwareReleaseDaoLazy: Lazy<FirmwareReleaseDao>,
    private val dispatchers: CoroutineDispatchers,
) {
    private val firmwareReleaseDao by lazy { firmwareReleaseDaoLazy.get() }

    suspend fun insertFirmwareReleases(
        firmwareReleases: List<NetworkFirmwareRelease>,
        releaseType: FirmwareReleaseType,
    ) = withContext(dispatchers.io) {
        firmwareReleases.forEach { firmwareRelease ->
            firmwareReleaseDao.insert(firmwareRelease.asEntity(releaseType))
        }
    }

    suspend fun deleteAllFirmwareReleases() = withContext(dispatchers.io) { firmwareReleaseDao.deleteAll() }

    suspend fun getLatestRelease(releaseType: FirmwareReleaseType): FirmwareReleaseEntity? =
        withContext(dispatchers.io) {
            val releases = firmwareReleaseDao.getReleasesByType(releaseType)
            if (releases.isEmpty()) {
                return@withContext null
            } else {
                val latestRelease = releases.maxBy { it.asDeviceVersion() }
                return@withContext latestRelease
            }
        }
}

