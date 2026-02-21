

package org.meshtastic.core.prefs.map

import android.content.SharedPreferences
import org.meshtastic.core.prefs.PrefDelegate
import org.meshtastic.core.prefs.di.MapSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface MapPrefs {
    var mapStyle: Int
    var showOnlyFavorites: Boolean
    var showWaypointsOnMap: Boolean
    var showPrecisionCircleOnMap: Boolean
    var lastHeardFilter: Long
    var lastHeardTrackFilter: Long
}

@Singleton
class MapPrefsImpl @Inject constructor(@MapSharedPreferences prefs: SharedPreferences) : MapPrefs {
    override var mapStyle: Int by PrefDelegate(prefs, "map_style_id", 0)
    override var showOnlyFavorites: Boolean by PrefDelegate(prefs, "show_only_favorites", false)
    override var showWaypointsOnMap: Boolean by PrefDelegate(prefs, "show_waypoints", true)
    override var showPrecisionCircleOnMap: Boolean by PrefDelegate(prefs, "show_precision_circle", true)
    override var lastHeardFilter: Long by PrefDelegate(prefs, "last_heard_filter", 0L)
    override var lastHeardTrackFilter: Long by PrefDelegate(prefs, "last_heard_track_filter", 0L)
}

