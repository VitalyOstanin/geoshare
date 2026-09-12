package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which documents a reader subscribes to.
 *
 * The collection of positions cannot be listed — a permission with an expiry
 * is not something a query can prove — so the profiles are what name the
 * documents, and getting that list wrong means a member silently missing from
 * the map.
 */
class PositionDocumentsTest {
    private fun profile(memberId: String, vararg devices: String) = MemberProfile(
        memberId = memberId,
        displayName = memberId,
        pictureUrl = null,
        devices = devices.toList(),
    )

    @Test
    fun `every device of every member is read`() {
        val documents =
            positionDocuments(
                mapOf(
                    "alice" to profile("alice", "one", "two"),
                    "bob" to profile("bob", "phone"),
                ),
            )

        assertEquals(listOf("alice.one", "alice.two", "bob.phone"), documents)
    }

    // A member who has signed in but never written a position has no device in
    // their profile yet, and there is nothing to subscribe to for them.
    @Test
    fun `a member without devices contributes nothing`() {
        assertEquals(emptyList<String>(), positionDocuments(mapOf("alice" to profile("alice"))))
    }

    // The list drives a remember() in the screen, so the same group in a
    // different order has to come out the same and not rebuild every
    // subscription.
    @Test
    fun `the order does not depend on how the profiles arrived`() {
        val one = positionDocuments(mapOf("alice" to profile("alice", "two", "one")))
        val other = positionDocuments(mapOf("alice" to profile("alice", "one", "two")))

        assertEquals(one, other)
    }
}
