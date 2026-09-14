
package com.geeksville.mesh.service

import co.touchlab.kermit.Logger
import com.geeksville.mesh.concurrent.handledLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import org.meshtastic.core.analytics.platform.PlatformAnalytics
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.database.entity.MetadataEntity
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.proto.MeshProtos
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class MeshConfigFlowManager
@Inject
constructor(
    private val nodeManager: MeshNodeManager,
    private val connectionManager: MeshConnectionManager,
    private val nodeRepository: NodeRepository,
    private val radioConfigRepository: RadioConfigRepository,
    private val connectionStateHolder: ConnectionStateHandler,
    private val serviceBroadcasts: MeshServiceBroadcasts,
    private val analytics: PlatformAnalytics,
    private val commandSender: MeshCommandSender,
    private val packetHandler: PacketHandler,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "MeshConfigFlowManager"
    }

    private val configOnlyNonce = 69420
    private val nodeInfoNonce = 69421
    private val wantConfigDelay = 100L

    fun start(scope: CoroutineScope) {
        this.scope = scope
    }

    private val newNodes = mutableListOf<MeshProtos.NodeInfo>()
    val newNodeCount: Int
        get() = newNodes.size

    private var rawMyNodeInfo: MeshProtos.MyNodeInfo? = null
    private var newMyNodeInfo: MyNodeEntity? = null
    private var myNodeInfo: MyNodeEntity? = null

    fun handleConfigComplete(configCompleteId: Int) {
        when (configCompleteId) {
            configOnlyNonce -> {
                Logger.d(TAG) { "Config-only handshake complete" }
                handleConfigOnlyComplete()
            }
            nodeInfoNonce -> {
                Logger.d(TAG) { "Node-info handshake complete" }
                handleNodeInfoComplete()
            }
            else -> Logger.w(TAG) { "Received configComplete for unknown nonce $configCompleteId" }
        }
    }

    private fun handleConfigOnlyComplete() {
        if (newMyNodeInfo != null) {
            myNodeInfo = newMyNodeInfo
        }

        scope.handledLaunch {
            delay(wantConfigDelay)
            sendHeartbeat()
            delay(wantConfigDelay)
            connectionManager.startNodeInfoOnly()
        }
    }

    private fun sendHeartbeat() {
        try {
            packetHandler.sendToRadio(
                MeshProtos.ToRadio.newBuilder().apply { heartbeat = MeshProtos.Heartbeat.getDefaultInstance() },
            )
        } catch (_: IOException) {
        }
    }

    private fun handleNodeInfoComplete() {
        val entities =
            newNodes.map { info ->
                nodeManager.installNodeInfo(info, withBroadcast = false)
                nodeManager.nodeDBbyNodeNum[info.num]!!
            }
        newNodes.clear()

        scope.handledLaunch {
            myNodeInfo?.let {
                nodeRepository.installConfig(it, entities)
                sendAnalytics(it)
            }
            nodeManager.isNodeDbReady.value = true
            nodeManager.allowNodeDbWrites.value = true
            connectionStateHolder.setState(ConnectionState.Connected)
            serviceBroadcasts.broadcastConnection()
            connectionManager.onHasSettings()
        }
    }

    private fun sendAnalytics(mi: MyNodeEntity) {
        analytics.setDeviceAttributes(mi.firmwareVersion ?: "unknown", mi.model ?: "unknown")
    }

    fun handleMyInfo(myInfo: MeshProtos.MyNodeInfo) {
        rawMyNodeInfo = myInfo
        nodeManager.myNodeNum = myInfo.myNodeNum
        regenMyNodeInfo()

        scope.handledLaunch {
            radioConfigRepository.clearChannelSet()
            radioConfigRepository.clearLocalConfig()
            radioConfigRepository.clearLocalModuleConfig()
        }
    }

    fun handleLocalMetadata(metadata: MeshProtos.DeviceMetadata) {
        regenMyNodeInfo(metadata)
    }

    fun handleNodeInfo(info: MeshProtos.NodeInfo) {
        newNodes.add(info)
    }

    private fun regenMyNodeInfo(metadata: MeshProtos.DeviceMetadata? = MeshProtos.DeviceMetadata.getDefaultInstance()) {
        val myInfo = rawMyNodeInfo
        if (myInfo != null) {
            val mi =
                with(myInfo) {
                    MyNodeEntity(
                        myNodeNum = myNodeNum,
                        model =
                        when (val hwModel = metadata?.hwModel) {
                            null,
                            MeshProtos.HardwareModel.UNSET,
                            -> null
                            else -> hwModel.name.replace('_', '-').replace('p', '.').lowercase()
                        },
                        firmwareVersion = metadata?.firmwareVersion,
                        couldUpdate = false,
                        shouldUpdate = false,
                        currentPacketId = commandSender.getCurrentPacketId() and 0xffffffffL,
                        messageTimeoutMsec = 300000,
                        minAppVersion = minAppVersion,
                        maxChannels = 8,
                        hasWifi = metadata?.hasWifi == true,
                        deviceId = deviceId.toStringUtf8(),
                        pioEnv = if (myInfo.pioEnv.isNullOrEmpty()) null else myInfo.pioEnv,
                    )
                }
            if (metadata != null && metadata != MeshProtos.DeviceMetadata.getDefaultInstance()) {
                scope.handledLaunch { nodeRepository.insertMetadata(MetadataEntity(mi.myNodeNum, metadata)) }
            }
            newMyNodeInfo = mi
        }
    }
}

