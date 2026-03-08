
package com.geeksville.mesh.service

import androidx.annotation.VisibleForTesting
import com.geeksville.mesh.model.NO_DEVICE_SELECTED
import com.google.protobuf.ByteString
import org.meshtastic.core.prefs.mesh.MeshPrefs
import org.meshtastic.proto.MeshProtos.MeshPacket
import org.meshtastic.proto.ModuleConfigProtos
import org.meshtastic.proto.Portnums
import org.meshtastic.proto.StoreAndForwardProtos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshHistoryManager
@Inject
constructor(
    private val meshPrefs: MeshPrefs,
    private val packetHandler: PacketHandler,
) {
    companion object {
        private const val DEFAULT_HISTORY_RETURN_WINDOW_MINUTES = 60 * 24
        private const val DEFAULT_HISTORY_RETURN_MAX_MESSAGES = 100

        @VisibleForTesting
        internal fun buildStoreForwardHistoryRequest(
            lastRequest: Int,
            historyReturnWindow: Int,
            historyReturnMax: Int,
        ): StoreAndForwardProtos.StoreAndForward {
            val historyBuilder = StoreAndForwardProtos.StoreAndForward.History.newBuilder()
            if (lastRequest > 0) historyBuilder.lastRequest = lastRequest
            if (historyReturnWindow > 0) historyBuilder.window = historyReturnWindow
            if (historyReturnMax > 0) historyBuilder.historyMessages = historyReturnMax
            return StoreAndForwardProtos.StoreAndForward.newBuilder()
                .setRr(StoreAndForwardProtos.StoreAndForward.RequestResponse.CLIENT_HISTORY)
                .setHistory(historyBuilder)
                .build()
        }

        @VisibleForTesting
        internal fun resolveHistoryRequestParameters(window: Int, max: Int): Pair<Int, Int> {
            val resolvedWindow = if (window > 0) window else DEFAULT_HISTORY_RETURN_WINDOW_MINUTES
            val resolvedMax = if (max > 0) max else DEFAULT_HISTORY_RETURN_MAX_MESSAGES
            return resolvedWindow to resolvedMax
        }
    }

    private fun activeDeviceAddress(): String? =
        meshPrefs.deviceAddress?.takeIf { !it.equals(NO_DEVICE_SELECTED, ignoreCase = true) && it.isNotBlank() }

    fun requestHistoryReplay(
        trigger: String,
        myNodeNum: Int?,
        storeForwardConfig: ModuleConfigProtos.ModuleConfig.StoreForwardConfig?,
        transport: String,
    ) {
        val address = activeDeviceAddress()
        if (address == null || myNodeNum == null) {
            return
        }

        val lastRequest = meshPrefs.getStoreForwardLastRequest(address)
        val (window, max) =
            resolveHistoryRequestParameters(
                storeForwardConfig?.historyReturnWindow ?: 0,
                storeForwardConfig?.historyReturnMax ?: 0,
            )

        val request = buildStoreForwardHistoryRequest(lastRequest, window, max)

        runCatching {
            packetHandler.sendToRadio(
                MeshPacket.newBuilder()
                    .apply {
                        to = myNodeNum
                        decoded =
                            org.meshtastic.proto.MeshProtos.Data.newBuilder()
                                .apply {
                                    portnumValue = Portnums.PortNum.STORE_FORWARD_APP_VALUE
                                    payload = ByteString.copyFrom(request.toByteArray())
                                }
                                .build()
                        priority = MeshPacket.Priority.BACKGROUND
                    }
                    .build(),
            )
        }
            .onFailure { }
    }

    fun updateStoreForwardLastRequest(source: String, lastRequest: Int, transport: String) {
        if (lastRequest <= 0) return
        val address = activeDeviceAddress() ?: return
        val current = meshPrefs.getStoreForwardLastRequest(address)
        if (lastRequest != current) {
            meshPrefs.setStoreForwardLastRequest(address, lastRequest)
        }
    }
}

