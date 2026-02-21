

package org.meshtastic.core.model.util

import android.util.Base64
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString

fun ByteString.encodeToString() = Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)

fun String.toByteString() = Base64.decode(this, Base64.NO_WRAP).toByteString()

