package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote

data class NotesUiState(
    val notes: List<FieldNote> = emptyList(),
    val editingNoteId: Int? = null,
    val editorTitle: String = "",
    val editorBody: String = "",
) {
    val isEditing: Boolean
        get() = editingNoteId != null
}
