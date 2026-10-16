package io.github.vitalyostanin.geoshare.membership

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMarkTest {
    @Test
    fun `a mark naming a group says the project is taken`() {
        val mark = projectMarkOf(exists = true, group = "alice-uid")

        assertEquals(ProjectMark.Taken("alice-uid"), mark)
        assertTrue(mark.isTaken())
    }

    @Test
    fun `no mark document leaves the project free`() {
        val mark = projectMarkOf(exists = false, group = null)

        assertEquals(ProjectMark.Free, mark)
        assertFalse(mark.isTaken())
    }

    @Test
    fun `a mark with no usable id reads as unknown and offers a group as free does`() {
        assertEquals(ProjectMark.Unknown, projectMarkOf(exists = true, group = null))
        assertEquals(ProjectMark.Unknown, projectMarkOf(exists = true, group = ""))
        assertEquals(ProjectMark.Unknown, projectMarkOf(exists = true, group = 7))
        assertFalse(ProjectMark.Unknown.isTaken())
    }

    // The founder made the group on another device; this one still looks at
    // the group a new connection starts in.
    @Test
    fun `a mark naming the account itself is its own group to move into`() {
        val mark = ProjectMark.Taken("alice-uid")

        assertEquals("alice-uid", ownProjectGroup(mark, "alice-uid", "family"))
    }

    @Test
    fun `a mark naming somebody else, or the group already shown, moves nowhere`() {
        assertNull(ownProjectGroup(ProjectMark.Taken("alice-uid"), "bob-uid", "family"))
        assertNull(ownProjectGroup(ProjectMark.Taken("alice-uid"), "alice-uid", "alice-uid"))
        assertNull(ownProjectGroup(ProjectMark.Free, "alice-uid", "family"))
        assertNull(ownProjectGroup(ProjectMark.Unknown, "alice-uid", "family"))
    }
}
