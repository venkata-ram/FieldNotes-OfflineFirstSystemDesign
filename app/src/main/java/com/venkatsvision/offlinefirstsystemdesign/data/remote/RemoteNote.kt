package com.venkatsvision.offlinefirstsystemdesign.data.remote

data class RemoteNote(
    val remoteId: String,
    val title: String,
    val body: String,
    val updatedAtMillis: Long,
)
