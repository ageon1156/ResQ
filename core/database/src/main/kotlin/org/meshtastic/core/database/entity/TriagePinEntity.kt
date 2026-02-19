package org.meshtastic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting triage incident pins.
 * Completely separate from the waypoint/custom-pin system.
 */
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

    /** Stores [TriageLevel.name]. */
    @ColumnInfo(name = "triage_level")
    val triageLevel: String,

    @ColumnInfo(name = "victim_count")
    val victimCount: Int = 1,

    /** nodeID string of the rescuer who created this pin, e.g. "!deadbeef". */
    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /** nodeID of the rescuer who claimed this pin; null = unclaimed. */
    @ColumnInfo(name = "claimed_by")
    val claimedBy: String? = null,

    @ColumnInfo(name = "claim_timestamp")
    val claimTimestamp: Long? = null,

    /** True when this pin was auto-created from a silent-node conversion. */
    @ColumnInfo(name = "is_silent_node_conversion")
    val isSilentNodeConversion: Boolean = false,

    /** Node number of the silent node this was converted from, if applicable. */
    @ColumnInfo(name = "source_node_num")
    val sourceNodeNum: Int? = null,
)
