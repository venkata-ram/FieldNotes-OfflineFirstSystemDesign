package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteDao
import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteEntity
import com.venkatsvision.offlinefirstsystemdesign.data.remote.FakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.data.remote.RemoteNote
import com.venkatsvision.offlinefirstsystemdesign.domain.ConflictResolution
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.RemoteFieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncResult
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomNotesRepository(
    private val noteDao: NoteDao,
    private val notesApi: FakeNotesApi,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : NotesRepository {
    override val notes: Flow<List<FieldNote>> =
        noteDao.observeNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    override val remoteNotes: Flow<List<RemoteFieldNote>> =
        notesApi.notes.map { remoteNotes ->
            remoteNotes.map { it.toDomain() }
        }
    private val _syncLog = MutableStateFlow(listOf("Debug sync log ready"))
    override val syncLog: Flow<List<String>> = _syncLog.asStateFlow()
    private val syncMutex = Mutex()

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
        log("Seeded starter note in Room")
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
        log("Saved local create: $title")
    }

    override suspend fun updateNote(noteId: Long, title: String, body: String) {
        val existing = noteDao.getNote(noteId) ?: return
        noteDao.update(
            existing.copy(
                title = title,
                body = body,
                syncStatus = if (existing.remoteId == null) {
                    SyncStatus.PendingCreate.name
                } else if (existing.syncStatus == SyncStatus.Conflict.name) {
                    SyncStatus.Conflict.name
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
        log("Saved local update: $title")
    }

    override suspend fun updateRemoteNote(remoteId: String, title: String, body: String) {
        notesApi.updateNote(
            remoteId = remoteId,
            title = title,
            body = body,
            updatedAtMillis = clock() + 3_600_000,
        )
        log("Edited remote note $remoteId")
    }

    override suspend fun resolveConflict(noteId: Long, resolution: ConflictResolution) {
        val existing = noteDao.getNote(noteId) ?: return
        if (existing.syncStatus != SyncStatus.Conflict.name) return

        val resolved = when (resolution) {
            ConflictResolution.KeepLocal -> existing.copy(
                syncStatus = SyncStatus.PendingUpdate.name,
                pendingOperation = PendingOperation.Update.name,
                conflictTitle = null,
                conflictBody = null,
                conflictUpdatedAtMillis = null,
                updatedAtMillis = clock(),
            )

            ConflictResolution.UseRemote -> existing.copy(
                title = existing.conflictTitle ?: existing.title,
                body = existing.conflictBody ?: existing.body,
                syncStatus = SyncStatus.Synced.name,
                pendingOperation = PendingOperation.None.name,
                conflictTitle = null,
                conflictBody = null,
                conflictUpdatedAtMillis = null,
                updatedAtMillis = existing.conflictUpdatedAtMillis ?: clock(),
            )

            ConflictResolution.MergeBoth -> existing.copy(
                title = mergeText(
                    local = existing.title,
                    remote = existing.conflictTitle,
                    label = "title",
                ),
                body = mergeText(
                    local = existing.body,
                    remote = existing.conflictBody,
                    label = "body",
                ),
                syncStatus = SyncStatus.PendingUpdate.name,
                pendingOperation = PendingOperation.Update.name,
                conflictTitle = null,
                conflictBody = null,
                conflictUpdatedAtMillis = null,
                updatedAtMillis = clock(),
            )
        }

        noteDao.update(resolved)
        log("Resolved conflict for note $noteId with $resolution")
    }

    override suspend fun deleteNote(noteId: Long) {
        val existing = noteDao.getNote(noteId) ?: return
        if (existing.remoteId == null) {
            noteDao.hardDelete(noteId)
            log("Hard-deleted never-synced note $noteId")
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
        log("Created delete tombstone for note $noteId")
    }

    override suspend fun syncNow(): SyncResult {
        if (syncMutex.isLocked) {
            log("Sync skipped because another sync is already running")
            return SyncResult(pushed = 0, pulled = 0, failed = 0)
        }

        return syncMutex.withLock {
            syncNowLocked()
        }
    }

    private suspend fun syncNowLocked(): SyncResult {
        log("Sync started")
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

                    PendingOperation.Update -> updateRemoteUnlessConflict(note)

                    PendingOperation.Delete -> {
                        notesApi.deleteNote(note.remoteId ?: continue)
                        noteDao.hardDelete(note.localId)
                        log("Pushed delete for local note ${note.localId}")
                        pushed += 1
                        null
                    }

                    PendingOperation.None -> null
                }

                if (remote != null) {
                    noteDao.update(note.syncedWith(remote))
                    log("Pushed ${note.pendingOperation} for local note ${note.localId}")
                    pushed += 1
                }
            } catch (_: IllegalStateException) {
                noteDao.update(note.copy(syncStatus = SyncStatus.Failed.name))
                log("Sync failed for local note ${note.localId}")
                failed += 1
            }
        }

        val pulled = pullRemoteNotes()
        log("Sync finished: pushed $pushed, pulled $pulled, failed $failed")
        return SyncResult(pushed = pushed, pulled = pulled, failed = failed)
    }

    private suspend fun pullRemoteNotes(): Int {
        var pulled = 0
        for (remote in notesApi.getNotes()) {
            val local = noteDao.getNoteByRemoteId(remote.remoteId)
            if (local == null) {
                noteDao.insert(remote.toEntity())
                log("Pulled new remote note ${remote.remoteId}")
                pulled += 1
            } else if (remote.updatedAtMillis > local.updatedAtMillis) {
                if (local.pendingOperation == PendingOperation.None.name) {
                    noteDao.update(local.updatedFrom(remote))
                    log("Pulled update for remote note ${remote.remoteId}")
                    pulled += 1
                } else {
                    noteDao.update(local.conflictedWith(remote))
                    log("Detected conflict for remote note ${remote.remoteId}")
                }
            }
        }
        return pulled
    }

    private suspend fun updateRemoteUnlessConflict(local: NoteEntity): RemoteNote? {
        val remoteId = local.remoteId ?: error("Pending update is missing remote ID")
        val remoteBeforePush = notesApi.getNotes().firstOrNull { it.remoteId == remoteId }
        if (remoteBeforePush != null && remoteBeforePush.updatedAtMillis > local.updatedAtMillis) {
            noteDao.update(local.conflictedWith(remoteBeforePush))
            log("Blocked push because remote note $remoteId has a newer version")
            return null
        }
        return notesApi.updateNote(
            remoteId = remoteId,
            title = local.title,
            body = local.body,
            updatedAtMillis = local.updatedAtMillis,
        )
    }

    private fun NoteEntity.syncedWith(remote: RemoteNote): NoteEntity =
        copy(
            remoteId = remote.remoteId,
            title = remote.title,
            body = remote.body,
            syncStatus = SyncStatus.Synced.name,
            pendingOperation = PendingOperation.None.name,
            conflictTitle = null,
            conflictBody = null,
            conflictUpdatedAtMillis = null,
            updatedAtMillis = remote.updatedAtMillis,
        )

    private fun NoteEntity.updatedFrom(remote: RemoteNote): NoteEntity =
        copy(
            title = remote.title,
            body = remote.body,
            syncStatus = SyncStatus.Synced.name,
            pendingOperation = PendingOperation.None.name,
            conflictTitle = null,
            conflictBody = null,
            conflictUpdatedAtMillis = null,
            updatedAtMillis = remote.updatedAtMillis,
        )

    private fun NoteEntity.conflictedWith(remote: RemoteNote): NoteEntity =
        copy(
            syncStatus = SyncStatus.Conflict.name,
            conflictTitle = remote.title,
            conflictBody = remote.body,
            conflictUpdatedAtMillis = remote.updatedAtMillis,
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

    private fun RemoteNote.toDomain(): RemoteFieldNote =
        RemoteFieldNote(
            remoteId = remoteId,
            title = title,
            body = body,
        )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String,
        defaultValue: T,
    ): T =
        enumValues<T>().firstOrNull { it.name == value } ?: defaultValue

    private fun log(message: String) {
        _syncLog.update { entries ->
            (listOf(message) + entries).take(20)
        }
    }

    private fun mergeText(local: String, remote: String?, label: String): String {
        val cleanRemote = remote.orEmpty()
        if (cleanRemote.isBlank() || cleanRemote == local) return local
        if (local.isBlank()) return cleanRemote

        return """
            Local $label:
            $local

            Remote $label:
            $cleanRemote
        """.trimIndent()
    }
}
