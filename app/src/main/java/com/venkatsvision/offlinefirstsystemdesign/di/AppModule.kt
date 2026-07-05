package com.venkatsvision.offlinefirstsystemdesign.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.venkatsvision.offlinefirstsystemdesign.data.RoomNotesRepository
import com.venkatsvision.offlinefirstsystemdesign.data.local.AppDatabase
import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteDao
import com.venkatsvision.offlinefirstsystemdesign.data.remote.FakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.data.remote.InMemoryFakeNotesApi
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "field-notes.db",
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao =
        database.noteDao()

    @Provides
    @Singleton
    fun provideFakeNotesApi(): FakeNotesApi =
        InMemoryFakeNotesApi()

    @Provides
    @Singleton
    fun provideNotesRepository(
        noteDao: NoteDao,
        notesApi: FakeNotesApi,
    ): NotesRepository =
        RoomNotesRepository(
            noteDao = noteDao,
            notesApi = notesApi,
        )

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager =
        WorkManager.getInstance(context)
}
