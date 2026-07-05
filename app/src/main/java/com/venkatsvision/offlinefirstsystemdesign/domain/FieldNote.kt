package com.venkatsvision.offlinefirstsystemdesign.domain

data class FieldNote(
    val id: Long,
    val remoteId: String?,
    val title: String,
    val body: String,
    val syncStatus: SyncStatus,
    val pendingOperation: PendingOperation,
    val conflictTitle: String? = null,
    val conflictBody: String? = null,
)
