

package org.meshtastic.feature.settings.navigation

import org.meshtastic.core.navigation.Route

fun getNavRouteFrom(routeName: String): Route? =
    ConfigRoute.entries.find { it.name == routeName }?.route

