package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class SoughtEndsTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val now = Instant.parse("2026-10-12T10:00:00Z")

    @Test
    fun `a search ending today shows the time alone`() {
        assertEquals("22:33", soughtEndsShort(Instant.parse("2026-10-12T22:33:00Z"), now, zone))
    }

    @Test
    fun `a search ending another day shows the day and the time`() {
        assertEquals(
            SOUGHT_ENDS_AT.format(Instant.parse("2026-10-13T01:05:00Z").atZone(zone)),
            soughtEndsShort(Instant.parse("2026-10-13T01:05:00Z"), now, zone),
        )
    }
}
