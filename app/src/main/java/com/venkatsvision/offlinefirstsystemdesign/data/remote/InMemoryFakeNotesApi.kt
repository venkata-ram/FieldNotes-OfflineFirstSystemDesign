package com.venkatsvision.offlinefirstsystemdesign.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class InMemoryFakeNotesApi(
    private val delayMillis: Long = 300,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : FakeNotesApi {
    private val remoteStore = linkedMapOf<String, RemoteNote>()
    private val notesFlow = MutableStateFlow(emptyList<RemoteNote>())
    var failNextRequest: Boolean = false

    override val notes: Flow<List<RemoteNote>> = notesFlow.asStateFlow()

    override suspend fun getNotes(): List<RemoteNote> {
        simulateNetwork()
        return sortedNotes()
    }

    override suspend fun createNote(
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote {
        simulateNetwork()
        val note = RemoteNote(
            remoteId = "remote-${UUID.randomUUID()}",
            title = title,
            body = body,
            updatedAtMillis = maxOf(updatedAtMillis, clock()),
        )
        remoteStore[note.remoteId] = note
        publishNotes()
        return note
    }

    override suspend fun updateNote(
        remoteId: String,
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote {
        simulateNetwork()
        val note = RemoteNote(
            remoteId = remoteId,
            title = title,
            body = body,
            updatedAtMillis = maxOf(updatedAtMillis, clock()),
        )
        remoteStore[remoteId] = note
        publishNotes()
        return note
    }

    override suspend fun deleteNote(remoteId: String) {
        simulateNetwork()
        remoteStore.remove(remoteId)
        publishNotes()
    }

    private fun publishNotes() {
        notesFlow.value = sortedNotes()
    }

    private fun sortedNotes(): List<RemoteNote> =
        remoteStore.values.sortedByDescending { it.updatedAtMillis }

    private suspend fun simulateNetwork() {
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        if (failNextRequest) {
            failNextRequest = false
            error("Fake network failure")
        }
    }
}
