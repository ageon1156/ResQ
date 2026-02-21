

package org.meshtastic.core.model.util

import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private const val ONLINE_WINDOW_HOURS = 2

fun getShortDate(time: Long): String? {
    val date = if (time != 0L) Date(time) else return null
    val isWithin24Hours = System.currentTimeMillis() - date.time <= TimeUnit.DAYS.toMillis(1)

    return if (isWithin24Hours) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
    } else {
        DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }
}

fun getShortDateTime(time: Long): String {
    val date = Date(time)
    val isWithin24Hours = System.currentTimeMillis() - date.time <= TimeUnit.DAYS.toMillis(1)

    return if (isWithin24Hours) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date)
    }
}

fun formatUptime(seconds: Int): String = formatUptime(seconds.toLong())

private fun formatUptime(seconds: Long): String {
    val days = TimeUnit.SECONDS.toDays(seconds)
    val hours = TimeUnit.SECONDS.toHours(seconds) % TimeUnit.DAYS.toHours(1)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % TimeUnit.HOURS.toMinutes(1)
    val secs = seconds % TimeUnit.MINUTES.toSeconds(1)

    return listOfNotNull(
        "${days}d".takeIf { days > 0 },
        "${hours}h".takeIf { hours > 0 },
        "${minutes}m".takeIf { minutes > 0 },
        "${secs}s".takeIf { secs > 0 },
    )
        .joinToString(" ")
}

fun onlineTimeThreshold(): Int {
    val currentSeconds = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)
    return (currentSeconds - TimeUnit.HOURS.toSeconds(ONLINE_WINDOW_HOURS.toLong())).toInt()
}

fun formatMuteRemainingTime(remainingMillis: Long): Pair<Int, Double> {
    if (remainingMillis <= 0) return Pair(0, 0.0)
    val totalHours = remainingMillis.toDouble() / TimeUnit.HOURS.toMillis(1)
    val days = (totalHours / TimeUnit.DAYS.toHours(1)).toInt()
    val hours = totalHours % TimeUnit.DAYS.toHours(1)
    return Pair(days, hours)
}

