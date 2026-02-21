
package org.meshtastic.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import org.meshtastic.core.data.datasource.BootloaderOtaQuirksJsonDataSource
import org.meshtastic.core.data.datasource.DeviceHardwareJsonDataSource
import org.meshtastic.core.data.datasource.DeviceHardwareLocalDataSource
import org.meshtastic.core.database.entity.DeviceHardwareEntity
import org.meshtastic.core.database.entity.asExternalModel
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.BootloaderOtaQuirk
import org.meshtastic.core.model.DeviceHardware
import org.meshtastic.core.network.DeviceHardwareRemoteDataSource
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHardwareRepository
@Inject
constructor(
    private val remoteDataSource: DeviceHardwareRemoteDataSource,
    private val localDataSource: DeviceHardwareLocalDataSource,
    private val jsonDataSource: DeviceHardwareJsonDataSource,
    private val bootloaderOtaQuirksJsonDataSource: BootloaderOtaQuirksJsonDataSource,
    private val dispatchers: CoroutineDispatchers,
) {

    @Suppress("LongMethod", "detekt:CyclomaticComplexMethod")
    suspend fun getDeviceHardwareByModel(
        hwModel: Int,
        target: String? = null,
        forceRefresh: Boolean = false,
    ): Result<DeviceHardware?> = withContext(dispatchers.io) {
        Logger.d {
            "DeviceHardwareRepository: getDeviceHardwareByModel(hwModel=$hwModel," +
                " target=$target, forceRefresh=$forceRefresh)"
        }

        val quirks = loadQuirks()

        if (forceRefresh) {
            Logger.d { "DeviceHardwareRepository: forceRefresh=true, clearing local device hardware cache" }
            localDataSource.deleteAllDeviceHardware()
        } else {
            
            var cachedEntities = localDataSource.getByHwModel(hwModel)

            if (cachedEntities.isEmpty() && target != null) {
                Logger.d {
                    "DeviceHardwareRepository: no cache for hwModel=$hwModel, trying target lookup for $target"
                }
                val byTarget = localDataSource.getByTarget(target)
                if (byTarget != null) {
                    cachedEntities = listOf(byTarget)
                }
            }

            if (cachedEntities.isNotEmpty() && cachedEntities.all { !it.isStale() }) {
                Logger.d { "DeviceHardwareRepository: using fresh cached device hardware for hwModel=$hwModel" }
                val matched = disambiguate(cachedEntities, target)
                return@withContext Result.success(
                    applyBootloaderQuirk(hwModel, matched?.asExternalModel(), quirks, target),
                )
            }
            Logger.d { "DeviceHardwareRepository: no fresh cache for hwModel=$hwModel, attempting remote fetch" }
        }

        runCatching {
            Logger.d { "DeviceHardwareRepository: fetching device hardware from remote API" }
            val remoteHardware = remoteDataSource.getAllDeviceHardware()
            Logger.d {
                "DeviceHardwareRepository: remote API returned ${remoteHardware.size} device hardware entries"
            }

            localDataSource.insertAllDeviceHardware(remoteHardware)
            var fromDb = localDataSource.getByHwModel(hwModel)

            if (fromDb.isEmpty() && target != null) {
                val byTarget = localDataSource.getByTarget(target)
                if (byTarget != null) fromDb = listOf(byTarget)
            }

            Logger.d {
                "DeviceHardwareRepository: lookup after remote fetch for hwModel=$hwModel returned" +
                    " ${fromDb.size} entries"
            }
            disambiguate(fromDb, target)?.asExternalModel()
        }
            .onSuccess {
                
                return@withContext Result.success(applyBootloaderQuirk(hwModel, it, quirks, target))
            }
            .onFailure { e ->
                Logger.w(e) {
                    "DeviceHardwareRepository: failed to fetch device hardware from server for hwModel=$hwModel"
                }

                var staleEntities = localDataSource.getByHwModel(hwModel)
                if (staleEntities.isEmpty() && target != null) {
                    val byTarget = localDataSource.getByTarget(target)
                    if (byTarget != null) staleEntities = listOf(byTarget)
                }

                if (staleEntities.isNotEmpty() && staleEntities.all { !it.isIncomplete() }) {
                    Logger.d { "DeviceHardwareRepository: using stale cached device hardware for hwModel=$hwModel" }
                    val matched = disambiguate(staleEntities, target)
                    return@withContext Result.success(
                        applyBootloaderQuirk(hwModel, matched?.asExternalModel(), quirks, target),
                    )
                }

                Logger.d {
                    "DeviceHardwareRepository: cache ${if (staleEntities.isEmpty()) "empty" else "incomplete"} " +
                        "for hwModel=$hwModel, falling back to bundled JSON asset"
                }
                return@withContext loadFromBundledJson(hwModel, target, quirks)
            }
    }

    private suspend fun loadFromBundledJson(
        hwModel: Int,
        target: String?,
        quirks: List<BootloaderOtaQuirk>,
    ): Result<DeviceHardware?> = runCatching {
        Logger.d { "DeviceHardwareRepository: loading device hardware from bundled JSON for hwModel=$hwModel" }
        val jsonHardware = jsonDataSource.loadDeviceHardwareFromJsonAsset()
        Logger.d {
            "DeviceHardwareRepository: bundled JSON returned ${jsonHardware.size} device hardware entries"
        }

        localDataSource.insertAllDeviceHardware(jsonHardware)
        var baseList = localDataSource.getByHwModel(hwModel)

        if (baseList.isEmpty() && target != null) {
            val byTarget = localDataSource.getByTarget(target)
            if (byTarget != null) baseList = listOf(byTarget)
        }

        Logger.d {
            "DeviceHardwareRepository: lookup after JSON load for hwModel=$hwModel returned ${baseList.size} entries"
        }

        val matched = disambiguate(baseList, target)
        applyBootloaderQuirk(hwModel, matched?.asExternalModel(), quirks, target)
    }
        .also { result ->
            result.exceptionOrNull()?.let { e ->
                Logger.e(e) {
                    "DeviceHardwareRepository: failed to load device hardware from bundled JSON for hwModel=$hwModel"
                }
            }
        }

    private fun disambiguate(entities: List<DeviceHardwareEntity>, target: String?): DeviceHardwareEntity? = when {
        entities.isEmpty() -> null
        target == null -> entities.first()
        else -> {
            entities.find { it.platformioTarget == target }
                ?: entities.find { it.platformioTarget.equals(target, ignoreCase = true) }
                ?: entities.first()
        }
    }

    private fun DeviceHardwareEntity.isIncomplete(): Boolean =
        displayName.isBlank() || platformioTarget.isBlank() || images.isNullOrEmpty()

    private fun DeviceHardwareEntity.isStale(): Boolean =
        isIncomplete() || (System.currentTimeMillis() - this.lastUpdated) > CACHE_EXPIRATION_TIME_MS

    private fun loadQuirks(): List<BootloaderOtaQuirk> {
        val quirks = bootloaderOtaQuirksJsonDataSource.loadBootloaderOtaQuirksFromJsonAsset()
        Logger.d { "DeviceHardwareRepository: loaded ${quirks.size} bootloader quirks" }
        return quirks
    }

    private fun applyBootloaderQuirk(
        hwModel: Int,
        base: DeviceHardware?,
        quirks: List<BootloaderOtaQuirk>,
        reportedTarget: String? = null,
    ): DeviceHardware? {
        if (base == null) return null

        val matchedQuirk = quirks.firstOrNull { it.hwModel == hwModel }
        val result =
            if (matchedQuirk != null) {
                Logger.d {
                    "DeviceHardwareRepository: applying quirk: " +
                        "requiresBootloaderUpgradeForOta=${matchedQuirk.requiresBootloaderUpgradeForOta}, " +
                        "infoUrl=${matchedQuirk.infoUrl}"
                }
                base.copy(
                    requiresBootloaderUpgradeForOta = matchedQuirk.requiresBootloaderUpgradeForOta,
                    bootloaderInfoUrl = matchedQuirk.infoUrl,
                )
            } else {
                base
            }

        return if (reportedTarget != null) {
            Logger.d { "DeviceHardwareRepository: using reported target $reportedTarget for hardware info" }
            result.copy(platformioTarget = reportedTarget)
        } else {
            result
        }
    }

    companion object {
        private val CACHE_EXPIRATION_TIME_MS = TimeUnit.DAYS.toMillis(1)
    }
}

