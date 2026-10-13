package io.github.vitalyostanin.geoshare.log

import org.junit.Assert.assertEquals
import org.junit.Test

class LogScrubTest {
    private val none = Known()

    @Test
    fun `a position in a fix line becomes a place`() {
        assertEquals(
            "I/Sharing: fix ‹place› ±12 m in mode fast",
            scrubLog("I/Sharing: fix 59.93428,30.33510 ±12 m in mode fast", none),
        )
    }

    @Test
    fun `negative coordinates and a stored false place are cut out`() {
        assertEquals("‹place›,50,home", scrubLog("-33.86882,151.20929,50,home", none))
    }

    @Test
    fun `a speed or a width is not taken for a place`() {
        assertEquals("3.5 m/s, 12.0 m", scrubLog("3.5 m/s, 12.0 m", none))
    }

    @Test
    fun `the same id gets the same number and a new one the next`() {
        val a = "AbCdEfGhIjKlMnOpQrStUvWxYz12"
        val b = "ZyXwVuTsRqPoNmLkJiHgFeDcBa98"
        assertEquals(
            "‹member 1› ‹member 2› ‹member 1›",
            scrubLog("$a $b $a", none),
        )
    }

    @Test
    fun `numbering starts again with each call`() {
        val a = "AbCdEfGhIjKlMnOpQrStUvWxYz12"
        assertEquals("‹member 1›", scrubLog(a, none))
        assertEquals("‹member 1›", scrubLog(a, none))
    }

    @Test
    fun `a position id yields two numbers`() {
        val member = "AbCdEfGhIjKlMnOpQrStUvWxYz12"
        assertEquals(
            "‹member 1›.‹member 2›",
            scrubLog("$member.0123456789abcdef", none),
        )
    }

    @Test
    fun `a device id made as a uuid is numbered`() {
        assertEquals(
            "device ‹member 1›",
            scrubLog("device 123e4567-e89b-12d3-a456-426614174000", none),
        )
    }

    @Test
    fun `ordinary words and numbers are left alone`() {
        val text = "walking 12 steps; Sharing started at 1700000000"
        assertEquals(text, scrubLog(text, none))
    }

    @Test
    fun `project group and site are cut out`() {
        val known = Known(
            projectIds = listOf("demo-project"),
            groupNames = listOf("Night Walkers"),
            sites = listOf("demo-project.web.app"),
        )
        assertEquals(
            "‹project› in ‹group› at ‹site›",
            scrubLog("demo-project in Night Walkers at demo-project.web.app", known),
        )
    }

    @Test
    fun `a longer known string wins over a shorter one inside it`() {
        val known = Known(names = listOf("Pixel", "Pixel 8 Pro"))
        assertEquals("‹name› and ‹name›", scrubLog("Pixel 8 Pro and Pixel", known))
    }

    @Test
    fun `known and unknown addresses become email`() {
        val known = Known(emails = listOf("lena@example.org"))
        assertEquals(
            "‹email› invited ‹email›",
            scrubLog("lena@example.org invited sam.k+x@mail.example.com", known),
        )
    }

    @Test
    fun `names of people and devices are cut out`() {
        val known = Known(names = listOf("Lena", "Pixel 8"))
        assertEquals("‹name›'s ‹name› left", scrubLog("Lena's Pixel 8 left", known))
    }

    @Test
    fun `a join link becomes an invitation whole`() {
        val link = "https://demo-project.web.app/join#g=grp1&c=ABCD2345&n=Night%20Walkers"
        val known = Known(sites = listOf("demo-project"), groupNames = listOf("Night Walkers"))
        assertEquals("shared ‹invitation›, done", scrubLog("shared $link, done", known))
    }

    @Test
    fun `a code next to the word code becomes an invitation`() {
        assertEquals("code ‹invitation› expired", scrubLog("code ABCD2345 expired", none))
        assertEquals("code=‹invitation›", scrubLog("code=ABCD2345", none))
    }
}
