

package org.meshtastic.core.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.database.entity.NodeWithRelations
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchingNodeInfoReadDataSource @Inject constructor(private val dbManager: DatabaseManager) :
    NodeInfoReadDataSource {

    override fun myNodeInfoFlow(): Flow<MyNodeEntity?> =
        dbManager.currentDb.flatMapLatest { db -> db.nodeInfoDao().getMyNodeInfo() }

    override fun nodeDBbyNumFlow(): Flow<Map<Int, NodeWithRelations>> =
        dbManager.currentDb.flatMapLatest { db -> db.nodeInfoDao().nodeDBbyNum() }

    override fun getNodesFlow(
        sort: String,
        filter: String,
        includeUnknown: Boolean,
        hopsAwayMax: Int,
        lastHeardMin: Int,
    ): Flow<List<NodeWithRelations>> = dbManager.currentDb.flatMapLatest { db ->
        db.nodeInfoDao()
            .getNodes(
                sort = sort,
                filter = filter,
                includeUnknown = includeUnknown,
                hopsAwayMax = hopsAwayMax,
                lastHeardMin = lastHeardMin,
            )
    }

    override suspend fun getNodesOlderThan(lastHeard: Int): List<NodeEntity> =
        dbManager.withDb { it.nodeInfoDao().getNodesOlderThan(lastHeard) }

    override suspend fun getUnknownNodes(): List<NodeEntity> = dbManager.withDb { it.nodeInfoDao().getUnknownNodes() }
}

