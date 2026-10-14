package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MemberOrderTest {
    private fun positionOf(
        memberId: String,
        device: String = "phone",
        takenAt: Instant = Instant.parse("2026-09-11T12:00:00Z"),
    ): MemberPosition = MemberPosition(
        positionId = "$memberId.$device",
        memberId = memberId,
        deviceName = null,
        latitude = 61.0,
        longitude = 4.0,
        takenAt = takenAt,
        writtenAt = null,
        mode = null,
        batteryPercent = null,
    )

    private fun idsOf(order: List<MemberPosition>): List<String> = order.map { it.memberId }

    @Test
    fun `the other members come before our own entry`() {
        val order =
            orderedForNavigation(listOf(positionOf("me"), positionOf("her")), ownMemberId = "me")

        assertEquals(listOf("her", "me"), idsOf(order))
    }

    @Test
    fun `the others keep the same places whatever order they arrive in`() {
        val members = listOf(positionOf("c"), positionOf("a"), positionOf("b"))

        assertEquals(
            listOf("a", "b", "c"),
            idsOf(orderedForNavigation(members, ownMemberId = "me")),
        )
        assertEquals(
            idsOf(orderedForNavigation(members, ownMemberId = "me")),
            idsOf(orderedForNavigation(members.reversed(), ownMemberId = "me")),
        )
    }

    @Test
    fun `a list of only ourselves is left as it is`() {
        assertEquals(
            listOf("me"),
            idsOf(orderedForNavigation(listOf(positionOf("me")), ownMemberId = "me")),
        )
    }

    // The same four cases the web page checks, so that a member is called the
    // same thing on the device and on the laptop.
    @Test
    fun `a member is called what their profile says`() {
        val profile =
            MemberProfile(memberId = "her", displayName = "Ann", pictureUrl = null)

        assertEquals("Ann", memberName(profile, "her", own = false, ownName = "You"))
    }

    @Test
    fun `a member without a profile is called by the start of their id`() {
        assertEquals(
            "Qm7d0K",
            memberName(null, "Qm7d0Kh8oOOL8dKLzdocJ2isAjIh", own = false, ownName = "You"),
        )
    }

    @Test
    fun `our own marker says what the screen calls us, profile or not`() {
        val profile =
            MemberProfile(memberId = "me", displayName = "Ann", pictureUrl = null)

        assertEquals("You", memberName(profile, "me", own = true, ownName = "You"))
        assertEquals("You", memberName(null, "me", own = true, ownName = "You"))
    }

    // The same rule the web page follows: a member carrying two devices has
    // them next to each other, in the same places at every glance.
    @Test
    fun `the devices of one member keep their places`() {
        val members =
            listOf(positionOf("a", "two"), positionOf("b"), positionOf("a", "one"))

        assertEquals(
            listOf("a.one", "a.two", "b.phone"),
            orderedForNavigation(members, ownMemberId = "me").map { it.positionId },
        )
        assertEquals(
            orderedForNavigation(members, ownMemberId = "me").map { it.positionId },
            orderedForNavigation(members.reversed(), ownMemberId = "me").map { it.positionId },
        )
    }

    @Test
    fun `a panel with room to spare shows everybody`() {
        assertEquals(1, membersOnTheStrip(1))
        assertEquals(2, membersOnTheStrip(2))
    }

    @Test
    fun `the last member is shown rather than a button holding them`() {
        assertEquals(3, membersOnTheStrip(3))
    }

    @Test
    fun `a line of text names a member without a name the way it is given`() {
        val id = "Qm7d0Kh8oOOL8dKLzdocJ2isAjIh"

        assertEquals(
            "Member without a name · ${idTail(id)}",
            memberName(
                null,
                id,
                own = false,
                ownName = "You",
                unnamed = "Member without a name · ${idTail(id)}",
            ),
        )
        assertEquals("Qm7d", idTail(id))
    }

    @Test
    fun `a published name wins over what a member without one is called`() {
        val profile = MemberProfile(memberId = "her", displayName = "Ann", pictureUrl = null)

        assertEquals(
            "Ann",
            memberName(profile, "her", own = false, ownName = "You", unnamed = "nobody"),
        )
    }

    @Test
    fun `a longer roster keeps the button for the rest`() {
        assertEquals(2, membersOnTheStrip(4))
        assertEquals(2, membersOnTheStrip(9))
    }

    @Test
    fun `the map opens on our own device last heard from`() {
        val old = positionOf("me", "old", Instant.parse("2026-09-11T11:00:00Z"))
        val new = positionOf("me", "new", Instant.parse("2026-09-11T12:00:00Z"))
        val members = listOf(positionOf("her"), old, new)

        assertEquals("me.new", openingPosition(members, ownMemberId = "me")?.positionId)
        assertEquals("her.phone", openingPosition(listOf(positionOf("her")), "me")?.positionId)
        assertEquals(null, openingPosition(emptyList(), "me"))
    }
}
