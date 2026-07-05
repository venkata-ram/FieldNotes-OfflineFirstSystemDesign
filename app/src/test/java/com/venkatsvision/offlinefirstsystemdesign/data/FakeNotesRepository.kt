package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeNotesRepository(
    initialNotes: List<FieldNote> = listOf(
        FieldNote(
            id = 1L,
            title = "Inspect storage before syncing",
            body = "Offline-first screens should read from local state first.",
            localLabel = "Stored locally",
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
                localLabel = "Stored locally",
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
                    localLabel = "Stored locally",
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
                        localLabel = "Updated locally",
                    )
                } else {
                    note
                }
            }
        }
    }
}
