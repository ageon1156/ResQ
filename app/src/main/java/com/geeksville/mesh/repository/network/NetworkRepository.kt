
package com.geeksville.mesh.repository.network

import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import org.meshtastic.core.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository
@Inject
constructor(
    private val nsdManagerLazy: dagger.Lazy<NsdManager>,
    private val connectivityManager: dagger.Lazy<ConnectivityManager>,
    private val dispatchers: CoroutineDispatchers,
) {

    val networkAvailable: Flow<Boolean>
        get() = connectivityManager.get().networkAvailable().flowOn(dispatchers.io).conflate()

    val resolvedList: Flow<List<NsdServiceInfo>>
        get() = nsdManagerLazy.get().serviceList(SERVICE_TYPE).flowOn(dispatchers.io).conflate()

    companion object {
        internal const val SERVICE_PORT = 4403
        private const val SERVICE_TYPE = "_meshtastic._tcp"

        fun NsdServiceInfo.toAddressString() = buildString {
            @Suppress("DEPRECATION")
            append(host.hostAddress)
            if (serviceType.trim('.') == SERVICE_TYPE && port != SERVICE_PORT) {
                append(":$port")
            }
        }
    }
}

