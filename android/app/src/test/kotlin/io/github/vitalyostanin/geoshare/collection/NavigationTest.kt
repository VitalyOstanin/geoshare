package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    @Test
    fun `the system is the top byte of the type`() {
        assertEquals(Constellation.Gps, navConstellation(0x0101))
        assertEquals(Constellation.Sbas, navConstellation(0x0201))
        assertEquals(Constellation.Glonass, navConstellation(0x0301))
        assertEquals(Constellation.Qzss, navConstellation(0x0401))
        assertEquals(Constellation.Beidou, navConstellation(0x0501))
        assertEquals(Constellation.Galileo, navConstellation(0x0601))
        assertEquals(Constellation.Irnss, navConstellation(0x0701))
    }

    // A phone newer than this code will name signals it does not know, and the
    // system is still worth having from such a type rather than thrown away.
    @Test
    fun `a system this does not know is named as such`() {
        assertEquals(Constellation.Other, navConstellation(0x0901))
        assertEquals(Constellation.Other, navConstellation(0))
    }

    @Test
    fun `the words are written as two characters a byte`() {
        assertEquals("007FFF10", hexOf(byteArrayOf(0, 0x7F, -1, 0x10)))
        assertEquals("", hexOf(byteArrayOf()))
    }

    // The satellite repeats the same page until it has something else to say,
    // and writing every repeat would fill the sky file within hours.
    @Test
    fun `the same page said again is not written again`() {
        val soFar = NavSoFar()
        val page = message(id = 7, data = "AABB")

        assertTrue(soFar.worthWriting(page))
        assertFalse(soFar.worthWriting(page))
        assertFalse(soFar.worthWriting(page))
    }

    @Test
    fun `the same page with different words is written`() {
        val soFar = NavSoFar()

        assertTrue(soFar.worthWriting(message(id = 7, data = "AABB")))
        assertTrue(soFar.worthWriting(message(id = 7, data = "AABC")))
    }

    @Test
    fun `another satellite saying the same words is written`() {
        val soFar = NavSoFar()

        assertTrue(soFar.worthWriting(message(id = 7, data = "AABB")))
        assertTrue(soFar.worthWriting(message(id = 8, data = "AABB")))
    }

    @Test
    fun `another page of the same satellite is written`() {
        val soFar = NavSoFar()

        assertTrue(soFar.worthWriting(message(id = 7, submessageId = 1, data = "AABB")))
        assertTrue(soFar.worthWriting(message(id = 7, submessageId = 2, data = "AABB")))
    }

    @Test
    fun `the repeats are counted even where they are not written`() {
        val soFar = NavSoFar()

        repeat(4) { soFar.worthWriting(message(id = 7, data = "AABB")) }
        soFar.worthWriting(message(id = 8, data = "CCDD"))
        soFar.worthWriting(message(id = 3, type = 0x0301, data = "EEFF"))

        assertEquals(
            NavSeen(messages = 5, satellites = 2),
            soFar.counted()[Constellation.Gps],
        )
        assertEquals(
            NavSeen(messages = 1, satellites = 1),
            soFar.counted()[Constellation.Glonass],
        )
    }

    // The answer this exists to find out is whether a chipset hands these over
    // at all, and an empty file cannot tell "nothing arrived" from "nobody
    // asked". So the summary says so rather than staying quiet.
    @Test
    fun `having heard nothing is said out loud`() {
        assertEquals(
            "the satellites have said nothing: no navigation messages at all",
            NavSoFar().saying(),
        )
    }

    @Test
    fun `what was heard is said by system`() {
        val soFar = NavSoFar()

        soFar.worthWriting(message(id = 7, data = "AABB"))
        soFar.worthWriting(message(id = 7, data = "AABB"))
        soFar.worthWriting(message(id = 3, type = 0x0301, data = "EEFF"))

        assertEquals(
            "the satellites are saying GPS 2 from 1, GLONASS 1 from 1; " +
                "2 of 3 messages carried something new",
            soFar.saying(),
        )
    }

    private fun message(id: Int, type: Int = 0x0101, submessageId: Int = 1, data: String) =
        NavMessage(
            constellation = navConstellation(type),
            id = id,
            type = type,
            status = 1,
            messageId = 1,
            submessageId = submessageId,
            data = data,
        )
}
