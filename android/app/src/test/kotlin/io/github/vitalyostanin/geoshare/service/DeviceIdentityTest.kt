package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which id the device goes by, and what it is told of the one it went by before. */
class DeviceIdentityTest {
    private val stored = mutableMapOf<String, String>()

    @After
    fun forgetTheSystem() {
        unmockkStatic(Settings.Secure::class)
    }

    @Test
    fun `the system's id is the device's, and there is nothing before it`() {
        val identity = identity(system = "9f3c21a07b44e5d1")

        assertEquals("9f3c21a07b44e5d1", identity.id)
        assertNull(identity.previousId)
        assertEquals(emptyMap<String, String>(), stored)
    }

    @Test
    fun `without the system's id a made one is kept and given each time`() {
        val identity = identity(system = null)
        val first = identity.id

        assertEquals(first, identity.id)
        assertEquals(first, stored["id"])
        assertNull(identity.previousId)
    }

    @Test
    fun `an id the system gives with a dot in it is not taken`() {
        val identity = identity(system = "owner.phone")

        assertNotEquals("owner.phone", identity.id)
        assertEquals(stored["id"], identity.id)
    }

    @Test
    fun `a made id from before the system's is told until it is forgotten`() {
        stored["id"] = "1b4e28ba-2fa1-11d2-883f-0016d3cca427"
        val identity = identity(system = "9f3c21a07b44e5d1")

        assertEquals("1b4e28ba-2fa1-11d2-883f-0016d3cca427", identity.previousId)

        identity.forgetPrevious("1b4e28ba-2fa1-11d2-883f-0016d3cca427")

        assertNull(identity.previousId)
        assertEquals(emptyMap<String, String>(), stored)
    }

    @Test
    fun `forgetting an id the device did not go by keeps the one it did`() {
        stored["id"] = "1b4e28ba-2fa1-11d2-883f-0016d3cca427"
        val identity = identity(system = "9f3c21a07b44e5d1")

        identity.forgetPrevious("an id from somewhere else")

        assertEquals("1b4e28ba-2fa1-11d2-883f-0016d3cca427", identity.previousId)
    }

    private fun identity(system: String?): DeviceIdentity {
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns system

        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.contentResolver } returns mockk()
        every { context.getSharedPreferences("device", Context.MODE_PRIVATE) } returns preferences()

        return DeviceIdentity(context)
    }

    /** Preferences kept in [stored], so a test can see what was written and set what was there. */
    private fun preferences(): SharedPreferences {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.putString(any(), any()) } answers {
            stored[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            stored.remove(firstArg<String>())
            editor
        }
        every { editor.apply() } just Runs

        return mockk {
            every { getString(any(), any()) } answers { stored[firstArg()] ?: secondArg() }
            every { edit() } returns editor
        }
    }
}
