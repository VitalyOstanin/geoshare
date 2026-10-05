package io.github.vitalyostanin.geoshare.service

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnLineTest {
    @Test
    fun `a list without the device read from the server is the device gone`() {
        val line =
            ownLineOf(exists = true, listed = setOf("other"), deviceId = "this", fromServer = true)

        assertEquals(OwnLine.Gone, line)
    }

    // The member left the group or was taken off it: a matter for the roster.
    @Test
    fun `no list at all is not the device gone`() {
        val line =
            ownLineOf(exists = false, listed = emptySet(), deviceId = "this", fromServer = true)

        assertEquals(OwnLine.Unknown, line)
    }

    @Test
    fun `a list without the device in the cache says nothing`() {
        val line =
            ownLineOf(exists = true, listed = setOf("other"), deviceId = "this", fromServer = false)

        assertEquals(OwnLine.Unknown, line)
    }

    @Test
    fun `the device in the list is listed wherever it is read from`() {
        val line =
            ownLineOf(exists = true, listed = setOf("this"), deviceId = "this", fromServer = false)

        assertEquals(OwnLine.Listed, line)
    }

    @Test
    fun `a line that was there and went is said once`() = runBlocking {
        val said =
            flowOf(OwnLine.Listed, OwnLine.Gone, OwnLine.Gone, OwnLine.Unknown)
                .goneAfterListed()
                .toList()

        assertEquals(1, said.size)
    }

    // A device just signed in has not written its status yet.
    @Test
    fun `a line never there is not a line gone`() = runBlocking {
        val said = flowOf(OwnLine.Unknown, OwnLine.Gone).goneAfterListed().toList()

        assertEquals(0, said.size)
    }
}
