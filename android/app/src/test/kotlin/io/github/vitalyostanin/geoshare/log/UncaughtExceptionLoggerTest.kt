package io.github.vitalyostanin.geoshare.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneId

class UncaughtExceptionLoggerTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `an uncaught exception is logged with its stack and passed on once`() {
        val log = FileLog(folder.newFolder(), zone = ZoneId.of("UTC"))
        val passed = mutableListOf<Pair<Thread, Throwable>>()
        val handler =
            UncaughtExceptionLogger(
                record = { message, error -> log.append('E', "geoshare.crash", message, error) },
                flush = log::flush,
                previous = { thread, error -> passed += thread to error },
            )
        val boom = RuntimeException("boom")
        val worker = Thread({}, "worker-7")

        handler.uncaughtException(worker, boom)

        val written = log.read()

        assertTrue(written.contains("uncaught on worker-7"))
        assertTrue(written.contains("java.lang.RuntimeException: boom"))
        assertTrue(written.contains("at "))
        assertEquals(1, passed.size)
        assertSame(worker, passed[0].first)
        assertSame(boom, passed[0].second)
    }

    @Test
    fun `a failing log does not keep the previous handler from running`() {
        var calls = 0
        val handler =
            UncaughtExceptionLogger(
                record = { _, _ -> error("disk gone") },
                flush = {},
                previous = { _, _ -> calls++ },
            )

        handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        assertEquals(1, calls)
    }
}
