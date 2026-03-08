

package org.meshtastic.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.meshtastic.proto.LocalOnlyProtos.LocalModuleConfig
import org.meshtastic.proto.ModuleConfigProtos.ModuleConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModuleConfigDataSource @Inject constructor(private val moduleConfigStore: DataStore<LocalModuleConfig>) {
    val moduleConfigFlow: Flow<LocalModuleConfig> =
        moduleConfigStore.data.catch { exception ->
            if (exception is IOException) {
                emit(LocalModuleConfig.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun clearLocalModuleConfig() {
        moduleConfigStore.updateData { preference -> preference.toBuilder().clear().build() }
    }

    suspend fun setLocalModuleConfig(config: ModuleConfig) = moduleConfigStore.updateData {
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

