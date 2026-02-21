
package org.meshtastic.core.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.Portnums

private inline fun <reified T : Parcelable> Parcel.readParcelableCompat(loader: ClassLoader?): T? =
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        readParcelable(loader)
    } else {
        readParcelable(loader, T::class.java)
    }

@Parcelize
enum class MessageStatus : Parcelable {
    UNKNOWN, 
    RECEIVED, 
    QUEUED, 
    ENROUTE, 
    DELIVERED, 
    SFPP_ROUTING, 
    SFPP_CONFIRMED, 
    ERROR, 
}

@Serializable
data class DataPacket(
    var to: String? = ID_BROADCAST, 
    val bytes: ByteArray?,
    
    val dataType: Int,
    var from: String? = ID_LOCAL, 
    var time: Long = System.currentTimeMillis(), 
    var id: Int = 0, 
    var status: MessageStatus? = MessageStatus.UNKNOWN,
    var hopLimit: Int = 0,
    var channel: Int = 0, 
    var wantAck: Boolean = true, 
    var hopStart: Int = 0,
    var snr: Float = 0f,
    var rssi: Int = 0,
    var replyId: Int? = null, 
    var relayNode: Int? = null,
    var relays: Int = 0,
    var viaMqtt: Boolean = false, 
    var retryCount: Int = 0, 
    var emoji: Int = 0,
    var sfppHash: ByteArray? = null,
    var priority: Int = 0, 
) : Parcelable {

    var errorMessage: String? = null

    constructor(
        to: String?,
        channel: Int,
        text: String,
        replyId: Int? = null,
    ) : this(
        to = to,
        bytes = text.encodeToByteArray(),
        dataType = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
        channel = channel,
        replyId = replyId ?: 0,
    )

    val text: String?
        get() =
            if (dataType == Portnums.PortNum.TEXT_MESSAGE_APP_VALUE) {
                bytes?.decodeToString()
            } else {
                null
            }

    val alert: String?
        get() =
            if (dataType == Portnums.PortNum.ALERT_APP_VALUE) {
                bytes?.decodeToString()
            } else {
                null
            }

    constructor(
        to: String?,
        channel: Int,
        waypoint: MeshProtos.Waypoint,
    ) : this(to = to, bytes = waypoint.toByteArray(), dataType = Portnums.PortNum.WAYPOINT_APP_VALUE, channel = channel)

    val waypoint: MeshProtos.Waypoint?
        get() =
            if (dataType == Portnums.PortNum.WAYPOINT_APP_VALUE) {
                MeshProtos.Waypoint.parseFrom(bytes)
            } else {
                null
            }

    val hopsAway: Int
        get() = if (hopStart == 0 || hopLimit > hopStart) -1 else hopStart - hopLimit

    constructor(
        parcel: Parcel,
    ) : this(
        parcel.readString(),
        parcel.createByteArray(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readParcelableCompat(MessageStatus::class.java.classLoader),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt() == 1,
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readInt().let { if (it == 0) null else it },
        parcel.readInt().let { if (it == -1) null else it },
        parcel.readInt(), 
        parcel.readInt() == 1, 
        parcel.readInt(), 
        parcel.readInt(), 
        parcel.createByteArray(), 
        parcel.readInt(), 
    )

    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataPacket

        if (from != other.from) return false
        if (to != other.to) return false
        if (channel != other.channel) return false
        if (time != other.time) return false
        if (id != other.id) return false
        if (dataType != other.dataType) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (status != other.status) return false
        if (hopLimit != other.hopLimit) return false
        if (wantAck != other.wantAck) return false
        if (hopStart != other.hopStart) return false
        if (snr != other.snr) return false
        if (rssi != other.rssi) return false
        if (replyId != other.replyId) return false
        if (relayNode != other.relayNode) return false
        if (relays != other.relays) return false
        if (viaMqtt != other.viaMqtt) return false
        if (retryCount != other.retryCount) return false
        if (emoji != other.emoji) return false
        if (!sfppHash.contentEquals(other.sfppHash)) return false
        if (priority != other.priority) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from?.hashCode() ?: 0
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + time.hashCode()
        result = 31 * result + id
        result = 31 * result + dataType
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + hopLimit
        result = 31 * result + channel
        result = 31 * result + wantAck.hashCode()
        result = 31 * result + hopStart
        result = 31 * result + snr.hashCode()
        result = 31 * result + rssi
        result = 31 * result + (replyId ?: 0)
        result = 31 * result + (relayNode ?: -1)
        result = 31 * result + relays
        result = 31 * result + viaMqtt.hashCode()
        result = 31 * result + retryCount
        result = 31 * result + emoji
        result = 31 * result + (sfppHash?.contentHashCode() ?: 0)
        result = 31 * result + priority
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(to)
        parcel.writeByteArray(bytes)
        parcel.writeInt(dataType)
        parcel.writeString(from)
        parcel.writeLong(time)
        parcel.writeInt(id)
        parcel.writeParcelable(status, flags)
        parcel.writeInt(hopLimit)
        parcel.writeInt(channel)
        parcel.writeInt(if (wantAck) 1 else 0)
        parcel.writeInt(hopStart)
        parcel.writeFloat(snr)
        parcel.writeInt(rssi)
        parcel.writeInt(replyId ?: 0)
        parcel.writeInt(relayNode ?: -1)
        parcel.writeInt(relays)
        parcel.writeInt(if (viaMqtt) 1 else 0)
        parcel.writeInt(retryCount)
        parcel.writeInt(emoji)
        parcel.writeByteArray(sfppHash)
        parcel.writeInt(priority)
    }

    override fun describeContents(): Int = 0

    fun readFromParcel(parcel: Parcel) {
        to = parcel.readString()

        from = parcel.readString()
        time = parcel.readLong()
        id = parcel.readInt()
        status = parcel.readParcelableCompat(MessageStatus::class.java.classLoader)
        hopLimit = parcel.readInt()
        channel = parcel.readInt()
        wantAck = parcel.readInt() == 1
        hopStart = parcel.readInt()
        snr = parcel.readFloat()
        rssi = parcel.readInt()
        replyId = parcel.readInt().let { if (it == 0) null else it }
        relayNode = parcel.readInt().let { if (it == -1) null else it }
        relays = parcel.readInt()
        viaMqtt = parcel.readInt() == 1
        retryCount = parcel.readInt()
        emoji = parcel.readInt()
        sfppHash = parcel.createByteArray()
        priority = parcel.readInt()
    }

    companion object CREATOR : Parcelable.Creator<DataPacket> {

        const val ID_BROADCAST = "^all"

        const val ID_LOCAL = "^local"

        const val NODENUM_BROADCAST = (0xffffffff).toInt()

        const val PKC_CHANNEL_INDEX = 8

        fun nodeNumToDefaultId(n: Int): String = "!%08x".format(n)

        @Suppress("MagicNumber")
        fun idToDefaultNodeNum(id: String?): Int? = runCatching { id?.toLong(16)?.toInt() }.getOrNull()

        override fun createFromParcel(parcel: Parcel): DataPacket = DataPacket(parcel)

        override fun newArray(size: Int): Array<DataPacket?> = arrayOfNulls(size)
    }
}

