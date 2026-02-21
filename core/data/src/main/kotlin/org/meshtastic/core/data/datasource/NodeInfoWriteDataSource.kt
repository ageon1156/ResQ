
package org.meshtastic.core.data.datasource

import org.meshtastic.core.database.entity.MetadataEntity
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.entity.NodeEntity

interface NodeInfoWriteDataSource {
    suspend fun upsert(node: NodeEntity)

    suspend fun installConfig(mi: MyNodeEntity, nodes: List<NodeEntity>)

    suspend fun clearNodeDB(preserveFavorites: Boolean)

    suspend fun clearMyNodeInfo()

    suspend fun deleteNode(num: Int)

    suspend fun deleteNodes(nodeNums: List<Int>)

    suspend fun deleteMetadata(num: Int)

    suspend fun upsert(metadata: MetadataEntity)

    suspend fun setNodeNotes(num: Int, notes: String)

    suspend fun backfillDenormalizedNames()
}

