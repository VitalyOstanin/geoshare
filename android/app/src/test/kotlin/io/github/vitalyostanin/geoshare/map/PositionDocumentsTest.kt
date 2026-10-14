package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which documents a reader subscribes to.
 *
 * The collection of positions cannot be listed — a permission with an expiry
 * is not something a query can prove — so the grants are what name the
 * documents, and getting that list wrong means a member silently missing from
 * the map.
 */
class PositionDocumentsTest {
    private fun grant(memberId: String, vararg devices: String) = MemberGrant(
        memberId = memberId,
        devices = devices.toList(),
    )

    @Test
    fun `every device of every member is read`() {
        val documents =
            positionDocuments(
                mapOf(
                    "alice" to grant("alice", "one", "two"),
                    "bob" to grant("bob", "phone"),
                ),
            )

        assertEquals(listOf("alice.one", "alice.two", "bob.phone"), documents)
    }

    // A member who has signed in but never written a position has no device in
    // their grant yet, and there is nothing to subscribe to for them.
    @Test
    fun `a member without devices contributes nothing`() {
        assertEquals(emptyList<String>(), positionDocuments(mapOf("alice" to grant("alice"))))
    }

    // The list drives a remember() in the screen, so the same group in a
    // different order has to come out the same and not rebuild every
    // subscription.
    @Test
    fun `the order does not depend on how the grants arrived`() {
        val one = positionDocuments(mapOf("alice" to grant("alice", "two", "one")))
        val other = positionDocuments(mapOf("alice" to grant("alice", "one", "two")))

        assertEquals(one, other)
    }

    // A device under observation: signed in, positions in hand, and a screen
    // saying nobody shares — its own grant was not written yet.
    @Test
    fun `this device's own document is followed before its grant names it`() {
        assertEquals(listOf("me.phone"), positionDocuments(emptyMap(), own = "me.phone"))
    }

    @Test
    fun `this device's own document is followed once when its grant names it too`() {
        val documents = positionDocuments(mapOf("me" to grant("me", "phone")), own = "me.phone")

        assertEquals(listOf("me.phone"), documents)
    }
}
