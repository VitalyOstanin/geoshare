package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

private const val FAMILY = "family"
private const val OTHER = "alice-uid"
private const val ALICE = "alice-uid"
private const val PHONE = "alice-phone"

private fun told(group: String, displayName: String = "Alice") = ProfileTold(
    group = group,
    memberId = ALICE,
    displayName = displayName,
    pictureUrl = null,
    deviceId = PHONE,
)

class ProfileToldTest {
    // The phone writes a profile only when what it holds differs from what the
    // account says, so the group has to be part of what it holds: a member who
    // joined another group would otherwise stay nameless there, the phone
    // believing it had already introduced them.
    @Test
    fun `what one group was told is not what another was told`() {
        assertNotEquals(told(FAMILY), told(OTHER))
    }

    @Test
    fun `the same thing said to the same group has already been said`() {
        assertEquals(told(FAMILY), told(FAMILY))
    }

    @Test
    fun `a member who changed their name has something new to say`() {
        assertNotEquals(told(FAMILY), told(FAMILY, displayName = "Alice B"))
    }
}
