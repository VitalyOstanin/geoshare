package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeSettingTest {
    @Test
    fun `a stored name is the answer it spells`() {
        assertEquals(ThemeChoice.Dark, themeNamed("Dark"))
    }

    @Test
    fun `a phone that has never been asked follows the system`() {
        assertEquals(ThemeChoice.System, themeNamed(null))
    }

    @Test
    fun `a name from a build that no longer has it follows the system`() {
        assertEquals(ThemeChoice.System, themeNamed("Sepia"))
    }

    @Test
    fun `a chosen theme outranks the system`() {
        assertEquals(true, darkWanted(ThemeChoice.Dark, systemDark = false))
        assertEquals(false, darkWanted(ThemeChoice.Light, systemDark = true))
    }

    @Test
    fun `following the system means both of its answers`() {
        assertEquals(true, darkWanted(ThemeChoice.System, systemDark = true))
        assertEquals(false, darkWanted(ThemeChoice.System, systemDark = false))
    }
}
