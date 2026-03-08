

package org.meshtastic.core.model

data class DeviceVersion(val asString: String) : Comparable<DeviceVersion> {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    val asInt
        get() =
            try {
                verStringToInt(asString)
            } catch (e: Exception) {
                0
            }

    @Suppress("TooGenericExceptionThrown", "MagicNumber")
    private fun verStringToInt(s: String): Int {
        
        val versionString =
            if (s.split(".").size == 2) {
                "$s.0"
            } else {
                s
            }
        val match =
            Regex("(\\d{1,2}).(\\d{1,2}).(\\d{1,2})").find(versionString) ?: throw Exception("Can't parse version $s")
        val (major, minor, build) = match.destructured
        return major.toInt() * 10000 + minor.toInt() * 100 + build.toInt()
    }

    override fun compareTo(other: DeviceVersion): Int = asInt - other.asInt
}

