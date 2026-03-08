

package org.meshtastic.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.meshtastic.proto.ConfigProtos.Config
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalConfigDataSource @Inject constructor(private val localConfigStore: DataStore<LocalConfig>) {
    val localConfigFlow: Flow<LocalConfig> =
        localConfigStore.data.catch { exception ->
            if (exception is IOException) {
                emit(LocalConfig.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun clearLocalConfig() {
        localConfigStore.updateData { preference -> preference.toBuilder().clear().build() }
    }

    suspend fun setLocalConfig(config: Config) = localConfigStore.updateData {
        val builder = it.toBuilder()
        config.allFields.forEach { (field, value) ->
            val localField = it.descriptorForType.findFieldByName(field.name)
            if (localField != null) {
                builder.setField(localField, value)
            }
        }
        builder.build()
    }
}

