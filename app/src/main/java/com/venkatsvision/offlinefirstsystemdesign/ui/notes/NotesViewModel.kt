package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venkatsvision.offlinefirstsystemdesign.domain.ConflictResolution
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val scheduleBackgroundSync: () -> Unit = {},
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
        viewModelScope.launch {
            notesRepository.syncLog.collect { syncLog ->
                _uiState.update { current ->
                    current.copy(syncLog = syncLog)
                }
            }
        }
    }

    fun onEvent(event: NotesUiEvent) {
        when (event) {
            is NotesUiEvent.TitleChanged -> updateTitle(event.title)
            is NotesUiEvent.BodyChanged -> updateBody(event.body)
            is NotesUiEvent.EditNote -> startEditing(event.noteId)
            is NotesUiEvent.DeleteNote -> deleteNote(event.noteId)
            is NotesUiEvent.SimulateRemoteEdit -> simulateRemoteEdit(event.noteId)
            is NotesUiEvent.KeepLocalConflict -> resolveConflict(event.noteId, ConflictResolution.KeepLocal)
            is NotesUiEvent.UseRemoteConflict -> resolveConflict(event.noteId, ConflictResolution.UseRemote)
            is NotesUiEvent.AutoBackgroundSyncChanged -> setAutoBackgroundSync(event.enabled)
            NotesUiEvent.ClearEditor -> clearEditor()
            NotesUiEvent.SaveNote -> saveNote()
            is NotesUiEvent.SyncNow -> syncNow(event.isOnline)
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
                scheduleBackgroundSyncIfEnabled()
            }
        } else {
            viewModelScope.launch {
                notesRepository.updateNote(
                    noteId = current.editingNoteId,
                    title = title,
                    body = cleanBody,
                )
                scheduleBackgroundSyncIfEnabled()
            }
        }

        clearEditor()
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
            scheduleBackgroundSyncIfEnabled()
        }
        if (_uiState.value.editingNoteId == noteId) {
            clearEditor()
        }
    }

    private fun simulateRemoteEdit(noteId: Long) {
        viewModelScope.launch {
            notesRepository.simulateRemoteEdit(noteId)
            _uiState.update { current ->
                current.copy(lastSyncMessage = "Remote edit staged. Now edit this same note locally, save, then sync.")
            }
        }
    }

    private fun resolveConflict(noteId: Long, resolution: ConflictResolution) {
        viewModelScope.launch {
            notesRepository.resolveConflict(noteId, resolution)
            if (resolution == ConflictResolution.KeepLocal) {
                scheduleBackgroundSyncIfEnabled()
            }
        }
    }

    private fun setAutoBackgroundSync(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                autoBackgroundSyncEnabled = enabled,
                lastSyncMessage = if (enabled) {
                    "Auto background sync enabled."
                } else {
                    "Auto background sync paused for manual demo control."
                },
            )
        }
    }

    private fun scheduleBackgroundSyncIfEnabled() {
        if (_uiState.value.autoBackgroundSyncEnabled) {
            scheduleBackgroundSync()
        }
    }

    private fun syncNow(isOnline: Boolean) {
        if (!isOnline) {
            _uiState.update { current ->
                current.copy(lastSyncMessage = "Offline. Local changes are saved and will sync when network returns.")
            }
            return
        }

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
