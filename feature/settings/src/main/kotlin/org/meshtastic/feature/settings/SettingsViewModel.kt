
package org.meshtastic.feature.settings

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.BuildConfigProvider
import org.meshtastic.core.data.repository.DeviceHardwareRepository
import org.meshtastic.core.data.repository.MeshLogRepository
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.database.DatabaseConstants
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.datastore.UiPreferencesDataSource
import org.meshtastic.core.model.Capabilities
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.util.positionToMeter
import org.meshtastic.core.prefs.meshlog.MeshLogPrefs
import org.meshtastic.core.prefs.radio.RadioPrefs
import org.meshtastic.core.prefs.radio.isBle
import org.meshtastic.core.prefs.radio.isSerial
import org.meshtastic.core.prefs.radio.isTcp
import org.meshtastic.core.prefs.ui.UiPrefs
import org.meshtastic.core.service.IMeshService
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.Portnums
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileWriter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@Suppress("LongParameterList")
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val app: Application,
    radioConfigRepository: RadioConfigRepository,
    private val serviceRepository: ServiceRepository,
    private val nodeRepository: NodeRepository,
    private val meshLogRepository: MeshLogRepository,
    private val uiPrefs: UiPrefs,
    private val uiPreferencesDataSource: UiPreferencesDataSource,
    private val buildConfigProvider: BuildConfigProvider,
    private val databaseManager: DatabaseManager,
    private val deviceHardwareRepository: DeviceHardwareRepository,
    private val radioPrefs: RadioPrefs,
    private val meshLogPrefs: MeshLogPrefs,
) : ViewModel() {
    val myNodeInfo: StateFlow<MyNodeEntity?> = nodeRepository.myNodeInfo

    val myNodeNum
        get() = myNodeInfo.value?.myNodeNum

    val ourNodeInfo: StateFlow<Node?> = nodeRepository.ourNodeInfo

    val isConnected =
        serviceRepository.connectionState.map { it.isConnected() }.stateInWhileSubscribed(initialValue = false)

    val localConfig: StateFlow<LocalConfig> =
        radioConfigRepository.localConfigFlow.stateInWhileSubscribed(initialValue = LocalConfig.getDefaultInstance())

    val meshService: IMeshService?
        get() = serviceRepository.meshService

    val provideLocation: StateFlow<Boolean> =
        myNodeInfo
            .flatMapLatest { myNodeEntity ->
                
                if (myNodeEntity == null) {
                    flowOf(false)
                } else {
                    uiPrefs.shouldProvideNodeLocation(myNodeEntity.myNodeNum)
                }
            }
            .stateInWhileSubscribed(initialValue = false)

    private val _excludedModulesUnlocked = MutableStateFlow(false)
    val excludedModulesUnlocked: StateFlow<Boolean> = _excludedModulesUnlocked.asStateFlow()

    val appVersionName
        get() = buildConfigProvider.versionName

    val isOtaCapable: StateFlow<Boolean> =
        combine(ourNodeInfo, serviceRepository.connectionState) { node, connectionState -> Pair(node, connectionState) }
            .flatMapLatest { (node, connectionState) ->
                if (node == null || !connectionState.isConnected()) {
                    flowOf(false)
                } else if (radioPrefs.isBle() || radioPrefs.isSerial() || radioPrefs.isTcp()) {
                    val hwModel = node.user.hwModel.number
                    val hw = deviceHardwareRepository.getDeviceHardwareByModel(hwModel).getOrNull()
                    val capabilities = Capabilities(node.metadata?.firmwareVersion)
                    val isSerial = radioPrefs.isSerial()

                    val isEsp32OtaSupported = hw?.isEsp32Arc == true && capabilities.supportsEsp32Ota && !isSerial

                    val isDfuSupported = hw?.requiresDfu == true && hw.isEsp32Arc != true

                    flow { emit(isDfuSupported || isEsp32OtaSupported) }
                } else {
                    flowOf(false)
                }
            }
            .stateInWhileSubscribed(initialValue = false)

    val dbCacheLimit: StateFlow<Int> = databaseManager.cacheLimit

    fun setDbCacheLimit(limit: Int) {
        val clamped = limit.coerceIn(DatabaseConstants.MIN_CACHE_LIMIT, DatabaseConstants.MAX_CACHE_LIMIT)
        databaseManager.setCacheLimit(clamped)
    }

    private val _meshLogRetentionDays = MutableStateFlow(meshLogPrefs.retentionDays)
    val meshLogRetentionDays: StateFlow<Int> = _meshLogRetentionDays.asStateFlow()

    private val _meshLogLoggingEnabled = MutableStateFlow(meshLogPrefs.loggingEnabled)
    val meshLogLoggingEnabled: StateFlow<Boolean> = _meshLogLoggingEnabled.asStateFlow()

    fun setMeshLogRetentionDays(days: Int) {
        val clamped = days.coerceIn(MeshLogPrefs.MIN_RETENTION_DAYS, MeshLogPrefs.MAX_RETENTION_DAYS)
        meshLogPrefs.retentionDays = clamped
        _meshLogRetentionDays.value = clamped
        viewModelScope.launch { meshLogRepository.deleteLogsOlderThan(clamped) }
    }

    fun setMeshLogLoggingEnabled(enabled: Boolean) {
        meshLogPrefs.loggingEnabled = enabled
        _meshLogLoggingEnabled.value = enabled
        if (!enabled) {
            viewModelScope.launch { meshLogRepository.deleteAll() }
        } else {
            viewModelScope.launch { meshLogRepository.deleteLogsOlderThan(meshLogPrefs.retentionDays) }
        }
    }

    fun setProvideLocation(value: Boolean) {
        myNodeNum?.let { uiPrefs.setShouldProvideNodeLocation(it, value) }
    }

    fun setTheme(theme: Int) {
        uiPreferencesDataSource.setTheme(theme)
    }

    fun showAppIntro() {
        uiPreferencesDataSource.setAppIntroCompleted(false)
    }

    fun unlockExcludedModules() {
        _excludedModulesUnlocked.update { true }
    }

    @Suppress("detekt:CyclomaticComplexMethod", "detekt:LongMethod")
    fun saveDataCsv(uri: Uri, filterPortnum: Int? = null) {
        viewModelScope.launch(Dispatchers.Main) {

            val myNodeNum = myNodeNum ?: return@launch

            val nodes = nodeRepository.nodeDBbyNum.value

            val positionToPos: (MeshProtos.Position?) -> Position? = { meshPosition ->
                meshPosition?.let { Position(it) }?.takeIf { it.isValid() }
            }

            writeToUri(uri) { writer ->
                val nodePositions = mutableMapOf<Int, MeshProtos.Position?>()

                @Suppress("MaxLineLength")
                writer.appendLine(
                    "\"date\",\"time\",\"from\",\"sender name\",\"sender lat\",\"sender long\",\"rx lat\",\"rx long\",\"rx elevation\",\"rx snr\",\"distance(m)\",\"hop limit\",\"payload\"",
                )

                val dateFormat = SimpleDateFormat("\"yyyy-MM-dd\",\"HH:mm:ss\"", Locale.getDefault())
                meshLogRepository.getAllLogsInReceiveOrder(Int.MAX_VALUE).first().forEach { packet ->
                    
                    packet.nodeInfo?.let { nodeInfo ->
                        positionToPos.invoke(nodeInfo.position)?.let { nodePositions[nodeInfo.num] = nodeInfo.position }
                    }

                    packet.meshPacket?.let { proto ->
                        
                        packet.position?.let { position ->
                            positionToPos.invoke(position)?.let {
                                nodePositions[
                                    proto.from.takeIf { it != 0 } ?: myNodeNum,
                                ] = position
                            }
                        }

                        if (
                            (filterPortnum == null || proto.decoded.portnumValue == filterPortnum) &&
                            proto.rxSnr != 0.0f
                        ) {
                            val rxDateTime = dateFormat.format(packet.received_date)
                            val rxFrom = proto.from.toUInt()
                            val senderName = nodes[proto.from]?.user?.longName ?: ""

                            val senderPosition = nodePositions[proto.from]
                            val senderPos = positionToPos.invoke(senderPosition)
                            val senderLat = senderPos?.latitude ?: ""
                            val senderLong = senderPos?.longitude ?: ""

                            val rxPosition = nodePositions[myNodeNum]
                            val rxPos = positionToPos.invoke(rxPosition)
                            val rxLat = rxPos?.latitude ?: ""
                            val rxLong = rxPos?.longitude ?: ""
                            val rxAlt = rxPos?.altitude ?: ""
                            val rxSnr = proto.rxSnr

                            val dist =
                                if (senderPos == null || rxPos == null) {
                                    ""
                                } else {
                                    positionToMeter(
                                        Position(rxPosition!!), 
                                        
                                        Position(senderPosition!!), 
                                        
                                    )
                                        .roundToInt()
                                        .toString()
                                }

                            val hopLimit = proto.hopLimit

                            val payload =
                                when {
                                    proto.decoded.portnumValue !in
                                        setOf(
                                            Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
                                            Portnums.PortNum.RANGE_TEST_APP_VALUE,
                                        ) -> "<${proto.decoded.portnum}>"

                                    proto.hasDecoded() -> proto.decoded.payload.toStringUtf8().replace("\"", "\"\"")

                                    proto.hasEncrypted() -> "${proto.encrypted.size()} encrypted bytes"
                                    else -> ""
                                }

                            @Suppress("MaxLineLength")
                            writer.appendLine(
                                "$rxDateTime,\"$rxFrom\",\"$senderName\",\"$senderLat\",\"$senderLong\",\"$rxLat\",\"$rxLong\",\"$rxAlt\",\"$rxSnr\",\"$dist\",\"$hopLimit\",\"$payload\"",
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend inline fun writeToUri(uri: Uri, crossinline block: suspend (BufferedWriter) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                app.contentResolver.openFileDescriptor(uri, "wt")?.use { parcelFileDescriptor ->
                    FileWriter(parcelFileDescriptor.fileDescriptor).use { fileWriter ->
                        BufferedWriter(fileWriter).use { writer -> block.invoke(writer) }
                    }
                }
            } catch (ex: FileNotFoundException) {
                Logger.e { "Can't write file error: ${ex.message}" }
            }
        }
    }
}

