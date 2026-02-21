

package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.meshtastic.core.database.entity.FirmwareReleaseEntity
import org.meshtastic.core.database.entity.FirmwareReleaseType

@Dao
interface FirmwareReleaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(firmwareReleaseEntity: FirmwareReleaseEntity)

    @Query("DELETE FROM firmware_release")
    suspend fun deleteAll()

    @Query("SELECT * FROM firmware_release")
    suspend fun getAllReleases(): List<FirmwareReleaseEntity>

    @Query("SELECT * FROM firmware_release WHERE release_type = :releaseType")
    suspend fun getReleasesByType(releaseType: FirmwareReleaseType): List<FirmwareReleaseEntity>
}

