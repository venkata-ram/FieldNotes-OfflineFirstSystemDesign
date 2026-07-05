package com.venkatsvision.offlinefirstsystemdesign.domain

enum class SyncStatus(val label: String) {
    Synced("Synced"),
    PendingCreate("Pending create"),
    PendingUpdate("Pending update"),
    PendingDelete("Pending delete"),
    Failed("Sync failed"),
}
