package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LeftOutRunTest {
    private val start = Instant.parse("2026-10-04T10:00:00Z")

    @Test
    fun `the first answer left out is told at once`() {
        val run = LeftOutRun()

        assertEquals(LeftOutNews(1, "speed", start), run.note("speed", start))
    }

    @Test
    fun `answers within the minute are counted but not told`() {
        val run = LeftOutRun()

        run.note("speed", start)

        assertNull(run.note("width", start.plusSeconds(7)))
        assertNull(run.note("width", start.plusSeconds(14)))
    }

    @Test
    fun `a minute later the whole run is told, dated from its beginning`() {
        val run = LeftOutRun()

        run.note("speed", start)
        run.note("speed", start.plusSeconds(30))

        assertEquals(
            LeftOutNews(3, "width", start),
            run.note("width", start.plusSeconds(60)),
        )
    }

    @Test
    fun `a written position starts the next run afresh`() {
        val run = LeftOutRun()
        val later = start.plusSeconds(20)

        run.note("speed", start)
        run.forget()

        assertEquals(LeftOutNews(1, "width", later), run.note("width", later))
    }
}
