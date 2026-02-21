package org.meshtastic.feature.map.triage

import android.os.RemoteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.TriagePinRepository
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.triage.ClaimPinPacket
import org.meshtastic.core.model.triage.ManualTriagePinPacket
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin
import org.meshtastic.core.model.triage.toDataPacket
import org.meshtastic.core.service.ServiceRepository
import javax.inject.Inject

@HiltViewModel
class TriageMapViewModel @Inject constructor(
    private val triagePinRepository: TriagePinRepository,
    private val serviceRepository: ServiceRepository,
    private val nodeRepository: NodeRepository,
) : ViewModel() {

    private val tag = "TriageMapViewModel"

    val ourNodeId: StateFlow<String?> = nodeRepository.myId

    private val allNodes: StateFlow<List<Node>> =
        nodeRepository.getNodes()
            .map { nodes -> nodes.filterNot { it.isIgnored } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val triagePins: StateFlow<List<TriagePin>> =
        triagePinRepository.getTriagePinsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val silentNodes: StateFlow<List<SilentNodeRecord>> =
        allNodes.map { nodeList ->
            val nowSecs = System.currentTimeMillis() / 1000
            nodeList
                .mapNotNull { detectSilentNode(it, nodeList, nowSecs) }
                .filter { !it.isSuppressed }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val triageMapState: StateFlow<TriageMapState> =
        combine(triagePins, silentNodes) { pins, silent ->
            TriageMapState(pins = pins, silentNodes = silent)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TriageMapState(),
        )

    fun addTriagePin(
        lat: Double,
        lon: Double,
        level: TriageLevel,
        victimCount: Int = 1,
        channel: Int = 0,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val myId = ourNodeId.value ?: DataPacket.ID_LOCAL
        val incoming = TriagePin(
            lat          = lat,
            lon          = lon,
            triageLevel  = level,
            victimCount  = victimCount,
            createdBy    = myId,
        )

        val currentPins = triagePins.value
        val merged = mergeTriagePin(incoming, currentPins)

        merged.forEach { updated ->
            val previous = currentPins.firstOrNull { it.pinId == updated.pinId }
            if (previous != updated) {
                triagePinRepository.upsertPin(updated)
            }
        }

        sendDataPacket(
            ManualTriagePinPacket(
                pinId        = incoming.pinId,
                lat          = lat,
                lon          = lon,
                triageLevel  = level.name,
                victimCount  = victimCount,
                createdBy    = myId,
                timestamp    = incoming.timestamp,
            ).toDataPacket(channel),
        )
    }

    fun convertSilentNodeToTriage(record: SilentNodeRecord, channel: Int = 0) =
        viewModelScope.launch(Dispatchers.IO) {
            if (record.node.validPosition == null) return@launch
            val myId = ourNodeId.value ?: DataPacket.ID_LOCAL
            val lat  = record.node.latitude
            val lon  = record.node.longitude

            val pin = TriagePin(
                lat                    = lat,
                lon                    = lon,
                triageLevel            = TriageLevel.RED,
                createdBy              = myId,
                isSilentNodeConversion = true,
                sourceNodeNum          = record.node.num,
            )

            val currentPins = triagePins.value
            val merged = mergeTriagePin(pin, currentPins)
            merged.forEach { updated ->
                val previous = currentPins.firstOrNull { it.pinId == updated.pinId }
                if (previous != updated) triagePinRepository.upsertPin(updated)
            }

            sendDataPacket(
                ManualTriagePinPacket(
                    pinId       = pin.pinId,
                    lat         = lat,
                    lon         = lon,
                    triageLevel = TriageLevel.RED.name,
                    createdBy   = myId,
                    timestamp   = pin.timestamp,
                ).toDataPacket(channel),
            )
        }

    fun deletePin(pinId: String) = viewModelScope.launch(Dispatchers.IO) {
        triagePinRepository.deletePin(pinId)
    }

    fun claimPin(pinId: String, channel: Int = 0) = viewModelScope.launch(Dispatchers.IO) {
        val myId = ourNodeId.value ?: DataPacket.ID_LOCAL
        triagePinRepository.claimPin(pinId, myId)
        sendDataPacket(
            ClaimPinPacket(
                pinId      = pinId,
                rescuerId  = myId,
                timestamp  = System.currentTimeMillis(),
            ).toDataPacket(channel),
        )
    }

    private fun sendDataPacket(p: DataPacket) {
        try {
            serviceRepository.meshService?.send(p)
        } catch (ex: RemoteException) {
            Logger.e(tag) { "Failed to send triage packet: ${ex.message}" }
        }
    }
}

data class TriageMapState(
    val pins: List<TriagePin>          = emptyList(),
    val silentNodes: List<SilentNodeRecord> = emptyList(),
)
