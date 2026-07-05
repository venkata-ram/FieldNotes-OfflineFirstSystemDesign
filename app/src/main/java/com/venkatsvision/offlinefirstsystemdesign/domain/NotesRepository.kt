package com.venkatsvision.offlinefirstsystemdesign.domain

import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    val notes: Flow<List<FieldNote>>
    val remoteNotes: Flow<List<RemoteFieldNote>>
    val syncLog: Flow<List<String>>

    suspend fun seedStarterNoteIfEmpty()
    suspend fun createNote(title: String, body: String)
    suspend fun updateNote(noteId: Long, title: String, body: String)
    suspend fun deleteNote(noteId: Long)
    suspend fun updateRemoteNote(remoteId: String, title: String, body: String)
    suspend fun resolveConflict(noteId: Long, resolution: ConflictResolution)
    suspend fun syncNow(): SyncResult
}
