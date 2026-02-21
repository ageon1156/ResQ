

package org.meshtastic.core.prefs.map

import android.content.SharedPreferences
import org.meshtastic.core.prefs.NullableStringPrefDelegate
import org.meshtastic.core.prefs.di.MapTileProviderSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface MapTileProviderPrefs {
    var customTileProviders: String?
}

@Singleton
class MapTileProviderPrefsImpl @Inject constructor(@MapTileProviderSharedPreferences prefs: SharedPreferences) :
    MapTileProviderPrefs {
    override var customTileProviders: String? by NullableStringPrefDelegate(prefs, "custom_tile_providers", null)
}

