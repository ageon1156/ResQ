

package com.geeksville.mesh.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
fun MeshService.Companion.startService(context: Context) {
    val intent = createIntent(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            context.startForegroundService(intent)
        } catch (_: ForegroundServiceStartNotAllowedException) {
        }
    } else {
        context.startForegroundService(intent)
    }
}

