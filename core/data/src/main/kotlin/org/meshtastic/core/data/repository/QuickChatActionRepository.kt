

package org.meshtastic.core.data.repository

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.QuickChatAction
import org.meshtastic.core.di.CoroutineDispatchers
import javax.inject.Inject

class QuickChatActionRepository
@Inject
constructor(
    private val dbManager: DatabaseManager,
    private val dispatchers: CoroutineDispatchers,
) {
    fun getAllActions() = dbManager.currentDb.flatMapLatest { it.quickChatActionDao().getAll() }.flowOn(dispatchers.io)

    suspend fun upsert(action: QuickChatAction) =
        withContext(dispatchers.io) { dbManager.currentDb.value.quickChatActionDao().upsert(action) }

    suspend fun deleteAll() = withContext(dispatchers.io) { dbManager.currentDb.value.quickChatActionDao().deleteAll() }

    suspend fun delete(action: QuickChatAction) =
        withContext(dispatchers.io) { dbManager.currentDb.value.quickChatActionDao().delete(action) }

    suspend fun setItemPosition(uuid: Long, newPos: Int) = withContext(dispatchers.io) {
        dbManager.currentDb.value.quickChatActionDao().updateActionPosition(uuid, newPos)
    }
}

