package io.github.vitalyostanin.geoshare.service

import android.content.SharedPreferences
import io.github.vitalyostanin.geoshare.log.AppLog
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** What a settings file holding the wrong type under a key does to the one reading it. */
class StoredReadsTest {
    private val wrongType = ClassCastException("not of that type")
    private var loggedBefore = true

    @Before
    fun keepOutOfTheSystemLog() {
        loggedBefore = AppLog.toSystemLog
        AppLog.toSystemLog = false
    }

    @After
    fun restoreTheLog() {
        AppLog.toSystemLog = loggedBefore
    }

    @Test
    fun `a value of the right type is read as it is`() {
        val store = mockk<SharedPreferences>()
        every { store.getBoolean("flag", false) } returns true
        every { store.getLong("stamp", -1) } returns 42L
        every { store.getString("name", null) } returns "kept"
        every { store.getStringSet("names", null) } returns setOf("a", "b")

        assertEquals(true, store.readBoolean("flag", false))
        assertEquals(42L, store.readLong("stamp", -1))
        assertEquals("kept", store.readString("name", null))
        assertEquals(setOf("a", "b"), store.readStringSet("names", null))
    }

    @Test
    fun `a value of another type reads as the default`() {
        val store = mockk<SharedPreferences>()
        every { store.getBoolean("flag", true) } throws wrongType
        every { store.getLong("stamp", -1) } throws wrongType
        every { store.getString("name", "none") } throws wrongType
        every { store.getStringSet("names", emptySet()) } throws wrongType

        assertEquals(true, store.readBoolean("flag", true))
        assertEquals(-1L, store.readLong("stamp", -1))
        assertEquals("none", store.readString("name", "none"))
        assertEquals(emptySet<String>(), store.readStringSet("names", emptySet()))
    }

    @Test
    fun `a null default survives a value of another type`() {
        val store = mockk<SharedPreferences>()
        every { store.getString("name", null) } throws wrongType
        every { store.getStringSet("names", null) } throws wrongType

        assertNull(store.readString("name", null))
        assertNull(store.readStringSet("names", null))
    }
}
