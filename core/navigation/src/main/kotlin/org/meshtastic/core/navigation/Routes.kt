
package org.meshtastic.core.navigation

import kotlinx.serialization.Serializable

const val DEEP_LINK_BASE_URI = "meshtastic://meshtastic"

interface Route

interface Graph : Route

object ChannelsRoutes {
    @Serializable data object ChannelsGraph : Graph

    @Serializable data object Channels : Route
}

object ConnectionsRoutes {
    @Serializable data object ConnectionsGraph : Graph

    @Serializable data object Connections : Route
}

object ContactsRoutes {
    @Serializable data object ContactsGraph : Graph

    @Serializable data object Contacts : Route

    @Serializable data class Messages(val contactKey: String, val message: String = "") : Route

    @Serializable data class Share(val message: String) : Route

    @Serializable data object QuickChat : Route
}

object MapRoutes {
    @Serializable data class Map(val waypointId: Int? = null) : Route
}

object NodesRoutes {
    @Serializable data object NodesGraph : Graph

    @Serializable data object Nodes : Route

    @Serializable data class NodeDetailGraph(val destNum: Int? = null) : Graph

    @Serializable data class NodeDetail(val destNum: Int? = null) : Route
}

object NodeDetailRoutes {
    @Serializable data class DeviceMetrics(val destNum: Int) : Route

    @Serializable data class NodeMap(val destNum: Int) : Route

    @Serializable data class PositionLog(val destNum: Int) : Route

    @Serializable data class EnvironmentMetrics(val destNum: Int) : Route

    @Serializable data class SignalMetrics(val destNum: Int) : Route

    @Serializable data class PowerMetrics(val destNum: Int) : Route

    @Serializable data class TracerouteLog(val destNum: Int) : Route

    @Serializable data class TracerouteMap(val destNum: Int, val requestId: Int, val logUuid: String? = null) : Route

    @Serializable data class HostMetricsLog(val destNum: Int) : Route

    @Serializable data class PaxMetrics(val destNum: Int) : Route
}

object SettingsRoutes {
    @Serializable data object ChannelConfig : Route
}

object EmergencyRoutes {
    @Serializable data object EmergencyGraph : Graph

    @Serializable data object EmergencyHome : Route

    @Serializable data class EmergencyTopic(val section: String, val topicId: String) : Route
}

object SOSRoutes {
    @Serializable data object SOSGraph : Graph

    @Serializable data object SOSHome : Route
}

