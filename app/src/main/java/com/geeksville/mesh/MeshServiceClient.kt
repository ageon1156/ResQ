
package com.geeksville.mesh

import android.content.Context
import androidx.appcompat.app.AppCompatActivity.BIND_ABOVE_CLIENT
import androidx.appcompat.app.AppCompatActivity.BIND_AUTO_CREATE
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import com.geeksville.mesh.android.BindFailedException
import com.geeksville.mesh.android.ServiceClient
import com.geeksville.mesh.concurrent.SequentialJob
import com.geeksville.mesh.service.MeshService
import com.geeksville.mesh.service.startService
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.launch
import org.meshtastic.core.service.IMeshService
import org.meshtastic.core.service.ServiceRepository
import javax.inject.Inject

@ActivityScoped
class MeshServiceClient
@Inject
constructor(
    @ActivityContext private val context: Context,
    private val serviceRepository: ServiceRepository,
    private val serviceSetupJob: SequentialJob,
) : ServiceClient<IMeshService>(IMeshService.Stub::asInterface),
    DefaultLifecycleObserver {

    private val lifecycleOwner: LifecycleOwner = context as LifecycleOwner

    init {
        Logger.d { "Adding self as LifecycleObserver for $lifecycleOwner" }
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onConnected(service: IMeshService) {
        serviceSetupJob.launch(lifecycleOwner.lifecycleScope) {
            serviceRepository.setMeshService(service)
            Logger.d { "connected to mesh service, connectionState=${serviceRepository.connectionState.value}" }
        }
    }

    override fun onDisconnected() {
        serviceSetupJob.cancel()
        serviceRepository.setMeshService(null)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Logger.d { "Lifecycle: ON_START" }

        owner.lifecycleScope.launch {
            try {
                bindMeshService()
            } catch (ex: BindFailedException) {
                Logger.e { "Bind of MeshService failed: ${ex.message}" }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Logger.d { "Lifecycle: ON_DESTROY" }

        owner.lifecycle.removeObserver(this)
        Logger.d { "Removed self as LifecycleObserver to $lifecycleOwner" }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun bindMeshService() {
        Logger.d { "Binding to mesh service!" }
        try {
            MeshService.startService(context)
        } catch (ex: Exception) {
            Logger.e { "Failed to start service from activity - but ignoring because bind will work: ${ex.message}" }
        }

        connect(context, MeshService.createIntent(context), BIND_AUTO_CREATE + BIND_ABOVE_CLIENT)
    }
}

