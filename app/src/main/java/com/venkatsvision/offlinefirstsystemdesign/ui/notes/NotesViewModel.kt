package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.lifecycle.ViewModel
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotesViewModel : ViewModel() {
    private var nextId = 2

    private val _uiState = MutableStateFlow(
        NotesUiState(
            notes = listOf(
                FieldNote(
                    id = 1,
                    title = "Inspect storage before syncing",
                    body = "Offline-first screens should read from local state first. Later milestones will replace this in-memory list with Room.",
                    localLabel = "Local only",
                ),
            ),
        ),
    )

    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun onEvent(event: NotesUiEvent) {
        when (event) {
            is NotesUiEvent.TitleChanged -> updateTitle(event.title)
            is NotesUiEvent.BodyChanged -> updateBody(event.body)
            is NotesUiEvent.EditNote -> startEditing(event.noteId)
            NotesUiEvent.ClearEditor -> clearEditor()
            NotesUiEvent.SaveNote -> saveNote()
        }
    }

    private fun updateTitle(title: String) {
        _uiState.update { current ->
            current.copy(editorTitle = title)
        }
    }

    private fun updateBody(body: String) {
        _uiState.update { current ->
            current.copy(editorBody = body)
        }
    }

    private fun startEditing(noteId: Int) {
        val note = _uiState.value.notes.firstOrNull { it.id == noteId } ?: return
        _uiState.update { current ->
            current.copy(
                editingNoteId = note.id,
                editorTitle = note.title,
                editorBody = note.body,
            )
        }
    }

    private fun clearEditor() {
        _uiState.update { current ->
            current.copy(
                editingNoteId = null,
                editorTitle = "",
                editorBody = "",
            )
        }
    }

    private fun saveNote() {
        val current = _uiState.value
        val cleanTitle = current.editorTitle.trim()
        val cleanBody = current.editorBody.trim()
        if (cleanTitle.isEmpty() && cleanBody.isEmpty()) return

        val title = cleanTitle.ifEmpty { "Untitled note" }
        val updatedNotes = if (current.editingNoteId == null) {
            listOf(
                FieldNote(
                    id = nextId,
                    title = title,
                    body = cleanBody,
                    localLabel = "Local only",
                ),
            ) + current.notes
        } else {
            current.notes.map { note ->
                if (note.id == current.editingNoteId) {
                    note.copy(
                        title = title,
                        body = cleanBody,
                        localLabel = "Edited locally",
                    )
                } else {
                    note
                }
            }
        }

        if (current.editingNoteId == null) {
            nextId += 1
        }

        _uiState.value = current.copy(
            notes = updatedNotes,
            editingNoteId = null,
            editorTitle = "",
            editorBody = "",
        )
    }
}
