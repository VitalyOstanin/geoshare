package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OrbitsSourceTest {
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
}
