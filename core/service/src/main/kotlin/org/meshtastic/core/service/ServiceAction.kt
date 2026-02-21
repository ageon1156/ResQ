
package org.meshtastic.core.service

import org.meshtastic.core.database.model.Node
import org.meshtastic.proto.AdminProtos

sealed class ServiceAction {
    data class GetDeviceMetadata(val destNum: Int) : ServiceAction()

    data class Favorite(val node: Node) : ServiceAction()

    data class Ignore(val node: Node) : ServiceAction()

    data class Mute(val node: Node) : ServiceAction()

    data class Reaction(val emoji: String, val replyId: Int, val contactKey: String) : ServiceAction()

    data class ImportContact(val contact: AdminProtos.SharedContact) : ServiceAction()

    data class SendContact(val contact: AdminProtos.SharedContact) : ServiceAction()

    data object LeaveMesh : ServiceAction()
}

