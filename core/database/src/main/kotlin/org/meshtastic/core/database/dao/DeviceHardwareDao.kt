
package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.meshtastic.core.database.entity.DeviceHardwareEntity

@Dao
interface DeviceHardwareDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deviceHardware: DeviceHardwareEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deviceHardware: List<DeviceHardwareEntity>)

    @Query("SELECT * FROM device_hardware WHERE hwModel = :hwModel")
    suspend fun getByHwModel(hwModel: Int): List<DeviceHardwareEntity>

    @Query("SELECT * FROM device_hardware WHERE platformio_target = :target")
    suspend fun getByTarget(target: String): DeviceHardwareEntity?

    @Query("SELECT * FROM device_hardware WHERE hwModel = :hwModel AND platformio_target = :target")
    suspend fun getByModelAndTarget(hwModel: Int, target: String): DeviceHardwareEntity?

    @Query("DELETE FROM device_hardware")
    suspend fun deleteAll()
}

