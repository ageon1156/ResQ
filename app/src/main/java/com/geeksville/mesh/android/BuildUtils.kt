

package com.geeksville.mesh.android

import android.os.Build

object BuildUtils {
    
    val isEmulator
        get() =
            Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.FINGERPRINT.contains("emulator") ||
                setOf(Build.MODEL, Build.PRODUCT).contains("google_sdk") ||
                Build.MODEL.contains("sdk_gphone64") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
}

