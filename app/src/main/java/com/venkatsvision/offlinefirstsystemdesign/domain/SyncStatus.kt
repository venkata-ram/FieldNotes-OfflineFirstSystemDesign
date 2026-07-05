package com.venkatsvision.offlinefirstsystemdesign.domain

enum class SyncStatus(val label: String) {
    Synced("Synced"),
    PendingCreate("Pending create"),
    PendingUpdate("Pending update"),
    Failed("Sync failed"),
}
