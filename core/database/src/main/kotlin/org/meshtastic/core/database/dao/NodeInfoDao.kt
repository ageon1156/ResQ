
package org.meshtastic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.database.entity.MetadataEntity
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.database.entity.NodeWithRelations
import org.meshtastic.proto.MeshProtos

@Suppress("TooManyFunctions")
@Dao
interface NodeInfoDao {

    private suspend fun getVerifiedNodeForUpsert(incomingNode: NodeEntity): NodeEntity {

        incomingNode.publicKey = incomingNode.user.publicKey

        if (incomingNode.user.hwModel != MeshProtos.HardwareModel.UNSET) {
            incomingNode.longName = incomingNode.user.longName
            incomingNode.shortName = incomingNode.user.shortName
        } else {
            incomingNode.longName = null
            incomingNode.shortName = null
        }

        val existingNodeEntity = getNodeByNum(incomingNode.num)?.node

        return if (existingNodeEntity == null) {
            handleNewNodeUpsertValidation(incomingNode)
        } else {
            handleExistingNodeUpsertValidation(existingNodeEntity, incomingNode)
        }
    }

    private suspend fun handleNewNodeUpsertValidation(newNode: NodeEntity): NodeEntity {

        if (newNode.publicKey?.isEmpty == false) {
            val nodeWithSamePK = findNodeByPublicKey(newNode.publicKey)
            if (nodeWithSamePK != null && nodeWithSamePK.num != newNode.num) {
                
                return nodeWithSamePK
            }
        }

        return newNode
    }

    private fun handleExistingNodeUpsertValidation(existingNode: NodeEntity, incomingNode: NodeEntity): NodeEntity {
        val isPlaceholder = incomingNode.user.hwModel == MeshProtos.HardwareModel.UNSET
        val hasExistingUser = existingNode.user.hwModel != MeshProtos.HardwareModel.UNSET
        val isDefaultName = incomingNode.user.longName.matches(Regex("^Meshtastic [0-9a-fA-F]{4}$"))

        val shouldPreserve = hasExistingUser && isPlaceholder && isDefaultName

        if (shouldPreserve) {
            
            val resolvedNotes = if (incomingNode.notes.isBlank()) existingNode.notes else incomingNode.notes
            return existingNode.copy(
                lastHeard = incomingNode.lastHeard,
                snr = incomingNode.snr,
                rssi = incomingNode.rssi,
                position = incomingNode.position,
                hopsAway = incomingNode.hopsAway,
                deviceTelemetry = incomingNode.deviceTelemetry,
                environmentTelemetry = incomingNode.environmentTelemetry,
                powerTelemetry = incomingNode.powerTelemetry,
                paxcounter = incomingNode.paxcounter,
                channel = incomingNode.channel,
                viaMqtt = incomingNode.viaMqtt,
                isFavorite = incomingNode.isFavorite,
                isIgnored = incomingNode.isIgnored,
                isMuted = incomingNode.isMuted,
                notes = resolvedNotes,
            )
        }

        val existingResolvedKey = existingNode.publicKey ?: existingNode.user.publicKey
        val isPublicKeyMatchingOrExistingIsEmpty = existingResolvedKey == incomingNode.publicKey || !existingNode.hasPKC

        val resolvedNotes = if (incomingNode.notes.isBlank()) existingNode.notes else incomingNode.notes

        return if (isPublicKeyMatchingOrExistingIsEmpty) {

            incomingNode.copy(notes = resolvedNotes)
        } else {

            incomingNode.copy(
                user = incomingNode.user.toBuilder().setPublicKey(NodeEntity.ERROR_BYTE_STRING).build(),
                publicKey = NodeEntity.ERROR_BYTE_STRING,
                notes = resolvedNotes,
            )
        }
    }

    @Query("SELECT * FROM my_node")
    fun getMyNodeInfo(): Flow<MyNodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMyNodeInfo(myInfo: MyNodeEntity)

    @Query("DELETE FROM my_node")
    suspend fun clearMyNodeInfo()

    @Query(
        """
        SELECT * FROM nodes
        ORDER BY CASE
            WHEN num = (SELECT myNodeNum FROM my_node LIMIT 1) THEN 0
            ELSE 1
        END,
        last_heard DESC
        """,
    )
    @Transaction
    fun nodeDBbyNum(): Flow<
        Map<
            @MapColumn(columnName = "num")
            Int,
            NodeWithRelations,
            >,
        >

