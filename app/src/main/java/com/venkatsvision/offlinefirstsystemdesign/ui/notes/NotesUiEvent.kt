package com.venkatsvision.offlinefirstsystemdesign.ui.notes

sealed interface NotesUiEvent {
    data class TitleChanged(val title: String) : NotesUiEvent
    data class BodyChanged(val body: String) : NotesUiEvent
    data class EditNote(val noteId: Long) : NotesUiEvent
    data class DeleteNote(val noteId: Long) : NotesUiEvent
    data class KeepLocalConflict(val noteId: Long) : NotesUiEvent
    data class UseRemoteConflict(val noteId: Long) : NotesUiEvent
    data class EditRemoteNote(val remoteId: String) : NotesUiEvent
    data class RemoteTitleChanged(val title: String) : NotesUiEvent
    data class RemoteBodyChanged(val body: String) : NotesUiEvent
    data class AutoBackgroundSyncChanged(val enabled: Boolean) : NotesUiEvent
    data object ClearEditor : NotesUiEvent
    data object ClearRemoteEditor : NotesUiEvent
    data object SaveRemoteNote : NotesUiEvent
    data object SaveNote : NotesUiEvent
    data class SyncNow(val isOnline: Boolean) : NotesUiEvent
}
