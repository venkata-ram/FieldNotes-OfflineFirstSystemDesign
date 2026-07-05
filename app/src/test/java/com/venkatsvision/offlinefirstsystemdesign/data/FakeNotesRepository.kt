package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import com.venkatsvision.offlinefirstsystemdesign.domain.ConflictResolution
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.RemoteFieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncResult
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeNotesRepository(
    initialNotes: List<FieldNote> = listOf(
        FieldNote(
            id = 1L,
            remoteId = "remote-1",
            title = "Inspect storage before syncing",
            body = "Offline-first screens should read from local state first.",
            syncStatus = SyncStatus.Synced,
            pendingOperation = PendingOperation.None,
        ),
    ),
) : NotesRepository {
    private var nextId = initialNotes.maxOfOrNull { it.id + 1 } ?: 1L
    private val notesFlow = MutableStateFlow(initialNotes)
    private val remoteNotesFlow = MutableStateFlow(
        initialNotes.mapNotNull { note ->
            note.remoteId?.let { remoteId ->
                RemoteFieldNote(remoteId, note.title, note.body)
            }
        },
    )
    private val syncLogFlow = MutableStateFlow(listOf("Fake sync log ready"))

    override val notes: Flow<List<FieldNote>> = notesFlow
    override val remoteNotes: Flow<List<RemoteFieldNote>> = remoteNotesFlow.asStateFlow()
    override val syncLog: Flow<List<String>> = syncLogFlow.asStateFlow()

    override suspend fun seedStarterNoteIfEmpty() {
        if (notesFlow.value.isNotEmpty()) return

        notesFlow.value = listOf(
            FieldNote(
                id = nextId++,
                remoteId = "remote-$nextId",
                title = "Inspect storage before syncing",
                body = "Offline-first screens should read from local state first.",
                syncStatus = SyncStatus.Synced,
                pendingOperation = PendingOperation.None,
            ),
        )
    }

    override suspend fun createNote(title: String, body: String) {
        notesFlow.update { notes ->
            listOf(
                FieldNote(
                    id = nextId++,
                    remoteId = null,
                    title = title,
                    body = body,
                    syncStatus = SyncStatus.PendingCreate,
                    pendingOperation = PendingOperation.Create,
                ),
            ) + notes
        }
        log("Created fake local note: $title")
    }

    override suspend fun updateNote(noteId: Long, title: String, body: String) {
        notesFlow.update { notes ->
            notes.map { note ->
                if (note.id == noteId) {
                    note.copy(
                        title = title,
                        body = body,
                        syncStatus = if (note.pendingOperation == PendingOperation.Create) {
                            SyncStatus.PendingCreate
                        } else {
                            SyncStatus.PendingUpdate
                        },
                        pendingOperation = if (note.pendingOperation == PendingOperation.Create) {
                            PendingOperation.Create
                        } else {
                            PendingOperation.Update
                        },
                    )
                } else {
                    note
                }
            }
        }
        log("Updated fake local note: $title")
    }

    override suspend fun deleteNote(noteId: Long) {
        notesFlow.update { notes ->
            notes.filterNot { it.id == noteId }
        }
        log("Deleted fake local note $noteId")
    }

    override suspend fun updateRemoteNote(remoteId: String, title: String, body: String) {
        remoteNotesFlow.update { remoteNotes ->
            remoteNotes.map { note ->
                if (note.remoteId == remoteId) {
                    note.copy(title = title, body = body)
                } else {
                    note
                }
            }
        }
        log("Edited fake remote note $remoteId")
    }

    override suspend fun resolveConflict(noteId: Long, resolution: ConflictResolution) {
        notesFlow.update { notes ->
            notes.map { note ->
                if (note.id == noteId) {
                    when (resolution) {
                        ConflictResolution.KeepLocal -> note.copy(
                            syncStatus = SyncStatus.PendingUpdate,
                            pendingOperation = PendingOperation.Update,
                            conflictTitle = null,
                            conflictBody = null,
                        )

                        ConflictResolution.UseRemote -> note.copy(
                            title = note.conflictTitle ?: note.title,
                            body = note.conflictBody ?: note.body,
                            syncStatus = SyncStatus.Synced,
                            pendingOperation = PendingOperation.None,
                            conflictTitle = null,
                            conflictBody = null,
                        )

                        ConflictResolution.MergeBoth -> note.copy(
                            title = mergeText(note.title, note.conflictTitle, "title"),
                            body = mergeText(note.body, note.conflictBody, "body"),
                            syncStatus = SyncStatus.PendingUpdate,
                            pendingOperation = PendingOperation.Update,
                            conflictTitle = null,
                            conflictBody = null,
                        )
                    }
                } else {
                    note
                }
            }
        }
        log("Resolved fake conflict for note $noteId")
    }

    override suspend fun syncNow(): SyncResult {
        val pendingCount = notesFlow.value.count { it.pendingOperation != PendingOperation.None }
        notesFlow.update { notes ->
            notes.map { note ->
                note.copy(
                    remoteId = note.remoteId ?: "remote-${note.id}",
                    syncStatus = SyncStatus.Synced,
                    pendingOperation = PendingOperation.None,
                )
            }
        }
        remoteNotesFlow.value = notesFlow.value.mapNotNull { note ->
            note.remoteId?.let { remoteId ->
                RemoteFieldNote(remoteId, note.title, note.body)
            }
        }
        log("Fake sync pushed $pendingCount note(s)")
        return SyncResult(pushed = pendingCount, pulled = 0, failed = 0)
    }

    private fun log(message: String) {
        syncLogFlow.update { entries ->
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
