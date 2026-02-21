
package org.meshtastic.core.data.datasource

import kotlinx.coroutines.withContext
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.MetadataEntity
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchingNodeInfoWriteDataSource
@Inject
constructor(
    private val dbManager: DatabaseManager,
    private val dispatchers: CoroutineDispatchers,
) : NodeInfoWriteDataSource {

    override suspend fun upsert(node: NodeEntity) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().upsert(node) } }

    override suspend fun installConfig(mi: MyNodeEntity, nodes: List<NodeEntity>) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().installConfig(mi, nodes) } }

    override suspend fun clearNodeDB(preserveFavorites: Boolean) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().clearNodeInfo(preserveFavorites) } }

    override suspend fun clearMyNodeInfo() =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().clearMyNodeInfo() } }

    override suspend fun deleteNode(num: Int) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().deleteNode(num) } }

    override suspend fun deleteNodes(nodeNums: List<Int>) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().deleteNodes(nodeNums) } }

    override suspend fun deleteMetadata(num: Int) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().deleteMetadata(num) } }

    override suspend fun upsert(metadata: MetadataEntity) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().upsert(metadata) } }

    override suspend fun setNodeNotes(num: Int, notes: String) =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().setNodeNotes(num, notes) } }

    override suspend fun backfillDenormalizedNames() =
        withContext(dispatchers.io) { dbManager.withDb { it.nodeInfoDao().backfillDenormalizedNames() } }
}

