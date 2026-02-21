

package com.geeksville.mesh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) {
            return
        }
        val prefs = context.getSharedPreferences("mesh-prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("device_address")) {
            return
        }

        MeshService.startService(context)
    }
}

