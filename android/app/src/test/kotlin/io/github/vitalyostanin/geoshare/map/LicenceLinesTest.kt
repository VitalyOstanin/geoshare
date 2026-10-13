package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How the list the build collected becomes the lines of the licences screen. */
class LicenceLinesTest {
    @Test
    fun `libraries are listed by name, whatever their case`() {
        val lines = licenceLines(LIST)

        assertEquals(listOf("Coil", "maplibre", "ZXing Core"), lines.map { it.name })
    }

    @Test
    fun `a licence is labelled by its SPDX id, and by its name without one`() {
        val lines = licenceLines(LIST).associateBy { it.name }

        assertEquals(listOf("Apache-2.0"), lines.getValue("Coil").labels)
        assertEquals(listOf("BSD 2-Clause License"), lines.getValue("maplibre").labels)
    }

    // An empty dialog would read as a library under no licence at all.
    @Test
    fun `a licence without a text is given by its address`() {
        val lines = licenceLines(LIST).associateBy { it.name }

        assertTrue(lines.getValue("Coil").text.startsWith("Apache License"))
        assertEquals("https://example.org/bsd-2", lines.getValue("maplibre").text)
        assertEquals("13.6.1", lines.getValue("maplibre").version)
    }

    @Test
    fun `a text naming the holder stands in for the blank one of the same licence`() {
        val line = licenceLines(HELD).single()

        assertEquals(listOf("Apache-2.0", "MIT"), line.labels)
        assertEquals("Apache License\n\nCopyright 2004 The Holders", line.text)
    }

    private companion object {
        val HELD =
            """
            {
              "libraries": [
                {"uniqueId": "org.example:held", "artifactVersion": "1.0", "name": "Held",
                 "developers": [], "licenses": ["Apache-2.0", "MIT-held", "MIT"]}
              ],
              "licenses": {
                "Apache-2.0": {"name": "Apache License 2.0", "spdxId": "Apache-2.0",
                  "content": "Apache License", "hash": "Apache-2.0"},
                "MIT-held": {"name": "MIT License", "spdxId": "MIT",
                  "content": "Copyright 2004 The Holders", "hash": "MIT-held"},
                "MIT": {"name": "MIT License", "spdxId": "MIT",
                  "content": "Copyright (c) <year> <copyright holders>", "hash": "MIT"}
              }
            }
            """.trimIndent()

        val LIST =
            """
            {
              "libraries": [
                {"uniqueId": "com.google.zxing:core", "artifactVersion": "3.5.4",
                 "name": "ZXing Core", "developers": [], "licenses": ["Apache-2.0"]},
                {"uniqueId": "org.maplibre.gl:android-sdk", "artifactVersion": "13.6.1",
                 "name": "maplibre", "developers": [], "licenses": ["bsd2"]},
                {"uniqueId": "io.coil-kt.coil3:coil", "artifactVersion": "3.6.2",
                 "name": "Coil", "developers": [], "licenses": ["Apache-2.0"]}
              ],
              "licenses": {
                "Apache-2.0": {"name": "Apache License 2.0", "url": "https://www.apache.org/licenses/LICENSE-2.0",
                  "spdxId": "Apache-2.0", "content": "Apache License\nVersion 2.0", "hash": "Apache-2.0"},
                "bsd2": {"name": "BSD 2-Clause License", "url": "https://example.org/bsd-2", "hash": "bsd2"}
              }
            }
            """.trimIndent()
    }
}
