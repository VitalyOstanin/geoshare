package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.GZIPOutputStream

class OrbitsSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a day's file is named by its year and day of the year`() {
        assertEquals(
            "https://igs.bkg.bund.de/root_ftp/IGS/BRDC/2026/274/BRDC00WRD_S_20262740000_01D_MN.rnx.gz",
            orbitsUrl("BRDC00WRD_S", LocalDate.of(2026, 10, 1)),
        )
    }

    @Test
    fun `yesterday's file is fresh enough, the day before is not`() {
        val today = LocalDate.of(2026, 10, 2)

        assertTrue(orbitsDue(null, today))
        assertFalse(orbitsDue(today.minusDays(1), today))
        assertTrue(orbitsDue(today.minusDays(2), today))
    }

    @Test
    fun `a kept file that is not gzip is deleted rather than read at every start`() {
        val kept = folder.newFile("2026-10-01.rnx.gz").apply { writeText("not compressed") }

        assertNull(readKept(kept))
        assertFalse(kept.exists())
    }

    @Test
    fun `a kept file cut short is deleted`() {
        val whole = gzipped("     3.04           N: GNSS NAV DATA    M: MIXED\n".repeat(50))
        val kept =
            folder.newFile("2026-10-01.rnx.gz").apply { writeBytes(whole.copyOf(whole.size / 2)) }

        assertNull(readKept(kept))
        assertFalse(kept.exists())
    }

    @Test
    fun `a kept file holding no orbits is deleted`() {
        val kept =
            folder.newFile("2026-10-01.rnx.gz").apply { writeBytes(gzipped("no orbits here\n")) }

        assertNull(readKept(kept))
        assertFalse(kept.exists())
    }

    private fun gzipped(text: String): ByteArray {
        val bytes = ByteArrayOutputStream()

        GZIPOutputStream(bytes).use { it.write(text.toByteArray()) }

        return bytes.toByteArray()
    }
}
