

package com.geeksville.mesh.repository.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun ConnectivityManager.networkAvailable(): Flow<Boolean> = observeNetworks()
        .map { activeNetworksList -> activeNetworksList.isNotEmpty() }
        .distinctUntilChanged()

internal fun ConnectivityManager.observeNetworks(
    networkRequest: NetworkRequest = NetworkRequest.Builder().build(),
): Flow<List<Network>> = callbackFlow {
    
    val activeNetworks = mutableSetOf<Network>()

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            activeNetworks.add(network)
            trySend(activeNetworks.toList())
        }

        override fun onLost(network: Network) {
            activeNetworks.remove(network)
            trySend(activeNetworks.toList())
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (activeNetworks.contains(network)) {
                trySend(activeNetworks.toList())
            }
        }
    }

    registerNetworkCallback(networkRequest, callback)

    awaitClose {
        unregisterNetworkCallback(callback)
    }
}

