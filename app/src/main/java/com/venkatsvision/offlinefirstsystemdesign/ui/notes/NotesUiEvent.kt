package com.venkatsvision.offlinefirstsystemdesign.ui.notes

sealed interface NotesUiEvent {
    data class TitleChanged(val title: String) : NotesUiEvent
    data class BodyChanged(val body: String) : NotesUiEvent
    data class EditNote(val noteId: Long) : NotesUiEvent
    data class DeleteNote(val noteId: Long) : NotesUiEvent
    data class SimulateRemoteEdit(val noteId: Long) : NotesUiEvent
    data class KeepLocalConflict(val noteId: Long) : NotesUiEvent
    data class UseRemoteConflict(val noteId: Long) : NotesUiEvent
    data object ClearEditor : NotesUiEvent
    data object SaveNote : NotesUiEvent
    data object SyncNow : NotesUiEvent
}
