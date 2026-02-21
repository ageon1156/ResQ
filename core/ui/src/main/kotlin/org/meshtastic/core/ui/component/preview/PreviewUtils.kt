

@file:Suppress("MatchingDeclarationName")

package org.meshtastic.core.ui.component.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.meshtastic.core.database.model.Node
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.PaxcountProtos
import org.meshtastic.proto.TelemetryProtos

class BooleanProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}

private val user = MeshProtos.User.newBuilder().setShortName("\uD83E\uDEE0").setLongName("John Doe").build()
val previewNode =
    Node(
        num = 13444,
        user = user,
        isIgnored = false,
        paxcounter = PaxcountProtos.Paxcount.newBuilder().setBle(10).setWifi(5).build(),
        environmentMetrics =
        TelemetryProtos.EnvironmentMetrics.newBuilder().setTemperature(25f).setRelativeHumidity(60f).build(),
    )

