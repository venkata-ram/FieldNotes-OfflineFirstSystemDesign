package com.venkatsvision.offlinefirstsystemdesign.domain

import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    val notes: Flow<List<FieldNote>>

    suspend fun seedStarterNoteIfEmpty()
    suspend fun createNote(title: String, body: String)
    suspend fun updateNote(noteId: Long, title: String, body: String)
    suspend fun deleteNote(noteId: Long)
    suspend fun syncNow(): SyncResult
}
