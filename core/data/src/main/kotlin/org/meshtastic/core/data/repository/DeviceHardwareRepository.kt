
package org.meshtastic.core.data.repository

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
        val quirks = loadQuirks()

        if (forceRefresh) {
            localDataSource.deleteAllDeviceHardware()
        } else {

            var cachedEntities = localDataSource.getByHwModel(hwModel)

            if (cachedEntities.isEmpty() && target != null) {
                val byTarget = localDataSource.getByTarget(target)
                if (byTarget != null) {
                    cachedEntities = listOf(byTarget)
                }
            }

            if (cachedEntities.isNotEmpty() && cachedEntities.all { !it.isStale() }) {
                val matched = disambiguate(cachedEntities, target)
                return@withContext Result.success(
                    applyBootloaderQuirk(hwModel, matched?.asExternalModel(), quirks, target),
                )
            }
        }

        runCatching {
            val remoteHardware = remoteDataSource.getAllDeviceHardware()

            localDataSource.insertAllDeviceHardware(remoteHardware)
            var fromDb = localDataSource.getByHwModel(hwModel)

            if (fromDb.isEmpty() && target != null) {
                val byTarget = localDataSource.getByTarget(target)
                if (byTarget != null) fromDb = listOf(byTarget)
            }

            disambiguate(fromDb, target)?.asExternalModel()
        }
            .onSuccess {

                return@withContext Result.success(applyBootloaderQuirk(hwModel, it, quirks, target))
            }
            .onFailure { e ->
                var staleEntities = localDataSource.getByHwModel(hwModel)
                if (staleEntities.isEmpty() && target != null) {
                    val byTarget = localDataSource.getByTarget(target)
                    if (byTarget != null) staleEntities = listOf(byTarget)
                }

                if (staleEntities.isNotEmpty() && staleEntities.all { !it.isIncomplete() }) {
                    val matched = disambiguate(staleEntities, target)
                    return@withContext Result.success(
                        applyBootloaderQuirk(hwModel, matched?.asExternalModel(), quirks, target),
                    )
                }

                return@withContext loadFromBundledJson(hwModel, target, quirks)
            }
    }

    private suspend fun loadFromBundledJson(
        hwModel: Int,
        target: String?,
        quirks: List<BootloaderOtaQuirk>,
    ): Result<DeviceHardware?> = runCatching {
        val jsonHardware = jsonDataSource.loadDeviceHardwareFromJsonAsset()

        localDataSource.insertAllDeviceHardware(jsonHardware)
        var baseList = localDataSource.getByHwModel(hwModel)

        if (baseList.isEmpty() && target != null) {
            val byTarget = localDataSource.getByTarget(target)
            if (byTarget != null) baseList = listOf(byTarget)
        }

        val matched = disambiguate(baseList, target)
        applyBootloaderQuirk(hwModel, matched?.asExternalModel(), quirks, target)
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

    private fun loadQuirks(): List<BootloaderOtaQuirk> =
        bootloaderOtaQuirksJsonDataSource.loadBootloaderOtaQuirksFromJsonAsset()

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
                base.copy(
                    requiresBootloaderUpgradeForOta = matchedQuirk.requiresBootloaderUpgradeForOta,
                    bootloaderInfoUrl = matchedQuirk.infoUrl,
                )
            } else {
                base
            }

        return if (reportedTarget != null) {
            result.copy(platformioTarget = reportedTarget)
        } else {
            result
        }
    }

    companion object {
        private val CACHE_EXPIRATION_TIME_MS = TimeUnit.DAYS.toMillis(1)
    }
}

