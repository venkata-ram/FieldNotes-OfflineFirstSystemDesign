package com.venkatsvision.offlinefirstsystemdesign.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val remoteId: String? = null,
    val title: String,
    val body: String,
    val localLabel: String,
    val updatedAtMillis: Long,
)
