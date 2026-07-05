package com.venkatsvision.offlinefirstsystemdesign.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NotesSyncScheduler(
    context: Context,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueueOneTimeSync() {
        val request = OneTimeWorkRequestBuilder<NotesSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_SYNC_WORK_NAME = "notes-one-time-sync"
    }
}
