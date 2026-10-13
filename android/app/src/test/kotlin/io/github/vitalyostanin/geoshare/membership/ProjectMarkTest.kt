package io.github.vitalyostanin.geoshare.membership

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
