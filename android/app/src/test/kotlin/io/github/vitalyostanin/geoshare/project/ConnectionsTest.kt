package io.github.vitalyostanin.geoshare.project

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What happens to a kept list of groups that cannot be read. */
class ConnectionsTest {
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
        every { putString(any(), any()) } returns this
        every { commit() } returns true
    }

    private fun deviceKeeping(stored: String, setAside: Boolean = false): Context {
        val files = mockk<SharedPreferences> {
            every { getString("list", null) } returns stored
            every { contains(KEY_UNREADABLE_LIST) } returns setAside
            every { edit() } returns editor
        }

        return mockk { every { getSharedPreferences(any(), any()) } returns files }
    }

    @Test
    fun `an unreadable list is set aside before anything overwrites it`() {
        Connections.start(deviceKeeping("{broken"))

        verify { editor.putString(KEY_UNREADABLE_LIST, "{broken") }
        verify(exactly = 0) { editor.putString("list", any()) }
    }

    // The first unreadable list is the one the groups were in.
    @Test
    fun `a list already set aside is not replaced by a later one`() {
        Connections.start(deviceKeeping("{later", setAside = true))

        verify(exactly = 0) { editor.putString(KEY_UNREADABLE_LIST, any()) }
    }

    // The files of a device, remembering what is written to them.
    private fun deviceWithFiles(vararg kept: String): Context {
        val keys = kept.toMutableSet()
        val fileEditor = mockk<SharedPreferences.Editor> {
            every { putBoolean(any(), any()) } answers {
                keys += firstArg<String>()
                this@mockk
            }
            every { commit() } returns true
        }
        val files = mockk<SharedPreferences> {
            every { contains(any()) } answers { firstArg<String>() in keys }
            every { edit() } returns fileEditor
        }

        return mockk { every { getSharedPreferences(any(), any()) } returns files }
    }

    @Test
    fun `a list set aside is told apart from a device that never had one`() {
        assertTrue(Connections.unreadableListKept(deviceWithFiles(KEY_UNREADABLE_LIST)))
        assertFalse(Connections.unreadableListKept(deviceWithFiles()))
    }

    // The notice belongs to the empty start after the damage, not to every
    // later time the device has no group.
    @Test
    fun `joining a group ends the notice for good while the list stays set aside`() {
        val device = deviceWithFiles(KEY_UNREADABLE_LIST)

        assertTrue(Connections.unreadableListKept(device))

        Connections.markJoinedAfterUnreadable(device)

        assertFalse(Connections.unreadableListKept(device))
        assertTrue(Connections.files(device).contains(KEY_UNREADABLE_LIST))

        // Leaving every group writes no mark and takes none away.
        Connections.markJoinedAfterUnreadable(device)

        assertFalse(Connections.unreadableListKept(device))
    }

    @Test
    fun `a device without a set aside list keeps no mark when it joins`() {
        val device = deviceWithFiles()

        Connections.markJoinedAfterUnreadable(device)

        assertFalse(Connections.files(device).contains(KEY_JOINED_AFTER_UNREADABLE))
    }
}
