

package org.meshtastic.feature.node.model

import org.meshtastic.core.database.model.Node
import org.meshtastic.core.database.model.isUnmessageableRole

val Node.isEffectivelyUnmessageable: Boolean
    get() =
        if (user.hasIsUnmessagable()) {
            user.isUnmessagable
        } else {
            user.role?.isUnmessageableRole() == true
        }

