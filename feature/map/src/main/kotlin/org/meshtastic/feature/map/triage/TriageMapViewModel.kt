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

    // ── Reactive state ────────────────────────────────────────────────────────

    val ourNodeId: StateFlow<String?> = nodeRepository.myId

    private val allNodes: StateFlow<List<Node>> =
        nodeRepository.getNodes()
            .map { nodes -> nodes.filterNot { it.isIgnored } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val triagePins: StateFlow<List<TriagePin>> =
        triagePinRepository.getTriagePinsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Detected silent nodes — re-evaluated whenever the node list changes.
     * Suppressed nodes (battery 1–4 %) are excluded.
     */
    val silentNodes: StateFlow<List<SilentNodeRecord>> =
        allNodes.map { nodeList ->
            val nowSecs = System.currentTimeMillis() / 1000
            nodeList
                .mapNotNull { detectSilentNode(it, nodeList, nowSecs) }
                .filter { !it.isSuppressed }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Combined triage-map state for the UI to observe in a single collect. */
    val triageMapState: StateFlow<TriageMapState> =
        combine(triagePins, silentNodes) { pins, silent ->
            TriageMapState(pins = pins, silentNodes = silent)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TriageMapState(),
        )

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Long-press handler for the triage map.
     * Runs merge logic, persists all affected pins, and broadcasts the new pin.
     */
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

        // Apply merge rules against current snapshot
        val currentPins = triagePins.value
        val merged = mergeTriagePin(incoming, currentPins)

        // Persist every pin that changed (new or updated)
        merged.forEach { updated ->
            val previous = currentPins.firstOrNull { it.pinId == updated.pinId }
            if (previous != updated) {
                triagePinRepository.upsertPin(updated)
            }
        }

        // Broadcast the raw incoming pin (peers apply their own merge logic)
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

    /**
     * Converts a silent node card to a RED triage pin at the node's last GPS fix.
     * Called from the "Convert to Triage Pin" button on the silent node card.
     */
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

    /** Deletes a triage pin locally. No mesh broadcast — local removal only. */
    fun deletePin(pinId: String) = viewModelScope.launch(Dispatchers.IO) {
        triagePinRepository.deletePin(pinId)
    }

    /**
     * Claims a triage pin for this rescuer and broadcasts the claim.
     * A pin can only be claimed once; the DAO uses a guard to prevent overwrite.
     */
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

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun sendDataPacket(p: DataPacket) {
        try {
            serviceRepository.meshService?.send(p)
        } catch (ex: RemoteException) {
            Logger.e(tag) { "Failed to send triage packet: ${ex.message}" }
        }
    }
}

/** Snapshot of all data the triage map needs to render. */
data class TriageMapState(
    val pins: List<TriagePin>          = emptyList(),
    val silentNodes: List<SilentNodeRecord> = emptyList(),
)
