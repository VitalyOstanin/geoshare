package io.github.vitalyostanin.geoshare.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionListTest {
    private fun settings(projectId: String) = ProjectSettings(
        version = 1,
        projectId = projectId,
        applicationId = "1:1:android:$projectId",
        apiKey = "AIzaExample",
        gcmSenderId = "1",
        site = "$projectId-site",
    )

    private val family = Connected(settings("family-project"), group = "alice")
    private val hike = Connected(settings("hike-project"), group = "bob")

    @Test
    fun `a list is read back as it was written`() {
        val list = ConnectionList(listOf(family, hike), selected = "hike-project")

        assertEquals(list, ConnectionList.parse(list.toJson()))
    }

    @Test
    fun `an empty list is read back as one`() {
        val list = ConnectionList(emptyList(), selected = null)

        assertEquals(list, ConnectionList.parse(list.toJson()))
    }

    @Test
    fun `an unreadable document is no list at all`() {
        assertNull(ConnectionList.parse("{"))
    }

    @Test
    fun `a list with one unreadable entry is not read at all`() {
        val broken = ConnectionList(listOf(family), selected = "family-project").toJson()
            .replace("\"version\":1", "\"version\":2")

        assertNull(ConnectionList.parse(broken))
    }

    @Test
    fun `the settings of an older build become the first connection`() {
        val list = ConnectionList.migrated(
            settingsJson = settings("family-project").toJson(),
            group = "alice",
        )

        assertEquals(ConnectionList(listOf(family), selected = "family-project"), list)
    }

    @Test
    fun `an older build that never named its group is in the inherited one`() {
        val list = ConnectionList.migrated(
            settingsJson = settings("family-project").toJson(),
            group = null,
        )

        assertEquals(INHERITED, list?.connections?.single()?.group)
    }

    @Test
    fun `unreadable settings of an older build migrate to nothing`() {
        assertNull(ConnectionList.migrated(settingsJson = "{", group = "alice"))
    }

    @Test
    fun `a second connection to the same project replaces the group in it`() {
        val list = ConnectionList(listOf(family, hike), selected = "hike-project")
            .with(Connected(settings("family-project"), group = "carol"))

        assertEquals(listOf("carol", "bob"), list.connections.map { it.group })
        assertEquals("family-project", list.selected)
    }

    @Test
    fun `a connection to another project is added and becomes the selected one`() {
        val list = ConnectionList(listOf(family), selected = "family-project").with(hike)

        assertEquals(
            listOf("family-project", "hike-project"),
            list.connections.map {
                it.settings.projectId
            },
        )
        assertEquals("hike-project", list.selected)
    }

    @Test
    fun `removing the selected connection selects the first of the rest`() {
        val list = ConnectionList(
            listOf(family, hike),
            selected = "hike-project",
        ).without("hike-project")

        assertEquals(listOf(family), list.connections)
        assertEquals("family-project", list.selected)
    }

    @Test
    fun `removing another connection keeps the selection`() {
        val list = ConnectionList(
            listOf(family, hike),
            selected = "hike-project",
        ).without("family-project")

        assertEquals("hike-project", list.selected)
    }

    @Test
    fun `removing the last connection leaves nothing selected`() {
        val list = ConnectionList(
            listOf(family),
            selected = "family-project",
        ).without("family-project")

        assertEquals(emptyList<Connected>(), list.connections)
        assertNull(list.selected)
    }

    @Test
    fun `selecting a project that is not in the list changes nothing`() {
        val list = ConnectionList(listOf(family), selected = "family-project")

        assertEquals(list, list.selecting("nowhere"))
    }

    @Test
    fun `a kept selection naming a project no longer listed falls back to the first`() {
        val kept = ConnectionList(listOf(family), selected = "gone-project").toJson()

        assertEquals("family-project", ConnectionList.parse(kept)?.selected)
    }
}
