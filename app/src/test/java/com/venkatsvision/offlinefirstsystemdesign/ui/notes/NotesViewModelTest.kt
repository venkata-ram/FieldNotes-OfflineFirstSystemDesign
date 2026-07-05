package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import com.venkatsvision.offlinefirstsystemdesign.MainDispatcherRule
import com.venkatsvision.offlinefirstsystemdesign.data.FakeNotesRepository
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveNote_addsLocalNoteAndClearsEditor() = runTest {
        val viewModel = NotesViewModel(FakeNotesRepository())

        viewModel.onEvent(NotesUiEvent.TitleChanged("Trail report"))
        viewModel.onEvent(NotesUiEvent.BodyChanged("Bridge is closed"))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        val state = viewModel.uiState.value
        assertEquals("Trail report", state.notes.first().title)
        assertEquals("Bridge is closed", state.notes.first().body)
        assertEquals(SyncStatus.PendingCreate, state.notes.first().syncStatus)
        assertEquals(PendingOperation.Create, state.notes.first().pendingOperation)
        assertEquals("", state.editorTitle)
        assertEquals("", state.editorBody)
        assertFalse(state.isEditing)
    }

    @Test
    fun saveNote_updatesSelectedNote() = runTest {
        val viewModel = NotesViewModel(FakeNotesRepository())
        val noteId = viewModel.uiState.value.notes.first().id

        viewModel.onEvent(NotesUiEvent.EditNote(noteId))
        viewModel.onEvent(NotesUiEvent.TitleChanged("Updated storage plan"))
        viewModel.onEvent(NotesUiEvent.BodyChanged("Room will become the source of truth"))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        val state = viewModel.uiState.value
        assertEquals("Updated storage plan", state.notes.first().title)
        assertEquals("Room will become the source of truth", state.notes.first().body)
        assertEquals(SyncStatus.PendingUpdate, state.notes.first().syncStatus)
        assertEquals(PendingOperation.Update, state.notes.first().pendingOperation)
        assertFalse(state.isEditing)
    }

    @Test
    fun saveNote_ignoresBlankInput() = runTest {
        val viewModel = NotesViewModel(FakeNotesRepository())
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
