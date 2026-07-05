package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteDao
import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteEntity
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNotesRepository(
    private val noteDao: NoteDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : NotesRepository {
    override val notes: Flow<List<FieldNote>> =
        noteDao.observeNotes().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun seedStarterNoteIfEmpty() {
        if (noteDao.countNotes() > 0) return

        noteDao.insert(
            NoteEntity(
                title = "Inspect storage before syncing",
                body = "Offline-first screens should read from local storage first. This note is now stored in Room.",
                syncStatus = SyncStatus.Synced.name,
                pendingOperation = PendingOperation.None.name,
                updatedAtMillis = clock(),
            ),
        )
    }

    override suspend fun createNote(title: String, body: String) {
        noteDao.insert(
            NoteEntity(
                title = title,
                body = body,
                syncStatus = SyncStatus.PendingCreate.name,
                pendingOperation = PendingOperation.Create.name,
                updatedAtMillis = clock(),
            ),
        )
    }

    override suspend fun updateNote(noteId: Long, title: String, body: String) {
        val existing = noteDao.getNote(noteId) ?: return
        noteDao.update(
            existing.copy(
                title = title,
                body = body,
                syncStatus = if (existing.remoteId == null) {
                    SyncStatus.PendingCreate.name
                } else {
                    SyncStatus.PendingUpdate.name
                },
                pendingOperation = if (existing.remoteId == null) {
                    PendingOperation.Create.name
                } else {
                    PendingOperation.Update.name
                },
                updatedAtMillis = clock(),
            ),
        )
    }
}
