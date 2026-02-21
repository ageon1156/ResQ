
package com.geeksville.mesh.service

import android.content.BroadcastReceiver
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.service.MeshServiceNotifications
import org.meshtastic.core.service.ServiceRepository

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {
    @Inject lateinit var serviceRepository: ServiceRepository

    @Inject lateinit var meshServiceNotifications: MeshServiceNotifications

    companion object {
        const val REPLY_ACTION = "com.geeksville.mesh.REPLY_ACTION"
        const val CONTACT_KEY = "contactKey"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    private fun sendMessage(str: String, contactKey: String = "0${DataPacket.ID_BROADCAST}") {
        
        val channel = contactKey[0].digitToIntOrNull()
        val dest = if (channel != null) contactKey.substring(1) else contactKey
        val p = DataPacket(dest, channel ?: 0, str)
        serviceRepository.meshService?.send(p)
    }

    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        if (remoteInput != null) {
            val contactKey = intent.getStringExtra(CONTACT_KEY) ?: ""
            val message = remoteInput.getCharSequence(KEY_TEXT_REPLY)?.toString() ?: ""
            sendMessage(message, contactKey)
            meshServiceNotifications.cancelMessageNotification(contactKey)
        }
    }
}

