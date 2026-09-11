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
}
