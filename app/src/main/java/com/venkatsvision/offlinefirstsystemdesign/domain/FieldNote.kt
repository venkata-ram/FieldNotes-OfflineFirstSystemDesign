package com.venkatsvision.offlinefirstsystemdesign.domain

data class FieldNote(
    val id: Long,
    val title: String,
    val body: String,
    val syncStatus: SyncStatus,
    val pendingOperation: PendingOperation,
)
