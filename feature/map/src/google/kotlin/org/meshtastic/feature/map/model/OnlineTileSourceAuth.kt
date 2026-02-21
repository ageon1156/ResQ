

package org.meshtastic.feature.map.model

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.util.MapTileIndex

@Suppress("LongParameterList")
open class OnlineTileSourceAuth(
    name: String,
    zoomLevel: Int,
    zoomMaxLevel: Int,
    tileSizePixels: Int,
    imageFileNameEnding: String,
    baseUrl: Array<String>,
    pCopyright: String,
    tileSourcePolicy: TileSourcePolicy,
    layerName: String?,
    apiKey: String,
) : OnlineTileSourceBase(
    name,
    zoomLevel,
    zoomMaxLevel,
    tileSizePixels,
    imageFileNameEnding,
    baseUrl,
    pCopyright,
    tileSourcePolicy,
) {
    private var layerName = ""
    private var apiKey = ""

    init {
        if (layerName != null) {
            this.layerName = layerName
        }
        this.apiKey = apiKey
    }

    override fun getTileURLString(pMapTileIndex: Long): String = "$baseUrl$layerName/" +
        (
            MapTileIndex.getZoom(pMapTileIndex).toString() +
                "/" +
                MapTileIndex.getX(pMapTileIndex).toString() +
                "/" +
                MapTileIndex.getY(pMapTileIndex).toString()
            ) +
        mImageFilenameEnding +
        "?appId=$apiKey"
}
