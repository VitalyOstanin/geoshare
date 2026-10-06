package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.membership.Roster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GroupLineTest {
    private val parent = "uid-parent"
    private val spouse = "uid-spouse"
    private val child = "uid-child"

    private val roster =
        Roster(members = listOf(spouse, child, parent), admins = listOf(parent))

    private val profiles =
        mapOf(
            parent to MemberProfile(memberId = parent, displayName = "Parent", pictureUrl = null),
            child to MemberProfile(memberId = child, displayName = "Child", pictureUrl = null),
        )

    // The panel is read by somebody checking on the others, and the first
    // question a list of names raises is which of them is this phone.
    @Test
    fun `the person looking is first`() {
        val lines = groupOverview(roster, profiles, emptyMap(), ownMemberId = child)

        assertEquals(child, lines.first().memberId)
        assertTrue(lines.first().isYou)
    }

    // An order following the roster would follow the order people happened to
    // join in, which is the order nobody can predict.
    @Test
    fun `the rest follow the name they are shown under`() {
        val lines = groupOverview(roster, profiles, emptyMap(), ownMemberId = child)

        assertEquals(listOf(child, parent, spouse), lines.map { it.memberId })
    }

    @Test
    fun `a member who published no name is ordered by the id shown in its place`() {
        val lines = groupOverview(roster, profiles, emptyMap(), ownMemberId = parent)

        assertEquals(null, lines.last().displayName)
        assertEquals(spouse, lines.last().memberId)
    }

    @Test
    fun `the one running the group is marked`() {
        val lines = groupOverview(roster, profiles, emptyMap(), ownMemberId = child)

        assertTrue(lines.single { it.memberId == parent }.runsTheGroup)
        assertFalse(lines.single { it.memberId == spouse }.runsTheGroup)
    }

    // What the panel counts is what this phone may watch, not what the member
    // owns: a grant nobody may read says nothing about how many phones there
    // are.
    @Test
    fun `the phones counted are the ones this phone was given`() {
        val grants =
            mapOf(
                child to
                    MemberGrant(
                        memberId = child,
                        devices = listOf("one", "two"),
                        sharedWith = emptyMap(),
                    ),
            )
        val lines = groupOverview(roster, profiles, grants, ownMemberId = parent)

        assertEquals(2, lines.single { it.memberId == child }.phonesShared)
        assertEquals(0, lines.single { it.memberId == spouse }.phonesShared)
    }

    // Leaving a group is not the same act as taking somebody out of it, and a
    // row offering both in one place is how a tap in the wrong place empties a
    // group.
    @Test
    fun `nobody is offered their own removal`() {
        val lines = groupOverview(roster, profiles, emptyMap(), ownMemberId = parent)

        assertFalse(lines.single { it.isYou }.removable)
        assertTrue(lines.single { it.memberId == child }.removable)
    }

    // The rules refuse a roster left with nobody to run it, so the panel must
    // not offer the write that would make one.
    @Test
    fun `the last administrator is not offered for removal`() {
        val alone = Roster(members = listOf(parent, child), admins = listOf(parent))
        val lines = groupOverview(alone, profiles, emptyMap(), ownMemberId = child)

        assertFalse(lines.single { it.memberId == parent }.removable)
    }

    // An administrator reads the phones without the grant, which is the case
    // the panel lists them for: whether a phone is in touch is not a secret
    // of whom its owner shares with.
    @Test
    fun `the panel offers the role to a member and takes it from an administrator`() {
        val two = Roster(members = listOf(parent, spouse, child), admins = listOf(parent, spouse))
        val lines = groupOverview(two, profiles, emptyMap(), parent)

        assertTrue(lines.single { it.memberId == child }.mayBeMadeAdmin)
        assertFalse(lines.single { it.memberId == child }.mayStopRunning)
        assertFalse(lines.single { it.memberId == spouse }.mayBeMadeAdmin)
        assertTrue(lines.single { it.memberId == spouse }.mayStopRunning)
        assertTrue(lines.single { it.memberId == parent }.mayStopRunning)
    }

    @Test
    fun `the phones are listed by the names the map gives them`() {
        val roster = Roster(members = listOf(parent, child), admins = listOf(parent))
        val grants =
            combinedGrants(
                named = emptyMap(),
                phones =
                mapOf(
                    child to
                        mapOf(
                            "old" to
                                PhoneStatus(model = "MAR-LX1", onlineAt = Instant.ofEpochSecond(5)),
                            "new" to PhoneStatus(model = "MAR-LX1", name = "school phone"),
                        ),
                ),
            )
        val labels = Labels(phones = mapOf("$child.old" to "the old one"))

        val lines = groupOverview(roster, profiles, grants, ownMemberId = parent, labels)
        val phones = lines.single { it.memberId == child }.phones

        assertEquals(listOf("school phone", "the old one"), phones.map { it.name })
        assertEquals(Instant.ofEpochSecond(5), phones.last().onlineAt)
    }

    // The state is written every so often and the position far more often, so
    // the panel has to say what the map says: the later of the two.
    @Test
    fun `a phone's age is the later of its state and its position`() {
        val id = "uid-parent.phone"
        val state = Instant.ofEpochSecond(100)
        val newer = Instant.ofEpochSecond(900)
        val older = Instant.ofEpochSecond(50)
        val phones = mapOf("phone" to PhoneStatus(name = "phone", onlineAt = state))

        fun seen(at: Instant?) =
            phoneLines(parent, phones, Labels(), mapOfNotNull(id, at)).single().onlineAt

        assertEquals(newer, seen(newer))
        assertEquals(state, seen(older))
        assertEquals(state, seen(null))
    }

    @Test
    fun `a phone with no state is as old as its position`() {
        val id = "uid-parent.phone"
        val at = Instant.ofEpochSecond(900)
        val phones = mapOf("phone" to PhoneStatus(name = "phone"))

        val line = phoneLines(parent, phones, Labels(), mapOf(id to at)).single()

        assertEquals(at, line.onlineAt)
    }

    private fun mapOfNotNull(id: String, at: Instant?): Map<String, Instant> =
        if (at == null) emptyMap() else mapOf(id to at)
}
