package io.github.vitalyostanin.geoshare.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.ZoneId

class FileLogTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val moment = Instant.parse("2026-09-11T14:45:12.345Z")

    private fun logIn(directory: File, limitBytes: Long = LOG_FILE_LIMIT_BYTES) =
        FileLog(directory, limitBytes, ZoneId.of("UTC"))

    @Test
    fun `a line carries the time, the level, the tag and the message`() {
        val log = logIn(folder.newFolder())

        log.append('I', "geoshare.auth", "signed in", at = moment)

        assertEquals("2026-09-11 14:45:12.345 I/geoshare.auth: signed in\n", log.read())
    }

    @Test
    fun `the directory is created on the first line`() {
        val directory = File(folder.newFolder(), "logs")
        val log = logIn(directory)

        log.append('I', "geoshare.app", "started", at = moment)

        // Reading is what waits for the lines already said to reach the file:
        // the writing itself happens on a thread of the log's own, so that the
        // callers — all of them on the main thread — are not kept waiting on
        // the disk.
        log.read()

        assertTrue(File(directory, "current.log").exists())
    }

    @Test
    fun `lines keep the order they were written in`() {
        val log = logIn(folder.newFolder())

        log.append('I', "geoshare.app", "first", at = moment)
        log.append('I', "geoshare.app", "second", at = moment)

        val written = log.read().lines().filter { it.isNotBlank() }

        assertEquals(2, written.size)
        assertTrue(written[0].endsWith("first"))
        assertTrue(written[1].endsWith("second"))
    }

    @Test
    fun `an error is written under its message`() {
        val log = logIn(folder.newFolder())

        log.append(
            'W',
            "geoshare.auth",
            "sign-in failed",
            IllegalStateException("no token"),
            moment,
        )

        val written = log.read()

        assertTrue(written.contains("W/geoshare.auth: sign-in failed"))
        assertTrue(written.contains("java.lang.IllegalStateException: no token"))
    }

    @Test
    fun `a full file rolls over and the older lines are still readable`() {
        // Small enough that the second line does not fit beside the first.
        val log = logIn(folder.newFolder(), limitBytes = 60)

        log.append('I', "geoshare.app", "older", at = moment)
        log.append('I', "geoshare.app", "newer", at = moment)

        val written = log.read().lines().filter { it.isNotBlank() }

        assertEquals(2, written.size)
        assertTrue(written[0].endsWith("older"))
        assertTrue(written[1].endsWith("newer"))
    }

    @Test
    fun `only two files are kept, so the oldest lines are dropped`() {
        val directory = folder.newFolder()
        val log = logIn(directory, limitBytes = 60)

        log.append('I', "geoshare.app", "first", at = moment)
        log.append('I', "geoshare.app", "second", at = moment)
        log.append('I', "geoshare.app", "third", at = moment)

        val written = log.read()

        assertTrue(written.contains("second"))
        assertTrue(written.contains("third"))
        assertTrue("the first line should have been dropped", !written.contains("first"))
        assertEquals(2, directory.listFiles().orEmpty().size)
    }
}
