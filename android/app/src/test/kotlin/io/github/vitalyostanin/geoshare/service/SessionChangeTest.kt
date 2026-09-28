package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the service does when the account on the phone changes: the phone
 * handed to somebody else keeps its id, and only this tells its collection to
 * write under the new owner's name.
 */
class SessionChangeTest {
    @Test
    fun `an account turning up starts the collection`() {
        assertEquals(SessionChange.Start, sessionChange(collectingFor = null, signedIn = "anna"))
    }

    @Test
    fun `the same account heard again changes nothing`() {
        assertEquals(SessionChange.None, sessionChange(collectingFor = "anna", signedIn = "anna"))
    }

    @Test
    fun `nobody signed in and nothing collecting changes nothing`() {
        assertEquals(SessionChange.None, sessionChange(collectingFor = null, signedIn = null))
    }

    // Left running, the collection would write under the old name with the
    // new session, and every write would be refused.
    @Test
    fun `signing out stops the collection`() {
        assertEquals(SessionChange.Stop, sessionChange(collectingFor = "anna", signedIn = null))
    }

    @Test
    fun `another account restarts the collection for it`() {
        assertEquals(
            SessionChange.Restart,
            sessionChange(collectingFor = "anna", signedIn = "granny"),
        )
    }
}
