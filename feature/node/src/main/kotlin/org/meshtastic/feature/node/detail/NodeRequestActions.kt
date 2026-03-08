
package org.meshtastic.feature.node.detail

import android.os.RemoteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.service.ServiceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeRequestActions @Inject constructor(private val serviceRepository: ServiceRepository) {
    private var scope: CoroutineScope? = null

    fun start(coroutineScope: CoroutineScope) {
        scope = coroutineScope
    }

    fun requestUserInfo(destNum: Int) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.meshService?.requestUserInfo(destNum)
            } catch (ex: RemoteException) {
            }
        }
    }

    fun requestNeighborInfo(destNum: Int) {
        scope?.launch(Dispatchers.IO) {
            try {
                val packetId = serviceRepository.meshService?.packetId ?: return@launch
                serviceRepository.meshService?.requestNeighborInfo(packetId, destNum)
            } catch (ex: RemoteException) {
            }
        }
    }

    fun requestPosition(destNum: Int, position: Position = Position(0.0, 0.0, 0)) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.meshService?.requestPosition(destNum, position)
            } catch (ex: RemoteException) {
            }
        }
    }

    fun requestTelemetry(destNum: Int, type: TelemetryType) {
        scope?.launch(Dispatchers.IO) {
            try {
                val packetId = serviceRepository.meshService?.packetId ?: return@launch
                serviceRepository.meshService?.requestTelemetry(packetId, destNum, type.ordinal)
            } catch (ex: RemoteException) {
            }
        }
    }

    fun requestTraceroute(destNum: Int) {
        scope?.launch(Dispatchers.IO) {
            try {
                val packetId = serviceRepository.meshService?.packetId ?: return@launch
                serviceRepository.meshService?.requestTraceroute(packetId, destNum)
            } catch (ex: RemoteException) {
            }
        }
    }
}

