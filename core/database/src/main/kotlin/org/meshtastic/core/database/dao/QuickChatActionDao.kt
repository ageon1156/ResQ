
package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.QuickChatAction

@Dao
interface QuickChatActionDao {

    @Query("Select * from quick_chat order by position asc")
    fun getAll(): Flow<List<QuickChatAction>>

    @Upsert suspend fun upsert(action: QuickChatAction)

    @Query("Delete from quick_chat")
    suspend fun deleteAll()

    @Query("Delete from quick_chat where uuid=:uuid")
    suspend fun delete(uuid: Long)

    @Transaction
    suspend fun delete(action: QuickChatAction) {
        delete(action.uuid)
        decrementPositionsAfter(action.position)
    }

    @Query("Update quick_chat set position=:position WHERE uuid=:uuid")
    suspend fun updateActionPosition(uuid: Long, position: Int)

    @Query("Update quick_chat set position=position-1 where position>=:position")
    suspend fun decrementPositionsAfter(position: Int)
}

