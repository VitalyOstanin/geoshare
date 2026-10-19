package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.membership.Roster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupCaptionTest {
    private val pattern = "%1\$s's group"

    @Test
    fun `a named group shows its own name`() {
        val roster = Roster(founder = "anna", name = "Hiking party")

        assertEquals("Hiking party", groupCaption(roster, emptyMap(), pattern))
    }

    @Test
    fun `a group with no name is called after the founder's profile`() {
        val roster = Roster(founder = "anna")
        val profiles = mapOf("anna" to MemberProfile("anna", "Anna", null))

        assertEquals("Anna's group", groupCaption(roster, profiles, pattern))
    }

    @Test
    fun `a founder with no profile is called by the short form of their id`() {
        val roster = Roster(founder = "anna1234567890")

        assertEquals("anna12's group", groupCaption(roster, emptyMap(), pattern))
    }

    @Test
    fun `a name that is only spaces is treated as no name`() {
        val roster = Roster(founder = "anna", name = "   ")
        val profiles = mapOf("anna" to MemberProfile("anna", "Anna", null))

        assertEquals("Anna's group", groupCaption(roster, profiles, pattern))
    }

    @Test
    fun `a field left blank founds a group with no name`() {
        assertNull(groupNameText(""))
        assertNull(groupNameText("   "))
    }

    @Test
    fun `a typed name is trimmed and held to the rules' limit`() {
        val typed = " ${"a".repeat(70)} "

        assertEquals("a".repeat(GROUP_NAME_LIMIT), groupNameText(typed))
    }

    // An emoji across the limit must go whole: its first half alone is not
    // text, and the document would get a replacement character in its place.
    @Test
    fun `a name is not cut between the halves of an emoji`() {
        val typed = "a".repeat(GROUP_NAME_LIMIT - 1) + "\uD83D\uDE00"

        assertEquals("a".repeat(GROUP_NAME_LIMIT - 1), groupNameText(typed))
    }
}
