

package com.geeksville.mesh.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
import co.touchlab.kermit.Logger
import com.geeksville.mesh.BuildConfig

fun MeshService.Companion.startService(context: Context) {

    Logger.i { "Trying to start service debug=${BuildConfig.DEBUG}" }

    val intent = createIntent(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            context.startForegroundService(intent)
        } catch (ex: ForegroundServiceStartNotAllowedException) {
            Logger.e { "Unable to start service: ${ex.message}" }
        }
    } else {
        context.startForegroundService(intent)
    }
}

