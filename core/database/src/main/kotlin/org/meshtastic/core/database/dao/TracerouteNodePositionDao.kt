

package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.TracerouteNodePositionEntity

@Dao
interface TracerouteNodePositionDao {

    @Query("SELECT * FROM traceroute_node_position WHERE log_uuid = :logUuid")
    fun getByLogUuid(logUuid: String): Flow<List<TracerouteNodePositionEntity>>

    @Query("DELETE FROM traceroute_node_position WHERE log_uuid = :logUuid")
    suspend fun deleteByLogUuid(logUuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TracerouteNodePositionEntity>)
}

