package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageSettingTest {
    @Test
    fun `a named language is a tag and the system is none`() {
        assertEquals("ru", languageTag(LanguageChoice.Russian))
        assertEquals("en", languageTag(LanguageChoice.English))
        assertNull(languageTag(LanguageChoice.System))
    }

    @Test
    fun `a tag names the language whatever region it carries`() {
        assertEquals(LanguageChoice.Russian, languageChosen("ru-RU"))
        assertEquals(LanguageChoice.English, languageChosen("en-GB"))
    }

    @Test
    fun `no tag at all is a member following their device`() {
        assertEquals(LanguageChoice.System, languageChosen(null))
        assertEquals(LanguageChoice.System, languageChosen(""))
    }

    // The platform hands back whatever was stored, which may have been written
    // by a build that spoke a language this one does not.
    @Test
    fun `a language this build does not speak leaves the device in charge`() {
        assertEquals(LanguageChoice.System, languageChosen("fr-FR"))
    }

    @Test
    fun `every choice survives being written down and read back`() {
        for (choice in LanguageChoice.entries) {
            assertEquals(choice, languageChosen(languageTag(choice)))
        }
    }

    @Test
    fun `from Android 13 a notification is left to the system`() {
        assertNull(noticeLanguage(33, "en", "en"))
        assertNull(noticeLanguage(34, "ru", ""))
    }

    @Test
    fun `before Android 13 the stored choice is the language of a notification`() {
        assertEquals("en", noticeLanguage(30, "en", ""))
        assertEquals("ru", noticeLanguage(26, "ru", "en"))
    }

    @Test
    fun `a stored choice of the system's language is no language at all`() {
        assertNull(noticeLanguage(30, "", "en"))
    }

    @Test
    fun `nothing stored falls back to what the platform holds`() {
        assertEquals("en", noticeLanguage(30, null, "en"))
        assertNull(noticeLanguage(30, null, ""))
    }
}
