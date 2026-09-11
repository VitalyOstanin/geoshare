package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MemberOrderTest {
    private fun positionOf(memberId: String): MemberPosition = MemberPosition(
        memberId = memberId,
        latitude = 61.0,
        longitude = 4.0,
        takenAt = Instant.parse("2026-09-11T12:00:00Z"),
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
    // same thing on the phone and on the laptop.
    @Test
    fun `a member is called what their profile says`() {
        val profile =
            MemberProfile(memberId = "her", displayName = "Ann", pictureUrl = null)

        assertEquals("Ann", memberName(profile, "her", own = false, ownName = "You"))
    }

    @Test
    fun `a member without a profile is called by the start of their id`() {
        assertEquals(
            "E9WyZT",
            memberName(null, "u7ExampleOwnerUid00000000000", own = false, ownName = "You"),
        )
    }

    @Test
    fun `our own marker says what the screen calls us, profile or not`() {
        val profile =
            MemberProfile(memberId = "me", displayName = "Ann", pictureUrl = null)

        assertEquals("You", memberName(profile, "me", own = true, ownName = "You"))
        assertEquals("You", memberName(null, "me", own = true, ownName = "You"))
    }
}
