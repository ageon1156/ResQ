

package org.meshtastic.core.database.model

import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.node_sort_alpha
import org.meshtastic.core.strings.node_sort_channel
import org.meshtastic.core.strings.node_sort_distance
import org.meshtastic.core.strings.node_sort_hops_away
import org.meshtastic.core.strings.node_sort_last_heard
import org.meshtastic.core.strings.node_sort_via_favorite
import org.meshtastic.core.strings.node_sort_via_mqtt

enum class NodeSortOption(val sqlValue: String, val stringRes: StringResource) {
    LAST_HEARD("last_heard", Res.string.node_sort_last_heard),
    ALPHABETICAL("alpha", Res.string.node_sort_alpha),
    DISTANCE("distance", Res.string.node_sort_distance),
    HOPS_AWAY("hops_away", Res.string.node_sort_hops_away),
    CHANNEL("channel", Res.string.node_sort_channel),
    VIA_MQTT("via_mqtt", Res.string.node_sort_via_mqtt),
    VIA_FAVORITE("via_favorite", Res.string.node_sort_via_favorite),
}

