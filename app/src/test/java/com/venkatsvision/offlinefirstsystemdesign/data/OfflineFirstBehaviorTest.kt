package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstBehaviorTest {
    @Test
    fun createNote_savesLocallyAsPendingCreateBeforeSync() = runTest {
        val repository = FakeNotesRepository(initialNotes = emptyList())

        repository.createNote("Offline note", "Saved without network")

        val note = repository.notes.first().first()
        assertEquals("Offline note", note.title)
        assertEquals(SyncStatus.PendingCreate, note.syncStatus)
        assertEquals(PendingOperation.Create, note.pendingOperation)
    }

    @Test
    fun updateSyncedNote_marksPendingUpdate() = runTest {
        val repository = FakeNotesRepository(
            initialNotes = listOf(
                FieldNote(
                    id = 1L,
                    remoteId = "remote-1",
                    title = "Synced",
                    body = "Original",
                    syncStatus = SyncStatus.Synced,
                    pendingOperation = PendingOperation.None,
                ),
            ),
        )

        repository.updateNote(1L, "Local edit", "Changed offline")

        val note = repository.notes.first().first()
        assertEquals("Local edit", note.title)
        assertEquals(SyncStatus.PendingUpdate, note.syncStatus)
        assertEquals(PendingOperation.Update, note.pendingOperation)
    }

    @Test
    fun syncNow_clearsPendingOperations() = runTest {
        val repository = FakeNotesRepository(initialNotes = emptyList())
        repository.createNote("Pending", "Needs sync")

        val result = repository.syncNow()

        val note = repository.notes.first().first()
        assertEquals(1, result.pushed)
        assertEquals(SyncStatus.Synced, note.syncStatus)
        assertEquals(PendingOperation.None, note.pendingOperation)
        assertTrue(note.remoteId != null)
    }

    @Test
    fun syncLog_recordsLocalWriteAndSync() = runTest {
        val repository = FakeNotesRepository(initialNotes = emptyList())

        repository.createNote("Logged note", "Observe me")
        repository.syncNow()

        val entries = repository.syncLog.first()
        assertTrue(entries.any { it.contains("Created fake local note") })
        assertTrue(entries.any { it.contains("Fake sync pushed") })
    }

    @Test
    fun deleteNote_removesLocalVisibleNote() = runTest {
        val repository = FakeNotesRepository()
        val noteId = repository.notes.first().first().id

        repository.deleteNote(noteId)

        assertEquals(emptyList<FieldNote>(), repository.notes.first())
    }
}
