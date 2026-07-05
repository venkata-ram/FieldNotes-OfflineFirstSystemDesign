package com.venkatsvision.offlinefirstsystemdesign.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["remoteId"], unique = true),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val remoteId: String? = null,
    val title: String,
    val body: String,
    val syncStatus: String,
    val pendingOperation: String,
    val isDeleted: Boolean = false,
    val updatedAtMillis: Long,
)
