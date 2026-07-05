package com.venkatsvision.offlinefirstsystemdesign.data

import android.content.Context
import androidx.room.Room
import com.venkatsvision.offlinefirstsystemdesign.data.local.AppDatabase
import com.venkatsvision.offlinefirstsystemdesign.data.remote.FakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.data.remote.InMemoryFakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.data.sync.NotesSyncScheduler
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository

object AppContainer {
    @Volatile
    private var repository: NotesRepository? = null
    private val fakeNotesApi: FakeNotesApi = InMemoryFakeNotesApi()

    fun notesRepository(context: Context): NotesRepository =
        repository ?: synchronized(this) {
            repository ?: buildRepository(context.applicationContext).also {
                repository = it
            }
        }

    fun notesSyncScheduler(context: Context): NotesSyncScheduler =
        NotesSyncScheduler(context.applicationContext)

    private fun buildRepository(context: Context): NotesRepository {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "field-notes.db",
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        return RoomNotesRepository(
            noteDao = database.noteDao(),
            notesApi = fakeNotesApi,
        )
    }
}
