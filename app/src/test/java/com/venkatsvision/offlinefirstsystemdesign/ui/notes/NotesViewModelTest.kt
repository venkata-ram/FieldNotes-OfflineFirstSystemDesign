package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import com.venkatsvision.offlinefirstsystemdesign.MainDispatcherRule
import com.venkatsvision.offlinefirstsystemdesign.data.FakeNotesRepository
import com.venkatsvision.offlinefirstsystemdesign.data.sync.SyncScheduler
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
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

    @Test
    fun syncNow_marksPendingNotesSynced() = runTest {
        val viewModel = NotesViewModel(FakeNotesRepository())

        viewModel.onEvent(NotesUiEvent.TitleChanged("Trail report"))
        viewModel.onEvent(NotesUiEvent.SaveNote)
        viewModel.onEvent(NotesUiEvent.SyncNow(isOnline = true))

        val state = viewModel.uiState.value
        assertEquals(SyncStatus.Synced, state.notes.first().syncStatus)
        assertEquals(PendingOperation.None, state.notes.first().pendingOperation)
        assertEquals("Sync complete: pushed 1, pulled 0", state.lastSyncMessage)
        assertFalse(state.isSyncing)
    }

    @Test
    fun syncNow_whenOfflineDoesNotPushPendingNotes() = runTest {
        val viewModel = NotesViewModel(FakeNotesRepository())

        viewModel.onEvent(NotesUiEvent.TitleChanged("Offline write"))
        viewModel.onEvent(NotesUiEvent.SaveNote)
        viewModel.onEvent(NotesUiEvent.SyncNow(isOnline = false))

        val state = viewModel.uiState.value
        assertEquals(SyncStatus.PendingCreate, state.notes.first().syncStatus)
        assertEquals(PendingOperation.Create, state.notes.first().pendingOperation)
        assertEquals(
            "Offline. Local changes are saved and will sync when network returns.",
            state.lastSyncMessage,
        )
        assertFalse(state.isSyncing)
    }

    @Test
    fun saveNote_withAutoSyncDisabled_doesNotScheduleBackgroundSync() = runTest {
        val syncScheduler = CountingSyncScheduler()
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(),
            syncScheduler = syncScheduler,
        )

        viewModel.onEvent(NotesUiEvent.TitleChanged("Needs background sync"))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        assertEquals(0, syncScheduler.scheduleCount)
    }

    @Test
    fun saveNote_withAutoSyncEnabled_schedulesBackgroundSync() = runTest {
        val syncScheduler = CountingSyncScheduler()
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(),
            syncScheduler = syncScheduler,
        )

        viewModel.onEvent(NotesUiEvent.AutoBackgroundSyncChanged(enabled = true))
        viewModel.onEvent(NotesUiEvent.TitleChanged("Needs background sync"))
        viewModel.onEvent(NotesUiEvent.SaveNote)

        assertEquals(1, syncScheduler.scheduleCount)
    }

    @Test
    fun enablingAutoSync_withExistingPendingNote_schedulesBackgroundSync() = runTest {
        val syncScheduler = CountingSyncScheduler()
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(),
            syncScheduler = syncScheduler,
        )

        viewModel.onEvent(NotesUiEvent.TitleChanged("Created before auto sync"))
        viewModel.onEvent(NotesUiEvent.SaveNote)
        viewModel.onEvent(NotesUiEvent.AutoBackgroundSyncChanged(enabled = true))

        assertEquals(1, syncScheduler.scheduleCount)
        assertEquals(
            "Auto background sync enabled. Pending changes queued.",
            viewModel.uiState.value.lastSyncMessage,
        )
    }

    @Test
    fun deleteNote_removesNoteWithoutSchedulingWhenAutoSyncDisabled() = runTest {
        val syncScheduler = CountingSyncScheduler()
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(),
            syncScheduler = syncScheduler,
        )
        val noteId = viewModel.uiState.value.notes.first().id

        viewModel.onEvent(NotesUiEvent.DeleteNote(noteId))

        assertEquals(emptyList<FieldNote>(), viewModel.uiState.value.notes)
        assertEquals(0, syncScheduler.scheduleCount)
    }

    @Test
    fun keepLocalConflict_returnsNoteToPendingUpdateAndSchedulesSync() = runTest {
        val syncScheduler = CountingSyncScheduler()
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(
                initialNotes = listOf(
                    FieldNote(
                        id = 1L,
                        remoteId = "remote-1",
                        title = "Local title",
                        body = "Local body",
                        syncStatus = SyncStatus.Conflict,
                        pendingOperation = PendingOperation.Update,
                        conflictTitle = "Remote title",
                        conflictBody = "Remote body",
                    ),
                ),
            ),
            syncScheduler = syncScheduler,
        )

        viewModel.onEvent(NotesUiEvent.AutoBackgroundSyncChanged(enabled = true))
        viewModel.onEvent(NotesUiEvent.KeepLocalConflict(1L))

        val note = viewModel.uiState.value.notes.first()
        assertEquals("Local title", note.title)
        assertEquals(SyncStatus.PendingUpdate, note.syncStatus)
        assertEquals(null, note.conflictTitle)
        assertEquals(1, syncScheduler.scheduleCount)
    }

    @Test
    fun useRemoteConflict_acceptsRemoteVersion() = runTest {
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(
                initialNotes = listOf(
                    FieldNote(
                        id = 1L,
                        remoteId = "remote-1",
                        title = "Local title",
                        body = "Local body",
                        syncStatus = SyncStatus.Conflict,
                        pendingOperation = PendingOperation.Update,
                        conflictTitle = "Remote title",
                        conflictBody = "Remote body",
                    ),
                ),
            ),
        )

        viewModel.onEvent(NotesUiEvent.UseRemoteConflict(1L))

        val note = viewModel.uiState.value.notes.first()
        assertEquals("Remote title", note.title)
        assertEquals("Remote body", note.body)
        assertEquals(SyncStatus.Synced, note.syncStatus)
        assertEquals(PendingOperation.None, note.pendingOperation)
        assertEquals(null, note.conflictTitle)
    }

    @Test
    fun mergeBothConflict_combinesLocalAndRemoteVersions() = runTest {
        val viewModel = NotesViewModel(
            notesRepository = FakeNotesRepository(
                initialNotes = listOf(
                    FieldNote(
                        id = 1L,
                        remoteId = "remote-1",
                        title = "Local title",
                        body = "Local body",
                        syncStatus = SyncStatus.Conflict,
                        pendingOperation = PendingOperation.Update,
                        conflictTitle = "Remote title",
                        conflictBody = "Remote body",
                    ),
                ),
            ),
        )

        viewModel.onEvent(NotesUiEvent.MergeBothConflict(1L))

        val note = viewModel.uiState.value.notes.first()
        assertTrue(note.title.contains("Local title"))
        assertTrue(note.title.contains("Remote title"))
        assertTrue(note.body.contains("Local body"))
        assertTrue(note.body.contains("Remote body"))
        assertEquals(SyncStatus.PendingUpdate, note.syncStatus)
        assertEquals(PendingOperation.Update, note.pendingOperation)
        assertEquals(null, note.conflictTitle)
    }

    @Test
    fun saveRemoteNote_updatesRemoteCopyAndShowsDemoMessage() = runTest {
        val viewModel = NotesViewModel(FakeNotesRepository())
        val remoteId = viewModel.uiState.value.remoteNotes.first().remoteId

        viewModel.onEvent(NotesUiEvent.EditRemoteNote(remoteId))
        viewModel.onEvent(NotesUiEvent.RemoteTitleChanged("Server title"))
        viewModel.onEvent(NotesUiEvent.RemoteBodyChanged("Server body"))
        viewModel.onEvent(NotesUiEvent.SaveRemoteNote)

        val state = viewModel.uiState.value
        assertEquals("Server title", state.remoteNotes.first().title)
        assertEquals("Server body", state.remoteNotes.first().body)
        assertEquals(false, state.isEditingRemote)
        assertEquals(
            "Remote note edited. Now edit the local copy and sync to detect conflict.",
            state.lastSyncMessage,
        )
    }

    private class CountingSyncScheduler : SyncScheduler {
        var scheduleCount = 0
            private set

        override fun enqueueOneTimeSync() {
            scheduleCount += 1
        }
    }
}
