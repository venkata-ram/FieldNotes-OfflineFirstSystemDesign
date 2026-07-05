package com.venkatsvision.offlinefirstsystemdesign.data.remote

import kotlinx.coroutines.delay

class InMemoryFakeNotesApi(
    private val delayMillis: Long = 300,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : FakeNotesApi {
    private val notes = linkedMapOf<String, RemoteNote>()
    private var nextRemoteId = 1
    var failNextRequest: Boolean = false

    override suspend fun getNotes(): List<RemoteNote> {
        simulateNetwork()
        return notes.values.sortedByDescending { it.updatedAtMillis }
    }

    override suspend fun createNote(
        title: String,
        body: String,
        updatedAtMillis: Long,
    ): RemoteNote {
        simulateNetwork()
        val note = RemoteNote(
            remoteId = "remote-${nextRemoteId++}",
            title = title,
            body = body,
            updatedAtMillis = maxOf(updatedAtMillis, clock()),
        )
        notes[note.remoteId] = note
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
        notes[remoteId] = note
        return note
    }

    override suspend fun deleteNote(remoteId: String) {
        simulateNetwork()
        notes.remove(remoteId)
    }

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
