

package org.meshtastic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.meshtastic.proto.MeshProtos

@Entity(
    tableName = "traceroute_node_position",
    primaryKeys = ["log_uuid", "node_num"],
    foreignKeys =
    [
        ForeignKey(
            entity = MeshLog::class,
            parentColumns = ["uuid"],
            childColumns = ["log_uuid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["log_uuid"]), Index(value = ["request_id"])],
)
data class TracerouteNodePositionEntity(
    @ColumnInfo(name = "log_uuid") val logUuid: String,
    @ColumnInfo(name = "request_id") val requestId: Int,
    @ColumnInfo(name = "node_num") val nodeNum: Int,
    @ColumnInfo(name = "position", typeAffinity = ColumnInfo.BLOB) val position: MeshProtos.Position,
)

