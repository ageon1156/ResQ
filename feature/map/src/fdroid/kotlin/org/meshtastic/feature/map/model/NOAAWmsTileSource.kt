

package org.meshtastic.feature.map.model

import android.content.res.Resources
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.util.MapTileIndex
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sinh

open class NOAAWmsTileSource(
    aName: String,
    aBaseUrl: Array<String>,
    layername: String,
    version: String,
    time: String?,
    srs: String,
    style: String?,
    format: String,
) : OnlineTileSourceBase(
    aName,
    0,
    5,
    256,
    "png",
    aBaseUrl,
    "",
    TileSourcePolicy(
        2,
        TileSourcePolicy.FLAG_NO_BULK or
            TileSourcePolicy.FLAG_NO_PREVENTIVE or
            TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL or
            TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED,
    ),
) {

    private val minX = 0
    private val maxX = 1
    private val minY = 2
    private val maxY = 3

    private val tileOrigin = doubleArrayOf(-20037508.34789244, 20037508.34789244)

    private val origX = 0
    private val origY = 1 

    private val mapSize = 20037508.34789244 * 2
    private var layer = ""
    private var version = "1.1.0"
    private var srs = "EPSG%3A3857" 
    private var format = ""
    private var time = ""
    private var style: String? = null
    private var forceHttps = false
    private var forceHttp = false

    init {
        Log.i(IMapView.LOGTAG, "WMS support is BETA. Please report any issues")
        layer = layername
        this.version = version
        this.srs = srs
        this.style = style
        this.format = format
        if (time != null) this.time = time
    }

    private fun tile2lon(x: Int, z: Int): Double = x / 2.0.pow(z.toDouble()) * 360.0 - 180

    private fun tile2lat(y: Int, z: Int): Double {
        val n = Math.PI - 2.0 * Math.PI * y / 2.0.pow(z.toDouble())
        return Math.toDegrees(atan(sinh(n)))
    }

    private fun getBoundingBox(x: Int, y: Int, zoom: Int): DoubleArray {
        val tileSize = mapSize / 2.0.pow(zoom.toDouble())
        val minx = tileOrigin[origX] + x * tileSize
        val maxx = tileOrigin[origX] + (x + 1) * tileSize
        val miny = tileOrigin[origY] - (y + 1) * tileSize
        val maxy = tileOrigin[origY] - y * tileSize
        val bbox = DoubleArray(4)
        bbox[minX] = minx
        bbox[minY] = miny
        bbox[maxX] = maxx
        bbox[maxY] = maxy
        return bbox
    }

    fun isForceHttps(): Boolean = forceHttps

    fun setForceHttps(forceHttps: Boolean) {
        this.forceHttps = forceHttps
    }

    fun isForceHttp(): Boolean = forceHttp

    fun setForceHttp(forceHttp: Boolean) {
        this.forceHttp = forceHttp
    }

    override fun getTileURLString(pMapTileIndex: Long): String? {
        var baseUrl = baseUrl
        if (forceHttps) baseUrl = baseUrl.replace("http://", "https://")
        if (forceHttp) baseUrl = baseUrl.replace("https://", "http://")
        val sb = StringBuilder(baseUrl)
        if (!baseUrl.endsWith("&")) sb.append("service=WMS")
        sb.append("&request=GetMap")
        sb.append("&version=").append(version)
        sb.append("&layers=").append(layer)
        if (style != null) sb.append("&styles=").append(style)
        sb.append("&format=").append(format)
        sb.append("&transparent=true")
        sb.append("&height=").append(Resources.getSystem().displayMetrics.heightPixels)
        sb.append("&width=").append(Resources.getSystem().displayMetrics.widthPixels)
        sb.append("&srs=").append(srs)
        sb.append("&size=").append(getSize())
        sb.append("&bbox=")
        val bbox =
            getBoundingBox(
                MapTileIndex.getX(pMapTileIndex),
                MapTileIndex.getY(pMapTileIndex),
                MapTileIndex.getZoom(pMapTileIndex),
            )
        sb.append(bbox[minX]).append(",")
        sb.append(bbox[minY]).append(",")
        sb.append(bbox[maxX]).append(",")
        sb.append(bbox[maxY])
        Log.i(IMapView.LOGTAG, sb.toString())
        return sb.toString()
    }

    private fun getSize(): String {
        val height = Resources.getSystem().displayMetrics.heightPixels
        val width = Resources.getSystem().displayMetrics.widthPixels
        return "$width,$height"
    }
}

