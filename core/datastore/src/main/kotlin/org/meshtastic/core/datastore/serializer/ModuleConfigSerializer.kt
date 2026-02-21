

package org.meshtastic.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import org.meshtastic.proto.LocalOnlyProtos.LocalModuleConfig
import java.io.InputStream
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object ModuleConfigSerializer : Serializer<LocalModuleConfig> {
    override val defaultValue: LocalModuleConfig = LocalModuleConfig.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LocalModuleConfig {
        try {
            return LocalModuleConfig.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: LocalModuleConfig, output: OutputStream) = t.writeTo(output)
}

