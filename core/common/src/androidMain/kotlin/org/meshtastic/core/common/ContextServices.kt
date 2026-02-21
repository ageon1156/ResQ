

package org.meshtastic.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

fun Context.hasGps(): Boolean {
    val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return lm?.allProviders?.contains(LocationManager.GPS_PROVIDER) == true
}

fun Context.gpsDisabled(): Boolean {
    val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return if (lm.allProviders.contains(LocationManager.GPS_PROVIDER)) {
        !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } else {
        false
    }
}

private fun Context.getBluetoothPermissions(): Array<String> {
    val requiredPermissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    return requiredPermissions
        .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        .toTypedArray()
}

fun Context.hasBluetoothPermission(): Boolean = getBluetoothPermissions().isEmpty()

fun Context.hasLocationPermission(): Boolean {
    val perms = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    return perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
}

