
package com.geeksville.mesh.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.data.repository.MeshLogRepository
import org.meshtastic.core.prefs.meshlog.MeshLogPrefs

@HiltWorker
class MeshLogCleanupWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val meshLogRepository: MeshLogRepository,
    private val meshLogPrefs: MeshLogPrefs,
) : CoroutineWorker(appContext, workerParams) {

    constructor(
        appContext: Context,
        workerParams: WorkerParameters,
    ) : this(
        appContext,
        workerParams,
        entryPoint(appContext).meshLogRepository(),
        entryPoint(appContext).meshLogPrefs(),
    )

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        val retentionDays = meshLogPrefs.retentionDays
        if (!meshLogPrefs.loggingEnabled) {
        } else if (retentionDays == MeshLogPrefs.NEVER_CLEAR_RETENTION_DAYS) {
        } else {
            meshLogRepository.deleteLogsOlderThan(retentionDays)
        }
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }

    companion object {
        const val WORK_NAME = "meshlog_cleanup_worker"

        private fun entryPoint(context: Context): WorkerEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, WorkerEntryPoint::class.java)
    }

}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun meshLogRepository(): MeshLogRepository

    fun meshLogPrefs(): MeshLogPrefs
}

