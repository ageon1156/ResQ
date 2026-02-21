

package org.meshtastic.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object LocalConfigSerializer : Serializer<LocalConfig> {
    override val defaultValue: LocalConfig = LocalConfig.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LocalConfig {
        try {
            return LocalConfig.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: LocalConfig, output: OutputStream) = t.writeTo(output)
}

