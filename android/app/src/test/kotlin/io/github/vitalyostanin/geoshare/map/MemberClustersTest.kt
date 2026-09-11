package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MemberClustersTest {
    // The same expectations the web page checks: the two clients group the
    // same members the same way, or a family standing together would be one
    // marker on the phone and two on the page.
    @Test
    fun `members far apart each keep their own marker`() {
        val clusters =
            clusterMembers(
                listOf(
                    PlacedMember("a", 0.0, 0.0),
                    PlacedMember("b", 300.0, 0.0),
                ),
                radius = 40.0,
            )

        assertEquals(listOf(listOf("a"), listOf("b")), clusters.map { it.memberIds })
    }

    @Test
    fun `members whose pins overlap are gathered into one`() {
        val clusters =
            clusterMembers(
                listOf(
                    PlacedMember("a", 100.0, 100.0),
                    PlacedMember("b", 112.0, 108.0),
                    PlacedMember("c", 95.0, 90.0),
                ),
                radius = 40.0,
            )

        assertEquals(listOf(listOf("a", "b", "c")), clusters.map { it.memberIds })
    }

    // The list arrives from the database in whatever order it was written in,
    // and the faces in a group must not swap places between two ticks.
    @Test
    fun `the grouping does not depend on the order the members arrive in`() {
        val placed =
            listOf(
                PlacedMember("c", 95.0, 90.0),
                PlacedMember("a", 100.0, 100.0),
                PlacedMember("b", 112.0, 108.0),
            )

        assertEquals(
            listOf(listOf("a", "b", "c")),
            clusterMembers(placed, radius = 40.0).map { it.memberIds },
        )
        assertEquals(
            listOf(listOf("a", "b", "c")),
            clusterMembers(placed.reversed(), radius = 40.0).map { it.memberIds },
        )
    }

    // A row of members each standing a pin apart from the next one: without an
    // anchor they would all end up in one marker somewhere in the middle, and
    // that marker would sit where nobody is.
    @Test
    fun `a chain of neighbours does not collapse into one marker`() {
        val clusters =
            clusterMembers(
                listOf(
                    PlacedMember("a", 0.0, 0.0),
                    PlacedMember("b", 30.0, 0.0),
                    PlacedMember("c", 60.0, 0.0),
                ),
                radius = 40.0,
            )

        assertEquals(listOf(listOf("a", "b"), listOf("c")), clusters.map { it.memberIds })
    }

    @Test
    fun `nobody on the map is no markers at all`() {
        assertEquals(emptyList<MemberCluster>(), clusterMembers(emptyList(), radius = 40.0))
    }
}
