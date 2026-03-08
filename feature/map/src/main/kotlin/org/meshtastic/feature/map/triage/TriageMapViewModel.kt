package org.meshtastic.feature.map.triage

import android.os.RemoteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.TriagePinRepository
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.triage.AssignmentPacket
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

    private val _isIcMode = MutableStateFlow(false)
    val isIcMode: StateFlow<Boolean> = _isIcMode.asStateFlow()

    private val _currentAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentAssignments: StateFlow<Map<String, String>> = _currentAssignments.asStateFlow()

    private val _lockedAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val lockedAssignments: StateFlow<Map<String, String>> = _lockedAssignments.asStateFlow()

    private val _myAssignment = MutableStateFlow<AssignmentPacket?>(null)
    val myAssignment: StateFlow<AssignmentPacket?> = _myAssignment.asStateFlow()

    init {
        setupReactiveAssignment()
    }

    private fun setupReactiveAssignment() {
        combine(triagePins, allNodes) { pins, nodes -> pins to nodes }
            .debounce(2_000L)
            .distinctUntilChanged()
            .onEach { (pins, nodes) ->
                if (_isIcMode.value && pins.isNotEmpty()) {
                    runRecompute(pins, nodes)
                }
            }
            .launchIn(viewModelScope)

        triagePinRepository.incomingAssignments
            .onEach { packet ->
                if (_isIcMode.value) return@onEach
                if (packet.rescuerId == ourNodeId.value) _myAssignment.value = packet
            }
            .launchIn(viewModelScope)
    }

    fun toggleIcMode() {
        _isIcMode.value = !_isIcMode.value
        if (_isIcMode.value) {
            requestPositionsFromRescuers()
        } else {
            _currentAssignments.value = emptyMap()
            _lockedAssignments.value  = emptyMap()
        }
    }

    private fun requestPositionsFromRescuers() = viewModelScope.launch(Dispatchers.IO) {
        val nowSec = (System.currentTimeMillis() / 1000).toInt()
        val rescuers = AssignmentEngine.eligibleRescuers(allNodes.value, ourNodeId.value, nowSec)
        rescuers.forEach { rescuer ->
            val nodeNum = allNodes.value.firstOrNull { it.user.id == rescuer.nodeId }?.num ?: return@forEach
            try {
                serviceRepository.meshService?.requestPosition(nodeNum, Position(0.0, 0.0, 0))
            } catch (ex: RemoteException) {
            }
        }
    }

    fun recomputeAssignments(channel: Int = 0) = viewModelScope.launch(Dispatchers.IO) {
        runRecompute(triagePins.value, allNodes.value, channel)
    }

    fun lockAssignment(rescuerId: String, pinId: String) {
        _lockedAssignments.value = _lockedAssignments.value + (rescuerId to pinId)
    }

    fun unlockAssignment(rescuerId: String) {
        _lockedAssignments.value = _lockedAssignments.value - rescuerId
    }

    fun dismissMyAssignment() {
        _myAssignment.value = null
    }

    private suspend fun runRecompute(pins: List<TriagePin>, nodes: List<Node>, channel: Int = 0) {
        val nowSec = (System.currentTimeMillis() / 1000).toInt()
        val rescuers = AssignmentEngine.eligibleRescuers(nodes, ourNodeId.value, nowSec)
        if (rescuers.isEmpty() || pins.isEmpty()) return

        val result = AssignmentEngine.computeAssignments(pins, rescuers, _lockedAssignments.value)
        _currentAssignments.value = result.assignments
        broadcastAssignments(result.assignments, channel)
    }

    private fun broadcastAssignments(assignments: Map<String, String>, channel: Int) {
        val ts = System.currentTimeMillis()
        val locked = _lockedAssignments.value
        assignments.forEach { (rescuerId, pinId) ->
            val packet = AssignmentPacket(
                rescuerId = rescuerId,
                pinId     = pinId,
                timestamp = ts,
                locked    = rescuerId in locked,
            ).toDataPacket(toNodeId = rescuerId, channel = channel)
            sendDataPacket(packet)
        }
    }

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

    fun clearAllPins() = viewModelScope.launch(Dispatchers.IO) {
        triagePinRepository.deleteAllPins()
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
        }
    }
}

data class TriageMapState(
    val pins: List<TriagePin>          = emptyList(),
    val silentNodes: List<SilentNodeRecord> = emptyList(),
)
