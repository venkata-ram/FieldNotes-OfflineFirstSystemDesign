package com.venkatsvision.offlinefirstsystemdesign.data.remote

import kotlinx.coroutines.flow.Flow

interface FakeNotesApi {
    val notes: Flow<List<RemoteNote>>

    suspend fun getNotes(): List<RemoteNote>
    suspend fun createNote(title: String, body: String, updatedAtMillis: Long): RemoteNote
    suspend fun updateNote(remoteId: String, title: String, body: String, updatedAtMillis: Long): RemoteNote
    suspend fun deleteNote(remoteId: String)
}
