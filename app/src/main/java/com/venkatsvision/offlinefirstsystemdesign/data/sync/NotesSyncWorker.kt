package com.venkatsvision.offlinefirstsystemdesign.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class NotesSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val notesRepository: NotesRepository,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            val syncResult = notesRepository.syncNow()
            if (syncResult.failed == 0) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
