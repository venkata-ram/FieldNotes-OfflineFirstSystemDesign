package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteDao
import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteEntity
import com.venkatsvision.offlinefirstsystemdesign.data.remote.FakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.data.remote.RemoteNote
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomNotesRepositoryConcurrencyTest {
    @Test
    fun syncNow_whenCalledConcurrently_pushesPendingCreateOnce() = runTest {
        val noteDao = InMemoryNoteDao(
            NoteEntity(
                localId = 1L,
                title = "Pending",
                body = "Created offline",
                syncStatus = SyncStatus.PendingCreate.name,
                pendingOperation = PendingOperation.Create.name,
                updatedAtMillis = 1000L,
            ),
        )
        val notesApi = CountingFakeNotesApi()
        val repository = RoomNotesRepository(
            noteDao = noteDao,
            notesApi = notesApi,
            clock = { 2000L },
        )

        val first = async { repository.syncNow() }
        val second = async { repository.syncNow() }

        first.await()
        second.await()

        assertEquals(1, notesApi.createCount)
        assertEquals("remote-1", noteDao.notes.value.single().remoteId)
        assertEquals(PendingOperation.None.name, noteDao.notes.value.single().pendingOperation)
    }
}

private class InMemoryNoteDao(
    initialNote: NoteEntity,
) : NoteDao {
    val notes = MutableStateFlow(listOf(initialNote))

    override fun observeNotes(): Flow<List<NoteEntity>> = notes

    override suspend fun getNote(localId: Long): NoteEntity? =
        notes.value.firstOrNull { it.localId == localId }

    override suspend fun getNoteByRemoteId(remoteId: String): NoteEntity? =
        notes.value.firstOrNull { it.remoteId == remoteId }

    override suspend fun getPendingNotes(): List<NoteEntity> =
        notes.value.filter { it.pendingOperation != PendingOperation.None.name }

    override suspend fun hardDelete(localId: Long) {
        notes.value = notes.value.filterNot { it.localId == localId }
    }

    override suspend fun countNotes(): Int = notes.value.size

    override suspend fun insert(note: NoteEntity): Long {
        val nextId = (notes.value.maxOfOrNull { it.localId } ?: 0L) + 1L
        notes.value = notes.value + note.copy(localId = nextId)
        return nextId
    }

    override suspend fun update(note: NoteEntity) {
        notes.value = notes.value.map { existing ->
            if (existing.localId == note.localId) note else existing
        }
    }
}

private class CountingFakeNotesApi : FakeNotesApi {
    private val notes = linkedMapOf<String, RemoteNote>()
    var createCount = 0
        private set

    override suspend fun getNotes(): List<RemoteNote> =
        notes.values.toList()

    override suspend fun createNote(
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote {
        createCount += 1
        delay(50)
        val remote = RemoteNote(
            remoteId = "remote-$createCount",
            title = title,
            body = body,
            updatedAtMillis = updatedAtMillis,
        )
        notes[remote.remoteId] = remote
        return remote
    }

    override suspend fun updateNote(
        remoteId: String,
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote {
        val remote = RemoteNote(remoteId, title, body, updatedAtMillis)
        notes[remoteId] = remote
        return remote
    }

    override suspend fun deleteNote(remoteId: String) {
        notes.remove(remoteId)
    }
}
