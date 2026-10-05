package io.github.vitalyostanin.geoshare.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class SignInMarkTest {
    // The page broke and the screen was made again: say the sign-in did not finish.
    @Test
    fun `a screen after an unended sign-in opens as unfinished`() {
        assertEquals(SignInFailure.Cancelled, signedOutAfter(startedEarlier = true).failure)
    }

    @Test
    fun `a screen with nothing tried opens with nothing to say`() {
        assertEquals(null, signedOutAfter(startedEarlier = false).failure)
    }
}
