package io.github.vitalyostanin.geoshare.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val BODY = """
    ### Added

    - The update screen shows what is new.
    - Settings keep a `theme`
      across restarts.

    ### Fixed

    - A crash on [the map](https://example.org/map) screen.

    ### Project owners

    - Rotate the signing key.

    <details lang="ru">
    <summary>По-русски</summary>

    ### Добавлено

    - Экран обновления показывает, что нового.

    ### Владельцам проектов

    - Сменить ключ подписи.

    </details>
""".trimIndent()

private val ENGLISH_ONLY = BODY.substringBefore("\n<details")

class ReleaseNotesTest {
    @Test
    fun `an english interface gets the english sections`() {
        val notes = ReleaseNotes.parse(BODY, "en")!!

        assertEquals(listOf("Added", "Fixed"), notes.sections.map { it.title })
        assertEquals(
            "The update screen shows what is new.",
            notes.sections[0].items[0],
        )
    }

    @Test
    fun `a russian interface gets the russian sections`() {
        val notes = ReleaseNotes.parse(BODY, "ru")!!

        assertEquals(
            listOf(
                NotesSection("Добавлено", listOf("Экран обновления показывает, что нового.")),
            ),
            notes.sections,
        )
    }

    @Test
    fun `without a russian block the russian interface gets english`() {
        val notes = ReleaseNotes.parse(ENGLISH_ONLY, "ru")!!

        assertEquals(listOf("Added", "Fixed"), notes.sections.map { it.title })
    }

    @Test
    fun `the owners subsection is dropped in both languages`() {
        assertEquals(
            false,
            ReleaseNotes.parse(BODY, "en")!!.sections.any { it.title == "Project owners" },
        )
        assertEquals(
            false,
            ReleaseNotes.parse(BODY, "ru")!!.sections.any { it.title == "Владельцам проектов" },
        )
    }

    @Test
    fun `continuation lines are joined into one item`() {
        val items = ReleaseNotes.parse(BODY, "en")!!.sections[0].items

        assertEquals("Settings keep a theme across restarts.", items[1])
    }

    @Test
    fun `links keep their text and code loses its backticks`() {
        val items = ReleaseNotes.parse(BODY, "en")!!.sections[1].items

        assertEquals(listOf("A crash on the map screen."), items)
    }

    @Test
    fun `carriage returns do not matter`() {
        val notes = ReleaseNotes.parse(BODY.replace("\n", "\r\n"), "ru")!!

        assertEquals("Добавлено", notes.sections.single().title)
    }

    @Test
    fun `a body with nothing to show gives null`() {
        assertNull(ReleaseNotes.parse("", "en"))
        assertNull(ReleaseNotes.parse("Just a line.", "en"))
        assertNull(ReleaseNotes.parse("### Project owners\n\n- Only this.", "en"))
    }
}
