package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val notesRepository: NotesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())

    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notesRepository.seedStarterNoteIfEmpty()
        }
        viewModelScope.launch {
            notesRepository.notes.collect { notes ->
                _uiState.update { current ->
                    current.copy(notes = notes)
                }
            }
        }
    }

    fun onEvent(event: NotesUiEvent) {
        when (event) {
            is NotesUiEvent.TitleChanged -> updateTitle(event.title)
            is NotesUiEvent.BodyChanged -> updateBody(event.body)
            is NotesUiEvent.EditNote -> startEditing(event.noteId)
            NotesUiEvent.ClearEditor -> clearEditor()
            NotesUiEvent.SaveNote -> saveNote()
            NotesUiEvent.SyncNow -> syncNow()
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

    private fun startEditing(noteId: Long) {
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
        if (current.editingNoteId == null) {
            viewModelScope.launch {
                notesRepository.createNote(title = title, body = cleanBody)
            }
        } else {
            viewModelScope.launch {
                notesRepository.updateNote(
                    noteId = current.editingNoteId,
                    title = title,
                    body = cleanBody,
                )
            }
        }

        clearEditor()
    }

    private fun syncNow() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(isSyncing = true, lastSyncMessage = "Syncing...")
            }
            val result = notesRepository.syncNow()
            _uiState.update { current ->
                current.copy(
                    isSyncing = false,
                    lastSyncMessage = result.message,
                )
            }
        }
    }
}
