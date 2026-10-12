package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which of the system's answers are taken as the id of the device. */
class SystemIdTest {
    @Test
    fun `the id the system gives is taken as it is`() {
        assertEquals("9f3c21a07b44e5d1", systemIdFrom("9f3c21a07b44e5d1"))
    }

    @Test
    fun `no id and an empty one fall back on a made one`() {
        assertNull(systemIdFrom(null))
        assertNull(systemIdFrom(""))
    }

    // A dot would move part of the id into the owner's half of the document id.
    @Test
    fun `an id that could split a document id is not taken`() {
        assertNull(systemIdFrom("owner.phone"))
        assertNull(systemIdFrom("a/b"))
    }
}
