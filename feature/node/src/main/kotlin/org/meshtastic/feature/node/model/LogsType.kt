

package org.meshtastic.feature.node.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChargingStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.navigation.NodeDetailRoutes
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.device_metrics_log
import org.meshtastic.core.strings.env_metrics_log
import org.meshtastic.core.strings.host_metrics_log
import org.meshtastic.core.strings.pax_metrics_log
import org.meshtastic.core.strings.position_log
import org.meshtastic.core.strings.power_metrics_log
import org.meshtastic.core.strings.sig_metrics_log
import org.meshtastic.core.strings.traceroute_log

enum class LogsType(val titleRes: StringResource, val icon: ImageVector, val routeFactory: (Int) -> Route) {
    DEVICE(Res.string.device_metrics_log, Icons.Default.ChargingStation, { NodeDetailRoutes.DeviceMetrics(it) }),
    POSITIONS(Res.string.position_log, Icons.Default.LocationOn, { NodeDetailRoutes.PositionLog(it) }),
    ENVIRONMENT(Res.string.env_metrics_log, Icons.Default.Thermostat, { NodeDetailRoutes.EnvironmentMetrics(it) }),
    SIGNAL(Res.string.sig_metrics_log, Icons.Default.SignalCellularAlt, { NodeDetailRoutes.SignalMetrics(it) }),
    POWER(Res.string.power_metrics_log, Icons.Default.Power, { NodeDetailRoutes.PowerMetrics(it) }),
    TRACEROUTE(Res.string.traceroute_log, Icons.Default.Route, { NodeDetailRoutes.TracerouteLog(it) }),
    HOST(Res.string.host_metrics_log, Icons.Default.Memory, { NodeDetailRoutes.HostMetricsLog(it) }),
    PAX(Res.string.pax_metrics_log, Icons.Default.People, { NodeDetailRoutes.PaxMetrics(it) }),
}

