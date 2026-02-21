
package com.geeksville.mesh.service

const val PREFIX = "com.geeksville.mesh"

const val ACTION_NODE_CHANGE = "$PREFIX.NODE_CHANGE"
const val ACTION_MESH_CONNECTED = "$PREFIX.MESH_CONNECTED"
const val ACTION_MESSAGE_STATUS = "$PREFIX.MESSAGE_STATUS"

fun actionReceived(portNum: String) = "$PREFIX.RECEIVED.$portNum"

const val EXTRA_CONNECTED = "$PREFIX.Connected"
const val EXTRA_PROGRESS = "$PREFIX.Progress"

const val EXTRA_PERMANENT = "$PREFIX.Permanent"

const val EXTRA_PAYLOAD = "$PREFIX.Payload"
const val EXTRA_NODEINFO = "$PREFIX.NodeInfo"
const val EXTRA_PACKET_ID = "$PREFIX.PacketId"
const val EXTRA_STATUS = "$PREFIX.Status"