    @Query(
        """
    WITH OurNode AS (
        SELECT latitude, longitude
        FROM nodes
        WHERE num = (SELECT myNodeNum FROM my_node LIMIT 1)
    )
    SELECT * FROM nodes
    WHERE (:includeUnknown = 1 OR short_name IS NOT NULL)
        AND (:filter = ''
            OR (long_name LIKE '%' || :filter || '%'
            OR short_name LIKE '%' || :filter || '%'
            OR printf('!%08x', CASE WHEN num < 0 THEN num + 4294967296 ELSE num END) LIKE '%' || :filter || '%'
            OR CAST(CASE WHEN num < 0 THEN num + 4294967296 ELSE num END AS TEXT) LIKE '%' || :filter || '%'))
        AND (:lastHeardMin = -1 OR last_heard >= :lastHeardMin)
        AND (:hopsAwayMax = -1 OR (hops_away <= :hopsAwayMax AND hops_away >= 0) OR num = (SELECT myNodeNum FROM my_node LIMIT 1))
    ORDER BY CASE
        WHEN num = (SELECT myNodeNum FROM my_node LIMIT 1) THEN 0
        ELSE 1
    END,
    CASE
        WHEN :sort = 'last_heard' THEN last_heard * -1
        WHEN :sort = 'alpha' THEN UPPER(long_name)
        WHEN :sort = 'distance' THEN
            CASE
                WHEN latitude IS NULL OR longitude IS NULL OR
                    (latitude = 0.0 AND longitude = 0.0) THEN 999999999
                ELSE
                    (latitude - (SELECT latitude FROM OurNode)) *
                    (latitude - (SELECT latitude FROM OurNode)) +
                    (longitude - (SELECT longitude FROM OurNode)) *
                    (longitude - (SELECT longitude FROM OurNode))
            END
        WHEN :sort = 'hops_away' THEN
            CASE
                WHEN hops_away = -1 THEN 999999999
                ELSE hops_away
            END
        WHEN :sort = 'channel' THEN channel
        WHEN :sort = 'via_mqtt' THEN via_mqtt
        WHEN :sort = 'via_favorite' THEN is_favorite * -1
        ELSE 0
    END ASC,
    last_heard DESC
    """,
    )
    @Transaction
    fun getNodes(
        sort: String,
        filter: String,
        includeUnknown: Boolean,
        hopsAwayMax: Int,
        lastHeardMin: Int,
    ): Flow<List<NodeWithRelations>>

    @Transaction
    suspend fun clearNodeInfo(preserveFavorites: Boolean) {
        if (preserveFavorites) {
            deleteNonFavoriteNodes()
        } else {
            deleteAllNodes()
        }
    }

    @Query("DELETE FROM nodes WHERE is_favorite = 0")
    suspend fun deleteNonFavoriteNodes()

    @Query("DELETE FROM nodes")
    suspend fun deleteAllNodes()

    @Query("DELETE FROM nodes WHERE num=:num")
    suspend fun deleteNode(num: Int)

    @Query("DELETE FROM nodes WHERE num IN (:nodeNums)")
    suspend fun deleteNodes(nodeNums: List<Int>)

    @Query("SELECT * FROM nodes WHERE last_heard < :lastHeard")
    suspend fun getNodesOlderThan(lastHeard: Int): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE short_name IS NULL")
    suspend fun getUnknownNodes(): List<NodeEntity>

    @Upsert suspend fun upsert(meta: MetadataEntity)

    @Query("DELETE FROM metadata WHERE num=:num")
    suspend fun deleteMetadata(num: Int)

    @Query("SELECT * FROM nodes WHERE num=:num")
    @Transaction
    suspend fun getNodeByNum(num: Int): NodeWithRelations?

    @Query("SELECT * FROM nodes WHERE public_key = :publicKey LIMIT 1")
    suspend fun findNodeByPublicKey(publicKey: ByteString?): NodeEntity?

    @Upsert suspend fun doUpsert(node: NodeEntity)

    suspend fun upsert(node: NodeEntity) {
        val verifiedNode = getVerifiedNodeForUpsert(node)
        doUpsert(verifiedNode)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(nodes: List<NodeEntity>)

    @Query("UPDATE nodes SET notes = :notes WHERE num = :num")
    suspend fun setNodeNotes(num: Int, notes: String)

    @Transaction
    suspend fun installConfig(mi: MyNodeEntity, nodes: List<NodeEntity>) {
        clearMyNodeInfo()
        setMyNodeInfo(mi)
        putAll(nodes.map { getVerifiedNodeForUpsert(it) })
    }

    @Transaction
    suspend fun backfillDenormalizedNames() {
        val nodes = getAllNodesSnapshot()
        val nodesToUpdate =
            nodes
                .filter { node ->
                    
                    (node.longName == null || node.shortName == null) &&
                        node.user.hwModel != MeshProtos.HardwareModel.UNSET
                }
                .map { node -> node.copy(longName = node.user.longName, shortName = node.user.shortName) }
        if (nodesToUpdate.isNotEmpty()) {
            putAll(nodesToUpdate)
        }
    }

    @Query("SELECT * FROM nodes")
    suspend fun getAllNodesSnapshot(): List<NodeEntity>
}

