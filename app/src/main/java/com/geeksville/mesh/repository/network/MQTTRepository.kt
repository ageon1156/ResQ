

package com.geeksville.mesh.repository.network

import com.geeksville.mesh.util.ignoreException
import com.google.protobuf.ByteString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttAsyncClient.generateClientId
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.model.util.subscribeList
import org.meshtastic.proto.MeshProtos.MqttClientProxyMessage
import org.meshtastic.proto.mqttClientProxyMessage
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext

@Singleton
class MQTTRepository
@Inject
constructor(
    private val radioConfigRepository: RadioConfigRepository,
    private val nodeRepository: NodeRepository,
) {

    companion object {
        
        private const val DEFAULT_QOS = 1
        private const val DEFAULT_TOPIC_ROOT = "msh"
        private const val DEFAULT_TOPIC_LEVEL = "/2/e/"
        private const val JSON_TOPIC_LEVEL = "/2/json/"
        private const val DEFAULT_SERVER_ADDRESS = "mqtt.meshtastic.org"
    }

    private var mqttClient: MqttAsyncClient? = null

    fun disconnect() {
        mqttClient?.apply {
            ignoreException { disconnect() }
            close(true)
            mqttClient = null
        }
    }

    val proxyMessageFlow: Flow<MqttClientProxyMessage> = callbackFlow {
        val ownerId = "MeshtasticAndroidMqttProxy-${nodeRepository.myId.value ?: generateClientId()}"
        val channelSet = radioConfigRepository.channelSetFlow.first()
        val mqttConfig = radioConfigRepository.moduleConfigFlow.first().mqtt

        val sslContext = SSLContext.getInstance("TLS")
        
        sslContext.init(null, null, null)

        val rootTopic = mqttConfig.root.ifEmpty { DEFAULT_TOPIC_ROOT }

        val connectOptions =
            MqttConnectOptions().apply {
                userName = mqttConfig.username
                password = mqttConfig.password.toCharArray()
                isAutomaticReconnect = true
                if (mqttConfig.tlsEnabled) {
                    socketFactory = sslContext.socketFactory
                }
            }

        val bufferOptions =
            DisconnectedBufferOptions().apply {
                isBufferEnabled = true
                bufferSize = 512
                isPersistBuffer = false
                isDeleteOldestMessages = true
            }

        val callback =
            object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    channelSet.subscribeList
                        .ifEmpty {
                            return
                        }
                        .forEach { globalId ->
                            subscribe("$rootTopic$DEFAULT_TOPIC_LEVEL$globalId/+")
                            if (mqttConfig.jsonEnabled) subscribe("$rootTopic$JSON_TOPIC_LEVEL$globalId/+")
                        }
                    subscribe("$rootTopic${DEFAULT_TOPIC_LEVEL}PKI/+")
                }

                override fun connectionLost(cause: Throwable) {
                    if (cause is IllegalArgumentException) close(cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    trySend(
                        mqttClientProxyMessage {
                            this.topic = topic
                            data = ByteString.copyFrom(message.payload)
                            retained = message.isRetained
                        },
                    )
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                }
            }

        val scheme = if (mqttConfig.tlsEnabled) "ssl" else "tcp"
        val (host, port) =
            mqttConfig.address
                .ifEmpty { DEFAULT_SERVER_ADDRESS }
                .split(":", limit = 2)
                .let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: -1) }

        mqttClient =
            MqttAsyncClient(URI(scheme, null, host, port, "", "", "").toString(), ownerId, MemoryPersistence()).apply {
                setCallback(callback)
                setBufferOpts(bufferOptions)
                connect(connectOptions)
            }

        awaitClose { disconnect() }
    }

    private fun subscribe(topic: String) {
        mqttClient?.subscribe(topic, DEFAULT_QOS)
    }

    fun publish(topic: String, data: ByteArray, retained: Boolean) {
        try {
            mqttClient?.publish(topic, data, DEFAULT_QOS, retained)
        } catch (ex: Exception) {
        }
    }
}

