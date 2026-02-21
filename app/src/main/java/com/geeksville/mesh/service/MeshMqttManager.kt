
package com.geeksville.mesh.service

import co.touchlab.kermit.Logger
import com.geeksville.mesh.repository.network.MQTTRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.MeshProtos.ToRadio
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshMqttManager
@Inject
constructor(
    private val mqttRepository: MQTTRepository,
    private val packetHandler: PacketHandler,
    private val serviceRepository: ServiceRepository,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mqttMessageFlow: Job? = null

    fun start(scope: CoroutineScope, enabled: Boolean, proxyToClientEnabled: Boolean) {
        this.scope = scope
        if (mqttMessageFlow?.isActive == true) return
        if (enabled && proxyToClientEnabled) {
            mqttMessageFlow =
                mqttRepository.proxyMessageFlow
                    .onEach { message ->
                        packetHandler.sendToRadio(ToRadio.newBuilder().apply { mqttClientProxyMessage = message })
                    }
                    .catch { throwable -> serviceRepository.setErrorMessage("MqttClientProxy failed: $throwable") }
                    .launchIn(scope)
        }
    }

    fun stop() {
        if (mqttMessageFlow?.isActive == true) {
            Logger.i { "Stopping MqttClientProxy" }
            mqttMessageFlow?.cancel()
            mqttMessageFlow = null
        }
    }

    fun handleMqttProxyMessage(message: MeshProtos.MqttClientProxyMessage) {
        Logger.d { "[mqttClientProxyMessage] ${message.topic}" }
        with(message) {
            when (payloadVariantCase) {
                MeshProtos.MqttClientProxyMessage.PayloadVariantCase.TEXT -> {
                    mqttRepository.publish(topic, text.encodeToByteArray(), retained)
                }
                MeshProtos.MqttClientProxyMessage.PayloadVariantCase.DATA -> {
                    mqttRepository.publish(topic, data.toByteArray(), retained)
                }
                else -> {}
            }
        }
    }
}

