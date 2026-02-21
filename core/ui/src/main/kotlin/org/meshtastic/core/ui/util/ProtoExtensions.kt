
package org.meshtastic.core.ui.util

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.unknown_age
import org.meshtastic.proto.ChannelProtos
import org.meshtastic.proto.ChannelProtos.ChannelSettings
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.MeshProtos.MeshPacket
import org.meshtastic.proto.MeshProtos.Position
import org.meshtastic.proto.channel
import org.meshtastic.proto.channelSettings
import kotlin.time.Duration.Companion.days

private const val SECONDS_TO_MILLIS = 1000L

@Composable
fun MeshProtos.Position.formatPositionTime(): String {
    val currentTime = System.currentTimeMillis()
    val sixMonthsAgo = currentTime - 180.days.inWholeMilliseconds
    val isOlderThanSixMonths = time * SECONDS_TO_MILLIS < sixMonthsAgo
    val timeText =
        if (isOlderThanSixMonths) {
            stringResource(Res.string.unknown_age)
        } else {
            DateUtils.formatDateTime(
                LocalContext.current,
                time * SECONDS_TO_MILLIS,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
            )
        }
    return timeText
}

fun MeshPacket.toPosition(): Position? = if (!decoded.wantResponse) {
    runCatching { Position.parseFrom(decoded.payload) }.getOrNull()
} else {
    null
}

fun getChannelList(new: List<ChannelSettings>, old: List<ChannelSettings>): List<ChannelProtos.Channel> = buildList {
    for (i in 0..maxOf(old.lastIndex, new.lastIndex)) {
        if (old.getOrNull(i) != new.getOrNull(i)) {
            add(
                channel {
                    role =
                        when (i) {
                            0 -> ChannelProtos.Channel.Role.PRIMARY
                            in 1..new.lastIndex -> ChannelProtos.Channel.Role.SECONDARY
                            else -> ChannelProtos.Channel.Role.DISABLED
                        }
                    index = i
                    settings = new.getOrNull(i) ?: channelSettings {}
                },
            )
        }
    }
}

