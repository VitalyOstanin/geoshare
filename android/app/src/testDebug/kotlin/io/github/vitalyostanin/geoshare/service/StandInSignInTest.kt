package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Development only: the sign-in from the shell against the stand-in services (ADR-0044).
class StandInSignInTest {
    @Test
    fun `a sign-in that names nobody signs in the first of the household`() {
        assertEquals("parent@geoshare.test", standInAccountFor(null))
        assertEquals("parent@geoshare.test", standInAccountFor(""))
    }

    @Test
    fun `a sign-in that names a stand-in account signs in that one`() {
        assertEquals("newcomer@geoshare.test", standInAccountFor("newcomer@geoshare.test"))
    }

    @Test
    fun `a sign-in that names an address the seed never made is refused`() {
        assertNull(standInAccountFor("someone@example.com"))
    }
}
