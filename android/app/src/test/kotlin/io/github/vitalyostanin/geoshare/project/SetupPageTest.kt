package io.github.vitalyostanin.geoshare.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupPageTest {
    @Test
    fun `the setup page address is an https site's root`() {
        val url = SetupPage.SETUP_PAGE_URL

        assertTrue(url.startsWith("https://"))
        assertTrue(url.endsWith("/"))
        assertEquals(url.trim(), url)
    }
}
