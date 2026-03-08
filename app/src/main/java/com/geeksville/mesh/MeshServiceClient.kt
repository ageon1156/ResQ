
package com.geeksville.mesh

import android.content.Context
import androidx.appcompat.app.AppCompatActivity.BIND_ABOVE_CLIENT
import androidx.appcompat.app.AppCompatActivity.BIND_AUTO_CREATE
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onConnected(service: IMeshService) {
        serviceSetupJob.launch(lifecycleOwner.lifecycleScope) {
            serviceRepository.setMeshService(service)
        }
    }

    override fun onDisconnected() {
        serviceSetupJob.cancel()
        serviceRepository.setMeshService(null)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        owner.lifecycleScope.launch {
            try {
                bindMeshService()
            } catch (_: BindFailedException) {
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        owner.lifecycle.removeObserver(this)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun bindMeshService() {
        try {
            MeshService.startService(context)
        } catch (_: Exception) {
        }

        connect(context, MeshService.createIntent(context), BIND_AUTO_CREATE + BIND_ABOVE_CLIENT)
    }
}

