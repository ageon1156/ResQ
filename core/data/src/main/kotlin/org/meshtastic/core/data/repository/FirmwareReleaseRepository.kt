

package org.meshtastic.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.meshtastic.core.data.datasource.FirmwareReleaseJsonDataSource
import org.meshtastic.core.data.datasource.FirmwareReleaseLocalDataSource
import org.meshtastic.core.database.entity.FirmwareRelease
import org.meshtastic.core.database.entity.FirmwareReleaseEntity
import org.meshtastic.core.database.entity.FirmwareReleaseType
import org.meshtastic.core.database.entity.asExternalModel
import org.meshtastic.core.network.FirmwareReleaseRemoteDataSource
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirmwareReleaseRepository
@Inject
constructor(
    private val remoteDataSource: FirmwareReleaseRemoteDataSource,
    private val localDataSource: FirmwareReleaseLocalDataSource,
    private val jsonDataSource: FirmwareReleaseJsonDataSource,
) {

    val stableRelease: Flow<FirmwareRelease?> = getLatestFirmware(FirmwareReleaseType.STABLE)

    val alphaRelease: Flow<FirmwareRelease?> = getLatestFirmware(FirmwareReleaseType.ALPHA)

    private fun getLatestFirmware(
        releaseType: FirmwareReleaseType,
        forceRefresh: Boolean = false,
    ): Flow<FirmwareRelease?> = flow {
        if (forceRefresh) {
            invalidateCache()
        }

        val cachedRelease = localDataSource.getLatestRelease(releaseType)
        cachedRelease?.let {
            Logger.d { "Emitting cached firmware for $releaseType (isStale=${it.isStale()})" }
            emit(it.asExternalModel())
        }

        if (cachedRelease != null && !cachedRelease.isStale() && !forceRefresh) {
            return@flow
        }

        updateCacheFromSources()

        val finalRelease = localDataSource.getLatestRelease(releaseType)
        Logger.d { "Emitting final firmware for $releaseType from cache." }
        emit(finalRelease?.asExternalModel())
    }

    private suspend fun updateCacheFromSources() {
        val remoteFetchSuccess =
            runCatching {
                Logger.d { "Fetching fresh firmware releases from remote API." }
                val networkReleases = remoteDataSource.getFirmwareReleases()

                localDataSource.insertFirmwareReleases(networkReleases.releases.stable, FirmwareReleaseType.STABLE)
                localDataSource.insertFirmwareReleases(networkReleases.releases.alpha, FirmwareReleaseType.ALPHA)
            }
                .isSuccess

        if (!remoteFetchSuccess) {
            Logger.w { "Remote fetch failed, attempting to cache from bundled JSON." }
            runCatching {
                val jsonReleases = jsonDataSource.loadFirmwareReleaseFromJsonAsset()
                localDataSource.insertFirmwareReleases(jsonReleases.releases.stable, FirmwareReleaseType.STABLE)
                localDataSource.insertFirmwareReleases(jsonReleases.releases.alpha, FirmwareReleaseType.ALPHA)
            }
                .onFailure { Logger.w { "Failed to cache from JSON: ${it.message}" } }
        }
    }

    suspend fun invalidateCache() {
        localDataSource.deleteAllFirmwareReleases()
    }

    private fun FirmwareReleaseEntity.isStale(): Boolean =
        (System.currentTimeMillis() - this.lastUpdated) > CACHE_EXPIRATION_TIME_MS

    companion object {
        private val CACHE_EXPIRATION_TIME_MS = TimeUnit.HOURS.toMillis(1)
    }
}

