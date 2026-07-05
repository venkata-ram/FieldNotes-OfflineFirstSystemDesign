package com.venkatsvision.offlinefirstsystemdesign.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
        assertTrue(created.remoteId.startsWith("remote-"))
        assertEquals(listOf(created), notes)
    }

    @Test
    fun createNote_afterApiRecreation_doesNotReusePreviousRemoteId() = runTest {
        val firstApiInstance = InMemoryFakeNotesApi(delayMillis = 0)
        val previous = firstApiInstance.createNote("Before restart", "Body", 1000L)
        val secondApiInstance = InMemoryFakeNotesApi(delayMillis = 0)

        val next = secondApiInstance.createNote("After restart", "Body", 1000L)

        assertNotEquals(previous.remoteId, next.remoteId)
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
