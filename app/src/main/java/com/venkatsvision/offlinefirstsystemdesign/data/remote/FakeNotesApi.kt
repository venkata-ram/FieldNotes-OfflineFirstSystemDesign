package com.venkatsvision.offlinefirstsystemdesign.data.remote

interface FakeNotesApi {
    suspend fun getNotes(): List<RemoteNote>
    suspend fun createNote(title: String, body: String, updatedAtMillis: Long): RemoteNote
    suspend fun updateNote(remoteId: String, title: String, body: String, updatedAtMillis: Long): RemoteNote
}
