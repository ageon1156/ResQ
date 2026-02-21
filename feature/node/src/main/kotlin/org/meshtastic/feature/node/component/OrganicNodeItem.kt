
package org.meshtastic.feature.node.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.VolumeOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.database.model.isUnmessageableRole
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.elevation_suffix
import org.meshtastic.core.strings.unknown_username
import org.meshtastic.core.ui.component.MaterialBatteryInfo
import org.meshtastic.core.ui.component.NodeKeyStatusIcon
import org.meshtastic.core.ui.component.SignalInfo
import org.meshtastic.core.ui.theme.RiverShape
import org.meshtastic.core.ui.theme.organicSpring
import org.meshtastic.proto.ConfigProtos.Config.DisplayConfig

@Composable
fun OrganicNodeItem(
    thisNode: Node?,
    thatNode: Node,
    distanceUnits: Int,
    tempInFahrenheit: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    connectionState: ConnectionState,
    isActive: Boolean = false,
) {
    val isFavorite = remember(thatNode) { thatNode.isFavorite }
    val isMuted = remember(thatNode) { thatNode.isMuted }
    val isIgnored = thatNode.isIgnored
    val longName = thatNode.user.longName.ifEmpty { stringResource(Res.string.unknown_username) }
    val isThisNode = remember(thatNode) { thisNode?.num == thatNode.num }
    val system = remember(distanceUnits) { DisplayConfig.DisplayUnits.forNumber(distanceUnits) }
    val distance = remember(thisNode, thatNode) {
        thisNode?.distance(thatNode)?.takeIf { it > 0 }?.toDistanceString(system)
    }

    val scale by animateFloatAsState(
        targetValue = if (isActive) 0.97f else 1f,
        animationSpec = organicSpring(),
        label = "node_scale"
    )

    val containerColor = when {
        isThisNode -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val style = if (thatNode.isUnknownUser) FontStyle.Italic else FontStyle.Normal

    val unmessageable = remember(thatNode) {
        when {
            thatNode.user.hasIsUnmessagable() -> thatNode.user.isUnmessagable
            else -> thatNode.user.role.isUnmessageableRole()
        }
    }

    Card(
        modifier = modifier
            .scale(scale)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RiverShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                
                OrganicNodeAvatar(
                    nodeNum = thatNode.num,
                    shortName = thatNode.user.shortName,
                    isThisNode = isThisNode
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NodeKeyStatusIcon(
                            hasPKC = thatNode.hasPKC,
                            mismatchKey = thatNode.mismatchKey,
                            publicKey = thatNode.user.publicKey,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = longName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = style,
                            textDecoration = TextDecoration.LineThrough.takeIf { isIgnored },
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = thatNode.user.hwModel.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = thatNode.user.role.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LastHeardInfo(
                        lastHeard = thatNode.lastHeard,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = isFavorite,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedVisibility(
                            visible = isMuted,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.TwoTone.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (thatNode.batteryLevel > 0 || thatNode.voltage > 0f) {
                        MaterialBatteryInfo(
                            level = thatNode.batteryLevel,
                            voltage = thatNode.voltage,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (distance != null) {
                        DistanceInfo(
                            distance = distance,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    thatNode.validPosition?.let { position ->
                        ElevationInfo(
                            altitude = position.altitude,
                            system = system,
                            suffix = stringResource(Res.string.elevation_suffix),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                        val satCount = position.satsInView
                        if (satCount > 0) {
                            SatelliteCountInfo(
                                satCount = satCount,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                SignalInfo(
                    node = thatNode,
                    isThisNode = isThisNode,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }

            val telemetryStrings = thatNode.getTelemetryStrings(tempInFahrenheit)
            if (telemetryStrings.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    telemetryStrings.forEach { telemetryString ->
                        Text(
                            text = telemetryString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = thatNode.user.id.ifEmpty { "???" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OrganicNodeAvatar(
    nodeNum: Int,
    shortName: String,
    isThisNode: Boolean
) {
    
    val gradient = remember(nodeNum) {
        val hue = ((nodeNum * 137) % 360).toFloat()  
        val color1 = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.7f))
        val color2 = android.graphics.Color.HSVToColor(floatArrayOf((hue + 60) % 360, 0.6f, 0.8f))
        Brush.linearGradient(
            colors = listOf(Color(color1), Color(color2))
        )
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        
        val initials = remember(shortName) {
            shortName.take(2).uppercase()
        }

        Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        if (isThisNode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

