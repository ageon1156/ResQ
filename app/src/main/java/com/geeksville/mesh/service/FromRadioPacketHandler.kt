
package com.geeksville.mesh.service

import co.touchlab.kermit.Logger
import org.meshtastic.core.service.MeshServiceNotifications
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.MeshProtos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FromRadioPacketHandler
@Inject
constructor(
    private val serviceRepository: ServiceRepository,
    private val router: MeshRouter,
    private val mqttManager: MeshMqttManager,
    private val packetHandler: PacketHandler,
    private val serviceNotifications: MeshServiceNotifications,
) {
    @Suppress("CyclomaticComplexMethod")
    fun handleFromRadio(proto: MeshProtos.FromRadio) {
        when (proto.payloadVariantCase) {
            MeshProtos.FromRadio.PayloadVariantCase.MY_INFO -> router.configFlowManager.handleMyInfo(proto.myInfo)
            MeshProtos.FromRadio.PayloadVariantCase.METADATA ->
                router.configFlowManager.handleLocalMetadata(proto.metadata)
            MeshProtos.FromRadio.PayloadVariantCase.NODE_INFO -> {
                router.configFlowManager.handleNodeInfo(proto.nodeInfo)
                serviceRepository.setStatusMessage("Nodes (${router.configFlowManager.newNodeCount})")
            }
            MeshProtos.FromRadio.PayloadVariantCase.CONFIG_COMPLETE_ID ->
                router.configFlowManager.handleConfigComplete(proto.configCompleteId)
            MeshProtos.FromRadio.PayloadVariantCase.MQTTCLIENTPROXYMESSAGE ->
                mqttManager.handleMqttProxyMessage(proto.mqttClientProxyMessage)
            MeshProtos.FromRadio.PayloadVariantCase.QUEUESTATUS -> packetHandler.handleQueueStatus(proto.queueStatus)
            MeshProtos.FromRadio.PayloadVariantCase.CONFIG -> router.configHandler.handleDeviceConfig(proto.config)
            MeshProtos.FromRadio.PayloadVariantCase.MODULECONFIG ->
                router.configHandler.handleModuleConfig(proto.moduleConfig)
            MeshProtos.FromRadio.PayloadVariantCase.CHANNEL -> router.configHandler.handleChannel(proto.channel)
            MeshProtos.FromRadio.PayloadVariantCase.CLIENTNOTIFICATION -> {
                serviceRepository.setClientNotification(proto.clientNotification)
                serviceNotifications.showClientNotification(proto.clientNotification)
                packetHandler.removeResponse(proto.clientNotification.replyId, complete = false)
            }
            
            MeshProtos.FromRadio.PayloadVariantCase.PACKET,
            MeshProtos.FromRadio.PayloadVariantCase.LOG_RECORD,
            MeshProtos.FromRadio.PayloadVariantCase.REBOOTED,
            MeshProtos.FromRadio.PayloadVariantCase.XMODEMPACKET,
            MeshProtos.FromRadio.PayloadVariantCase.DEVICEUICONFIG,
            MeshProtos.FromRadio.PayloadVariantCase.FILEINFO,
            -> {
                
            }

            else -> Logger.d { "Dispatcher ignoring ${proto.payloadVariantCase}" }
        }
    }
}

