package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteDao
import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteEntity
import com.venkatsvision.offlinefirstsystemdesign.data.remote.FakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.data.remote.RemoteNote
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncResult
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNotesRepository(
    private val noteDao: NoteDao,
    private val notesApi: FakeNotesApi,
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

    override suspend fun deleteNote(noteId: Long) {
        val existing = noteDao.getNote(noteId) ?: return
        if (existing.remoteId == null) {
            noteDao.hardDelete(noteId)
            return
        }

        noteDao.update(
            existing.copy(
                syncStatus = SyncStatus.PendingDelete.name,
                pendingOperation = PendingOperation.Delete.name,
                isDeleted = true,
                updatedAtMillis = clock(),
            ),
        )
    }

    override suspend fun syncNow(): SyncResult {
        var pushed = 0
        var failed = 0

        for (note in noteDao.getPendingNotes()) {
            try {
                val remote = when (enumValueOrDefault(note.pendingOperation, PendingOperation.None)) {
                    PendingOperation.Create -> notesApi.createNote(
                        title = note.title,
                        body = note.body,
                        updatedAtMillis = note.updatedAtMillis,
                    )

                    PendingOperation.Update -> notesApi.updateNote(
                        remoteId = note.remoteId ?: continue,
                        title = note.title,
                        body = note.body,
                        updatedAtMillis = note.updatedAtMillis,
                    )

                    PendingOperation.Delete -> {
                        notesApi.deleteNote(note.remoteId ?: continue)
                        noteDao.hardDelete(note.localId)
                        pushed += 1
                        null
                    }

                    PendingOperation.None -> null
                }

                if (remote != null) {
                    noteDao.update(note.syncedWith(remote))
                    pushed += 1
                }
            } catch (_: IllegalStateException) {
                noteDao.update(note.copy(syncStatus = SyncStatus.Failed.name))
                failed += 1
            }
        }

        val pulled = pullRemoteNotes()
        return SyncResult(pushed = pushed, pulled = pulled, failed = failed)
    }

    private suspend fun pullRemoteNotes(): Int {
        var pulled = 0
        for (remote in notesApi.getNotes()) {
            val local = noteDao.getNoteByRemoteId(remote.remoteId)
            if (local == null) {
                noteDao.insert(remote.toEntity())
                pulled += 1
            } else if (local.pendingOperation == PendingOperation.None.name &&
                remote.updatedAtMillis > local.updatedAtMillis
            ) {
                noteDao.update(local.updatedFrom(remote))
                pulled += 1
            }
        }
        return pulled
    }

    private fun NoteEntity.syncedWith(remote: RemoteNote): NoteEntity =
        copy(
            remoteId = remote.remoteId,
            title = remote.title,
            body = remote.body,
            syncStatus = SyncStatus.Synced.name,
            pendingOperation = PendingOperation.None.name,
            updatedAtMillis = remote.updatedAtMillis,
        )

    private fun NoteEntity.updatedFrom(remote: RemoteNote): NoteEntity =
        copy(
            title = remote.title,
            body = remote.body,
            syncStatus = SyncStatus.Synced.name,
            pendingOperation = PendingOperation.None.name,
            updatedAtMillis = remote.updatedAtMillis,
        )

    private fun RemoteNote.toEntity(): NoteEntity =
        NoteEntity(
            remoteId = remoteId,
            title = title,
            body = body,
            syncStatus = SyncStatus.Synced.name,
            pendingOperation = PendingOperation.None.name,
            updatedAtMillis = updatedAtMillis,
        )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String,
        defaultValue: T,
    ): T =
        enumValues<T>().firstOrNull { it.name == value } ?: defaultValue
}
