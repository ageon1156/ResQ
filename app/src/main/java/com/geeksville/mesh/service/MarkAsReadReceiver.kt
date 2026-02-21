
package com.geeksville.mesh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.service.MeshServiceNotifications
import javax.inject.Inject

@AndroidEntryPoint
class MarkAsReadReceiver : BroadcastReceiver() {
    @Inject lateinit var packetRepository: PacketRepository

    @Inject lateinit var meshServiceNotifications: MeshServiceNotifications

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val MARK_AS_READ_ACTION = "com.geeksville.mesh.MARK_AS_READ_ACTION"
        const val CONTACT_KEY = "contactKey"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MARK_AS_READ_ACTION) {
            val contactKey = intent.getStringExtra(CONTACT_KEY) ?: return
            val pendingResult = goAsync()
            scope.launch {
                try {
                    packetRepository.clearUnreadCount(contactKey, System.currentTimeMillis())
                    meshServiceNotifications.cancelMessageNotification(contactKey)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

