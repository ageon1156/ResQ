
package com.meshtastic.android.meshserviceexample

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.meshtastic.core.service.IMeshService
import org.meshtastic.core.ui.theme.AppTheme

private const val TAG: String = "MeshServiceExample"

class MainActivity : ComponentActivity() {

    private var meshService: IMeshService? = null
    private var isMeshServiceBound = false

    private val viewModel: MeshServiceViewModel by viewModels()

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                meshService = IMeshService.Stub.asInterface(service)
                Log.i(TAG, "Connected to MeshService")
                isMeshServiceBound = true
                viewModel.onServiceConnected(meshService)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                meshService = null
                isMeshServiceBound = false
                viewModel.onServiceDisconnected()
            }
        }

    private val meshtasticReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { viewModel.handleIncomingIntent(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bindMeshService()

        val intentFilter =
            IntentFilter().apply {
                addAction("com.geeksville.mesh.NODE_CHANGE")
                addAction("com.geeksville.mesh.CONNECTION_CHANGED")
                addAction("com.geeksville.mesh.MESH_CONNECTED")
                addAction("com.geeksville.mesh.MESH_DISCONNECTED")
                addAction("com.geeksville.mesh.MESSAGE_STATUS")
                addAction("com.geeksville.mesh.RECEIVED.TEXT_MESSAGE_APP")
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(meshtasticReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(meshtasticReceiver, intentFilter)
        }

        setContent { AppTheme { MainScreen(viewModel) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(meshtasticReceiver)
        unbindMeshService()
    }

    private fun bindMeshService() {
        try {
            Log.i(TAG, "Attempting to bind to Mesh Service...")
            val intent = Intent("com.geeksville.mesh.Service")
            intent.setClassName("com.geeksville.mesh", "com.geeksville.mesh.service.MeshService")
            val success = bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            if (!success) {
                Log.e(TAG, "bindService returned false")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while binding", e)
        }
    }

    private fun unbindMeshService() {
        if (isMeshServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "MeshService not registered or already unbound: ${e.message}")
            }
            isMeshServiceBound = false
            meshService = null
        }
    }
}

