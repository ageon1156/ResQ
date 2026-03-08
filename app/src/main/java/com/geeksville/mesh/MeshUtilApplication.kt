
package com.geeksville.mesh

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.geeksville.mesh.worker.MeshLogCleanupWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.prefs.mesh.MeshPrefs
import org.meshtastic.core.prefs.meshlog.MeshLogPrefs
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MeshUtilApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initializeMaps(this)

        scheduleMeshLogCleanup()

        val entryPoint = EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        CoroutineScope(Dispatchers.Default).launch {
            entryPoint.databaseManager().init(entryPoint.meshPrefs().deviceAddress)
        }
    }

    private fun scheduleMeshLogCleanup() {
        val cleanupRequest =
            PeriodicWorkRequestBuilder<MeshLogCleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                MeshLogCleanupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                cleanupRequest,
            )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun databaseManager(): DatabaseManager

    fun meshPrefs(): MeshPrefs

    fun meshLogPrefs(): MeshLogPrefs
}

fun logAssert(executeReliableWrite: Boolean) {
    if (!executeReliableWrite) {
        val ex = AssertionError("Assertion failed")
        throw ex
    }
}

