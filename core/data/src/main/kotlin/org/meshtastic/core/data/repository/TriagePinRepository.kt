package org.meshtastic.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.TriagePinEntity
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriagePinRepository @Inject constructor(
    private val dbManager: DatabaseManager,
    private val dispatchers: CoroutineDispatchers,
) {
    fun getTriagePinsFlow(): Flow<List<TriagePin>> =
        dbManager.currentDb
            .flatMapLatest { db -> db.triagePinDao().getAllFlow() }
            .map { entities -> entities.map { it.toDomain() } }

    suspend fun upsertPin(pin: TriagePin) = withContext(dispatchers.io) {
        dbManager.currentDb.value.triagePinDao().upsert(pin.toEntity())
    }

    suspend fun claimPin(pinId: String, rescuerId: String) = withContext(dispatchers.io) {
        dbManager.currentDb.value.triagePinDao().claimPin(
            pinId      = pinId,
            rescuerId  = rescuerId,
            ts         = System.currentTimeMillis(),
        )
    }

    suspend fun updateVictimCount(pinId: String, newCount: Int) = withContext(dispatchers.io) {
        dbManager.currentDb.value.triagePinDao().updateVictimCount(pinId, newCount)
    }

    suspend fun upgradeTriageLevel(pinId: String, newLevel: TriageLevel) = withContext(dispatchers.io) {
        dbManager.currentDb.value.triagePinDao().updateTriageLevel(pinId, newLevel.name)
    }

    suspend fun deletePin(pinId: String) = withContext(dispatchers.io) {
        dbManager.currentDb.value.triagePinDao().delete(pinId)
    }
}

private fun TriagePinEntity.toDomain(): TriagePin = TriagePin(
    pinId                  = pinId,
    lat                    = lat,
    lon                    = lon,
    triageLevel            = TriageLevel.fromString(triageLevel),
    victimCount            = victimCount,
    createdBy              = createdBy,
    timestamp              = timestamp,
    claimedBy              = claimedBy,
    claimTimestamp         = claimTimestamp,
    isSilentNodeConversion = isSilentNodeConversion,
    sourceNodeNum          = sourceNodeNum,
)

private fun TriagePin.toEntity(): TriagePinEntity = TriagePinEntity(
    pinId                  = pinId,
    lat                    = lat,
    lon                    = lon,
    triageLevel            = triageLevel.name,
    victimCount            = victimCount,
    createdBy              = createdBy,
    timestamp              = timestamp,
    claimedBy              = claimedBy,
    claimTimestamp         = claimTimestamp,
    isSilentNodeConversion = isSilentNodeConversion,
    sourceNodeNum          = sourceNodeNum,
)
