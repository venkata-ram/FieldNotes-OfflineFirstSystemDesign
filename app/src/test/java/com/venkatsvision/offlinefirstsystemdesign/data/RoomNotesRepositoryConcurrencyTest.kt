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

    @Test
    fun syncNow_whenFakeApiResetsRemoteId_doesNotCrashAndKeepsPendingCreate() = runTest {
        val noteDao = InMemoryNoteDao(
            listOf(
                NoteEntity(
                    localId = 1L,
                    remoteId = "remote-1",
                    title = "Already synced",
                    body = "Persisted from a previous app process",
                    syncStatus = SyncStatus.Synced.name,
                    pendingOperation = PendingOperation.None.name,
                    updatedAtMillis = 1000L,
                ),
                NoteEntity(
                    localId = 2L,
                    title = "Pending after restart",
                    body = "Fake API forgot old remote IDs",
                    syncStatus = SyncStatus.PendingCreate.name,
                    pendingOperation = PendingOperation.Create.name,
                    updatedAtMillis = 2000L,
                ),
            ),
        )
        val repository = RoomNotesRepository(
            noteDao = noteDao,
            notesApi = FixedCreateIdFakeNotesApi(remoteId = "remote-1"),
            clock = { 3000L },
        )

        val result = repository.syncNow()

        val pending = noteDao.notes.value.single { it.localId == 2L }
        assertEquals(0, result.pushed)
        assertEquals(1, result.failed)
        assertEquals(SyncStatus.Failed.name, pending.syncStatus)
        assertEquals(PendingOperation.Create.name, pending.pendingOperation)
    }
}

private class InMemoryNoteDao(
    initialNotes: List<NoteEntity>,
) : NoteDao {
    constructor(initialNote: NoteEntity) : this(listOf(initialNote))

    val notes = MutableStateFlow(initialNotes)

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
        val inserted = note.copy(localId = nextId)
        enforceUniqueRemoteId(inserted)
        notes.value = notes.value + inserted
        return nextId
    }

    override suspend fun update(note: NoteEntity) {
        enforceUniqueRemoteId(note)
        notes.value = notes.value.map { existing ->
            if (existing.localId == note.localId) note else existing
        }
    }

    private fun enforceUniqueRemoteId(note: NoteEntity) {
        val remoteId = note.remoteId ?: return
        val duplicate = notes.value.any { existing ->
            existing.localId != note.localId && existing.remoteId == remoteId
        }
        if (duplicate) {
            error("UNIQUE constraint failed: notes.remoteId")
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

private class FixedCreateIdFakeNotesApi(
    private val remoteId: String,
) : FakeNotesApi {
    override suspend fun getNotes(): List<RemoteNote> = emptyList()

    override suspend fun createNote(
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote =
        RemoteNote(
            remoteId = remoteId,
            title = title,
            body = body,
            updatedAtMillis = updatedAtMillis,
        )

    override suspend fun updateNote(
        remoteId: String,
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote =
        RemoteNote(remoteId, title, body, updatedAtMillis)

    override suspend fun deleteNote(remoteId: String) = Unit
}
