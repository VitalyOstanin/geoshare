package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FirstShareTest {
    private val forever = Instant.parse("9999-12-31T00:00:00Z")

    @Test
    fun `a confirmed grant with nobody in it asks`() {
        assertEquals(true, sharesNothing(pendingWrites = false, permissions = emptyMap()))
    }

    @Test
    fun `a confirmed grant with readers does not ask`() {
        assertEquals(
            false,
            sharesNothing(pendingWrites = false, permissions = mapOf("mark" to forever)),
        )
    }

    // Signing in adds the device to the grant; until the server confirms that
    // write the read can hold it alone, with no permissions, and a member who
    // shares with the whole group would be asked whom to share with.
    @Test
    fun `a read carrying an unconfirmed write says nothing`() {
        assertNull(sharesNothing(pendingWrites = true, permissions = emptyMap()))
        assertNull(sharesNothing(pendingWrites = true, permissions = mapOf("mark" to forever)))
    }
}
