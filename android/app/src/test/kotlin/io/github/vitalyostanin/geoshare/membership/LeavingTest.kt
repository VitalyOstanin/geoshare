package io.github.vitalyostanin.geoshare.membership

import org.junit.Assert.assertEquals
import org.junit.Test

class LeavingTest {
    @Test
    fun `a member leaves the roster`() {
        val roster = Roster(members = listOf("a", "b"), admins = listOf("a"), founder = "a")

        assertEquals(Leave.ROSTER, howToLeave(roster, "b"))
    }

    @Test
    fun `the founder leaves while another administrator runs the group`() {
        val roster = Roster(members = listOf("a", "b"), admins = listOf("a", "b"), founder = "a")

        assertEquals(Leave.ROSTER, howToLeave(roster, "a"))
    }

    @Test
    fun `an administrator leaves while the founder still runs the group`() {
        val roster = Roster(members = listOf("a", "b"), admins = listOf("b"), founder = "a")

        assertEquals(Leave.ROSTER, howToLeave(roster, "b"))
    }

    @Test
    fun `the only administrator only drops the connection`() {
        val roster = Roster(members = listOf("a", "b"), admins = listOf("a"), founder = "a")

        assertEquals(Leave.DEVICE_ONLY, howToLeave(roster, "a"))
    }

    @Test
    fun `the last administrator of a group without its founder only drops the connection`() {
        val roster = Roster(members = listOf("b", "c"), admins = listOf("b"), founder = "a")

        assertEquals(Leave.DEVICE_ONLY, howToLeave(roster, "b"))
    }

    // The founder runs the group without being named among the
    // administrators; leaving would hand it to nobody, even though the rules
    // would let the write through.
    @Test
    fun `a founder running the group alone only drops the connection`() {
        val roster = Roster(members = listOf("a", "b"), admins = emptyList(), founder = "a")

        assertEquals(Leave.DEVICE_ONLY, howToLeave(roster, "a"))
    }

    @Test
    fun `a member of a group nobody runs leaves the roster`() {
        val roster = Roster(members = listOf("b", "c"), admins = emptyList(), founder = "a")

        assertEquals(Leave.ROSTER, howToLeave(roster, "b"))
    }

    @Test
    fun `somebody no longer on the roster only drops the connection`() {
        val roster = Roster(members = listOf("a"), admins = listOf("a"), founder = "a")

        assertEquals(Leave.NOT_ON_ROSTER, howToLeave(roster, "b"))
    }

    @Test
    fun `a group whose roster cannot be read only drops the connection`() {
        assertEquals(Leave.NOT_ON_ROSTER, howToLeave(null, "b"))
    }
}
