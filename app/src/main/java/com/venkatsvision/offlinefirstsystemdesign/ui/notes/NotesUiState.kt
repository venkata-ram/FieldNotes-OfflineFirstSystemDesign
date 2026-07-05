package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote

data class NotesUiState(
    val notes: List<FieldNote> = emptyList(),
    val editingNoteId: Long? = null,
    val editorTitle: String = "",
    val editorBody: String = "",
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = "Not synced in this session",
) {
    val isEditing: Boolean
        get() = editingNoteId != null
}
