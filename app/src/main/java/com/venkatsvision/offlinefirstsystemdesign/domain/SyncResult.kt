package com.venkatsvision.offlinefirstsystemdesign.domain

data class SyncResult(
    val pushed: Int,
    val pulled: Int,
    val failed: Int,
) {
    val message: String
        get() = if (failed == 0) {
            "Sync complete: pushed $pushed, pulled $pulled"
        } else {
            "Sync finished with $failed failure(s)"
        }
}
