

package org.meshtastic.core.common

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

fun xorHash(b: ByteArray) = b.fold(0) { acc, x -> acc xor (x.toInt() and BYTE_MASK) }

private const val BYTE_MASK = 0xff

