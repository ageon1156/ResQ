
package org.meshtastic.core.ui.util

import android.text.format.DateUtils
import com.meshtastic.core.strings.getString
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.now
import java.lang.System.currentTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun formatAgo(lastSeenUnixSeconds: Int): String {
    val lastSeenDuration = lastSeenUnixSeconds.seconds
    val currentDuration = currentTimeMillis().milliseconds
    val diff = (currentDuration - lastSeenDuration).absoluteValue

    return if (diff < 1.minutes) {
        getString(Res.string.now)
    } else {
        DateUtils.getRelativeTimeSpanString(
            lastSeenDuration.inWholeMilliseconds,
            currentDuration.inWholeMilliseconds,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        )
            .toString()
    }
}

