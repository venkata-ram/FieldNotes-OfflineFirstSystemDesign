package com.venkatsvision.offlinefirstsystemdesign.data

import android.content.Context
import androidx.room.Room
import com.venkatsvision.offlinefirstsystemdesign.data.local.AppDatabase
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository

object AppContainer {
    @Volatile
    private var repository: NotesRepository? = null

    fun notesRepository(context: Context): NotesRepository =
        repository ?: synchronized(this) {
            repository ?: buildRepository(context.applicationContext).also {
                repository = it
            }
        }

    private fun buildRepository(context: Context): NotesRepository {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "field-notes.db",
        ).build()

        return RoomNotesRepository(database.noteDao())
    }
}
