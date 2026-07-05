package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeNotesRepository(
    initialNotes: List<FieldNote> = listOf(
        FieldNote(
            id = 1L,
            title = "Inspect storage before syncing",
            body = "Offline-first screens should read from local state first.",
            syncStatus = SyncStatus.Synced,
            pendingOperation = PendingOperation.None,
        ),
    ),
) : NotesRepository {
    private var nextId = initialNotes.maxOfOrNull { it.id + 1 } ?: 1L
    private val notesFlow = MutableStateFlow(initialNotes)

    override val notes: Flow<List<FieldNote>> = notesFlow

    override suspend fun seedStarterNoteIfEmpty() {
        if (notesFlow.value.isNotEmpty()) return

        notesFlow.value = listOf(
            FieldNote(
                id = nextId++,
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
                    title = title,
                    body = body,
                    syncStatus = SyncStatus.PendingCreate,
                    pendingOperation = PendingOperation.Create,
                ),
            ) + notes
        }
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
    }
}
