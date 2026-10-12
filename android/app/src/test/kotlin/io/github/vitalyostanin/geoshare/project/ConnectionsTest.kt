package io.github.vitalyostanin.geoshare.project

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
}
