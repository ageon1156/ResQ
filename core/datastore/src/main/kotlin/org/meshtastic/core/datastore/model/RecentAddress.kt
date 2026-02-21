

package org.meshtastic.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable data class RecentAddress(val address: String, val name: String)

