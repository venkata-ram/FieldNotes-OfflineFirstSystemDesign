package com.venkatsvision.offlinefirstsystemdesign.ui.notes

sealed interface NotesUiEvent {
    data class TitleChanged(val title: String) : NotesUiEvent
    data class BodyChanged(val body: String) : NotesUiEvent
    data class EditNote(val noteId: Int) : NotesUiEvent
    data object ClearEditor : NotesUiEvent
    data object SaveNote : NotesUiEvent
}
