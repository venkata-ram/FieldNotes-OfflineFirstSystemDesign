package com.venkatsvision.offlinefirstsystemdesign.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class InMemoryFakeNotesApiTest {
    @Test
    fun createNote_storesRemoteNote() = runTest {
        val api = InMemoryFakeNotesApi(delayMillis = 0, clock = { 1000L })

        val created = api.createNote(
            title = "Remote draft",
            body = "Created by fake API",
            updatedAtMillis = 900L,
        )

        val notes = api.getNotes()
        assertEquals("remote-1", created.remoteId)
        assertEquals(listOf(created), notes)
    }

    @Test
    fun updateNote_replacesRemoteNote() = runTest {
        val api = InMemoryFakeNotesApi(delayMillis = 0, clock = { 1000L })
        val created = api.createNote("Original", "Body", 900L)

        val updated = api.updateNote(created.remoteId, "Updated", "New body", 1100L)

        val notes = api.getNotes()
        assertEquals("Updated", updated.title)
        assertEquals(listOf(updated), notes)
    }

    @Test
    fun failNextRequest_failsOnce() = runTest {
        val api = InMemoryFakeNotesApi(delayMillis = 0)

        api.failNextRequest = true

        try {
            api.getNotes()
            fail("Expected fake network failure")
        } catch (expected: IllegalStateException) {
            assertEquals("Fake network failure", expected.message)
        }
        assertEquals(emptyList<RemoteNote>(), api.getNotes())
    }

    @Test
    fun deleteNote_removesRemoteNote() = runTest {
        val api = InMemoryFakeNotesApi(delayMillis = 0)
        val created = api.createNote("Delete me", "Body", 1000L)

        api.deleteNote(created.remoteId)

        assertEquals(emptyList<RemoteNote>(), api.getNotes())
    }
}
