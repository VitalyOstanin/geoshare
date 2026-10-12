package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountPictureTest {
    @Test
    fun `a picture the sign-in provider keeps is shown`() {
        val address = "https://lh3.googleusercontent.com/a/ACg8ocK=s96-c"

        assertEquals(address, accountPicture(address))
    }

    @Test
    fun `a picture anywhere else is not fetched`() {
        assertNull(accountPicture("https://example.com/me.png"))
        assertNull(accountPicture("http://lh3.googleusercontent.com/a/x"))
        assertNull(accountPicture("https://example.com/lh3.googleusercontent.com/a/x"))
        assertNull(accountPicture("https://googleusercontent.com.example.com/a/x"))
        assertNull(accountPicture("https://x@example.com/.googleusercontent.com/a"))
        assertNull(accountPicture("https://colab.googleusercontent.com/a/x"))
    }

    @Test
    fun `no address is no picture`() {
        assertNull(accountPicture(null))
        assertNull(accountPicture(""))
    }
}
