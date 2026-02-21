package org.meshtastic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "triage_pin",
    indices = [
        Index(value = ["triage_level"]),
        Index(value = ["timestamp"]),
        Index(value = ["claimed_by"]),
    ],
)
data class TriagePinEntity(
    @PrimaryKey
    @ColumnInfo(name = "pin_id")
    val pinId: String,

    @ColumnInfo(name = "lat")
    val lat: Double,

    @ColumnInfo(name = "lon")
    val lon: Double,

    @ColumnInfo(name = "triage_level")
    val triageLevel: String,

    @ColumnInfo(name = "victim_count")
    val victimCount: Int = 1,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "claimed_by")
    val claimedBy: String? = null,

    @ColumnInfo(name = "claim_timestamp")
    val claimTimestamp: Long? = null,

    @ColumnInfo(name = "is_silent_node_conversion")
    val isSilentNodeConversion: Boolean = false,

    @ColumnInfo(name = "source_node_num")
    val sourceNodeNum: Int? = null,
)
