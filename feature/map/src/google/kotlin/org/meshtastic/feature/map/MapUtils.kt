

package org.meshtastic.feature.map

import android.content.Context
import android.util.TypedValue
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlin.math.log2
import kotlin.math.pow

private const val DEGREES_IN_CIRCLE = 360.0
private const val METERS_PER_DEGREE_LATITUDE = 111320.0
private const val ZOOM_ADJUSTMENT_FACTOR = 0.8

fun BoundingBox.requiredZoomLevel(): Double {
    val topLeft = GeoPoint(this.latNorth, this.lonWest)
    val bottomRight = GeoPoint(this.latSouth, this.lonEast)
    val latLonWidth = topLeft.distanceToAsDouble(GeoPoint(topLeft.latitude, bottomRight.longitude))
    val latLonHeight = topLeft.distanceToAsDouble(GeoPoint(bottomRight.latitude, topLeft.longitude))
    val requiredLatZoom = log2(DEGREES_IN_CIRCLE / (latLonHeight / METERS_PER_DEGREE_LATITUDE))
    val requiredLonZoom = log2(DEGREES_IN_CIRCLE / (latLonWidth / METERS_PER_DEGREE_LATITUDE))
    return maxOf(requiredLatZoom, requiredLonZoom) * ZOOM_ADJUSTMENT_FACTOR
}

fun BoundingBox.zoomIn(zoomFactor: Double): BoundingBox {
    val center = GeoPoint((latNorth + latSouth) / 2, (lonWest + lonEast) / 2)
    val latDiff = latNorth - latSouth
    val lonDiff = lonEast - lonWest

    val newLatDiff = latDiff / (2.0.pow(zoomFactor))
    val newLonDiff = lonDiff / (2.0.pow(zoomFactor))

    return BoundingBox(
        center.latitude + newLatDiff / 2,
        center.longitude + newLonDiff / 2,
        center.latitude - newLatDiff / 2,
        center.longitude - newLonDiff / 2,
    )
}

fun Context.spToPx(sp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics).toInt()

fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
