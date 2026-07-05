package com.venkatsvision.offlinefirstsystemdesign.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.venkatsvision.offlinefirstsystemdesign.data.AppContainer

class NotesSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            val syncResult = AppContainer.notesRepository(applicationContext).syncNow()
            if (syncResult.failed == 0) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
