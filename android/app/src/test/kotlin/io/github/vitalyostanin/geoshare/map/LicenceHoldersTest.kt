package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The licences screen as the build collected it, read off the list it wrote.
 *
 * MIT and BSD ask for the holder's own copyright line to go with the
 * application. The SPDX texts kept in config/licenses carry a blank in its
 * place, so a library left on one of them shows "<year> <copyright holders>"
 * instead of who wrote it; each such library is pointed at a text of its own in
 * config/libraries. A new one under MIT or BSD fails here until it is.
 */
class LicenceHoldersTest {
    @Test
    fun `every licence on the screen names its holder`() {
        val unnamed =
            licenceLines(LIST.readText())
                .filter { line -> BLANKS.any { it in line.text } }
                .map { it.name }

        assertEquals(emptyList<String>(), unnamed)
    }

    private companion object {
        // Written by prepareLibraryDefinitionsDebug, which every test run
        // depends on (see app/build.gradle.kts).
        val LIST = File("build/generated/aboutLibraries/debug/res/raw/aboutlibraries.json")

        val BLANKS = listOf("<year>", "<copyright holders>", "<owner>")
    }
}
