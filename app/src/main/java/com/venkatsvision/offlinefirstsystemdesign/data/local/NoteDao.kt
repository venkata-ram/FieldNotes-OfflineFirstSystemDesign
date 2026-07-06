package com.venkatsvision.offlinefirstsystemdesign.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAtMillis DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE localId = :localId")
    suspend fun getNote(localId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getNoteByRemoteId(remoteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE pendingOperation != 'None' AND syncStatus != 'Conflict' ORDER BY updatedAtMillis ASC")
    suspend fun getPendingNotes(): List<NoteEntity>

    @Query("DELETE FROM notes WHERE localId = :localId")
    suspend fun hardDelete(localId: Long)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countNotes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)
}
