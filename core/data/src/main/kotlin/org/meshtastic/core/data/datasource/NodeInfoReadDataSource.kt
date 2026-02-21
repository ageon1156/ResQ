

package org.meshtastic.core.data.datasource

import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.database.entity.NodeWithRelations

interface NodeInfoReadDataSource {
    fun myNodeInfoFlow(): Flow<MyNodeEntity?>

    fun nodeDBbyNumFlow(): Flow<Map<Int, NodeWithRelations>>

    fun getNodesFlow(
        sort: String,
        filter: String,
        includeUnknown: Boolean,
        hopsAwayMax: Int,
        lastHeardMin: Int,
    ): Flow<List<NodeWithRelations>>

    suspend fun getNodesOlderThan(lastHeard: Int): List<NodeEntity>

    suspend fun getUnknownNodes(): List<NodeEntity>
}

