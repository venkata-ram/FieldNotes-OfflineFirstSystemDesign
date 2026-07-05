package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.RemoteFieldNote

data class NotesUiState(
    val notes: List<FieldNote> = emptyList(),
    val remoteNotes: List<RemoteFieldNote> = emptyList(),
    val editingNoteId: Long? = null,
    val editorTitle: String = "",
    val editorBody: String = "",
    val editingRemoteId: String? = null,
    val remoteEditorTitle: String = "",
    val remoteEditorBody: String = "",
    val isSyncing: Boolean = false,
    val autoBackgroundSyncEnabled: Boolean = false,
    val lastSyncMessage: String = "Not synced in this session",
    val syncLog: List<String> = emptyList(),
) {
    val isEditing: Boolean
        get() = editingNoteId != null

    val isEditingRemote: Boolean
        get() = editingRemoteId != null
}
