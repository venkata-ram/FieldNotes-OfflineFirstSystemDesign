package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesViewModelTest {
    @Test
    fun saveNote_addsLocalNoteAndClearsEditor() {
        val viewModel = NotesViewModel()

        viewModel.onEvent(NotesUiEvent.TitleChanged("Trail report"))
        viewModel.onEvent(NotesUiEvent.BodyChanged("Bridge is closed"))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        val state = viewModel.uiState.value
        assertEquals("Trail report", state.notes.first().title)
        assertEquals("Bridge is closed", state.notes.first().body)
        assertEquals("Local only", state.notes.first().localLabel)
        assertEquals("", state.editorTitle)
        assertEquals("", state.editorBody)
        assertFalse(state.isEditing)
    }

    @Test
    fun saveNote_updatesSelectedNote() {
        val viewModel = NotesViewModel()
        val noteId = viewModel.uiState.value.notes.first().id

        viewModel.onEvent(NotesUiEvent.EditNote(noteId))
        viewModel.onEvent(NotesUiEvent.TitleChanged("Updated storage plan"))
        viewModel.onEvent(NotesUiEvent.BodyChanged("Room will become the source of truth"))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        val state = viewModel.uiState.value
        assertEquals("Updated storage plan", state.notes.first().title)
        assertEquals("Room will become the source of truth", state.notes.first().body)
        assertEquals("Edited locally", state.notes.first().localLabel)
        assertFalse(state.isEditing)
    }

    @Test
    fun saveNote_ignoresBlankInput() {
        val viewModel = NotesViewModel()
        val originalNotes = viewModel.uiState.value.notes

        viewModel.onEvent(NotesUiEvent.TitleChanged("   "))
        viewModel.onEvent(NotesUiEvent.BodyChanged("   "))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        val state = viewModel.uiState.value
        assertEquals(originalNotes, state.notes)
        assertTrue(state.editorTitle.isBlank())
        assertTrue(state.editorBody.isBlank())
    }
}
