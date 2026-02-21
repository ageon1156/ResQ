
package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.MeshLog

@Dao
interface MeshLogDao {

    @Query("SELECT * FROM log ORDER BY received_date DESC LIMIT 0,:maxItem")
    fun getAllLogs(maxItem: Int): Flow<List<MeshLog>>

    @Query("SELECT * FROM log ORDER BY received_date ASC LIMIT 0,:maxItem")
    fun getAllLogsInReceiveOrder(maxItem: Int): Flow<List<MeshLog>>

    @Query(
        """
        SELECT * FROM log 
        WHERE from_num = :fromNum AND (:portNum = 0 AND port_num != 0 OR port_num = :portNum)
        ORDER BY received_date DESC LIMIT 0,:maxItem
        """,
    )
    fun getLogsFrom(fromNum: Int, portNum: Int, maxItem: Int): Flow<List<MeshLog>>

    @Insert suspend fun insert(log: MeshLog)

    @Query("DELETE FROM log")
    suspend fun deleteAll()

    @Query("DELETE FROM log WHERE uuid = :uuid")
    suspend fun deleteLog(uuid: String)

    @Query("DELETE FROM log WHERE from_num = :fromNum AND port_num = :portNum")
    suspend fun deleteLogs(fromNum: Int, portNum: Int)

    @Query("DELETE FROM log WHERE received_date < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}

