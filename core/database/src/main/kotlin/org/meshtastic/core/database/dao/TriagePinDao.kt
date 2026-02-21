package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.TriagePinEntity

@Dao
interface TriagePinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pin: TriagePinEntity)

    @Query("SELECT * FROM triage_pin ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TriagePinEntity>>

    @Query("SELECT * FROM triage_pin WHERE pin_id = :pinId LIMIT 1")
    suspend fun getById(pinId: String): TriagePinEntity?

    @Query(
        "UPDATE triage_pin " +
            "SET claimed_by = :rescuerId, claim_timestamp = :ts " +
            "WHERE pin_id = :pinId AND (claimed_by IS NULL OR claimed_by = :rescuerId)",
    )
    suspend fun claimPin(pinId: String, rescuerId: String, ts: Long)

    @Query("UPDATE triage_pin SET victim_count = :newCount WHERE pin_id = :pinId")
    suspend fun updateVictimCount(pinId: String, newCount: Int)

    @Query("UPDATE triage_pin SET triage_level = :level WHERE pin_id = :pinId")
    suspend fun updateTriageLevel(pinId: String, level: String)

    @Query("DELETE FROM triage_pin WHERE pin_id = :pinId")
    suspend fun delete(pinId: String)

    @Query("DELETE FROM triage_pin")
    suspend fun deleteAll()
}
