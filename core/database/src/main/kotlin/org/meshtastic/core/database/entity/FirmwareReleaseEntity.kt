
package org.meshtastic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.meshtastic.core.model.DeviceVersion
import org.meshtastic.core.model.NetworkFirmwareRelease

@Serializable
@Entity(tableName = "firmware_release")
data class FirmwareReleaseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = "",
    @ColumnInfo(name = "page_url") val pageUrl: String = "",
    @ColumnInfo(name = "release_notes") val releaseNotes: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "zip_url") val zipUrl: String = "",
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "release_type") val releaseType: FirmwareReleaseType = FirmwareReleaseType.STABLE,
)

fun NetworkFirmwareRelease.asEntity(releaseType: FirmwareReleaseType) = FirmwareReleaseEntity(
    id = id,
    pageUrl = pageUrl,
    releaseNotes = releaseNotes,
    title = title,
    zipUrl = zipUrl,
    lastUpdated = System.currentTimeMillis(),
    releaseType = releaseType,
)

fun FirmwareReleaseEntity.asExternalModel() = FirmwareRelease(
    id = id,
    pageUrl = pageUrl,
    releaseNotes = releaseNotes,
    title = title,
    zipUrl = zipUrl,
    lastUpdated = lastUpdated,
    releaseType = releaseType,
)

data class FirmwareRelease(
    val id: String = "",
    val pageUrl: String = "",
    val releaseNotes: String = "",
    val title: String = "",
    val zipUrl: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val releaseType: FirmwareReleaseType = FirmwareReleaseType.STABLE,
)

fun FirmwareReleaseEntity.asDeviceVersion(): DeviceVersion = DeviceVersion(id.substringBeforeLast(".").replace("v", ""))

fun FirmwareRelease.asDeviceVersion(): DeviceVersion = DeviceVersion(id.substringBeforeLast(".").replace("v", ""))

enum class FirmwareReleaseType {
    STABLE,
    ALPHA,
    LOCAL,
}

