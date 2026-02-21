
package org.meshtastic.core.model.util

import org.meshtastic.core.model.BuildConfig
import org.meshtastic.proto.ConfigProtos
import org.meshtastic.proto.MeshProtos

val Any?.anonymize: String
    get() = this.anonymize()

fun Any?.anonymize(maxLen: Int = 3) = if (this != null) ("..." + this.toString().takeLast(maxLen)) else "null"

fun Any.toOneLineString() = this.toString().replace('\n', ' ')

fun ConfigProtos.Config.toOneLineString(): String {
    val redactedFields = """(wifi_psk:|public_key:|private_key:|admin_key:)\s*".*"""
    return this.toString()
        .replace(redactedFields.toRegex()) { "${it.groupValues[1]} \"[REDACTED]\"" }
        .replace('\n', ' ')
}

fun MeshProtos.MeshPacket.toOneLineString(): String {
    val redactedFields = """(public_key:|private_key:|admin_key:)\s*".*""" 
    return this.toString()
        .replace(redactedFields.toRegex()) { "${it.groupValues[1]} \"[REDACTED]\"" }
        .replace('\n', ' ')
}

fun MeshProtos.toOneLineString(): String {
    val redactedFields = """(public_key:|private_key:|admin_key:)\s*".*""" 
    return this.toString()
        .replace(redactedFields.toRegex()) { "${it.groupValues[1]} \"[REDACTED]\"" }
        .replace('\n', ' ')
}

fun Any.toPIIString() = if (!BuildConfig.DEBUG) {
    "<PII?>"
} else {
    this.toOneLineString()
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

private const val MPS_TO_KMPH = 3.6f
private const val KM_TO_MILES = 0.621371f

fun Int.mpsToKmph(): Float {
    
    val kmph = this * MPS_TO_KMPH
    return kmph
}

fun Int.mpsToMph(): Float {
    
    val mph = this * MPS_TO_KMPH * KM_TO_MILES
    return mph
}

