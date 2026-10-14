package io.github.vitalyostanin.geoshare.map

import android.util.Log
import io.github.vitalyostanin.geoshare.log.AppLog
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** What the report does with a name or an address it could not read to scrub. */
class KnownOrSaidTest {
    private val wasToSystemLog = AppLog.toSystemLog

    @Before
    fun listenToTheSystemLog() {
        AppLog.toSystemLog = true
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun putItBack() {
        AppLog.toSystemLog = wasToSystemLog
        unmockkStatic(Log::class)
    }

    @Test
    fun `what could be read is known, and nothing is said`() {
        assertEquals("Anna", knownOrSaid("the published profile") { "Anna" })

        verify(exactly = 0) { Log.w(any(), any<String>(), any()) }
    }

    // The list scrubbed from the report is shorter by what failed, and the
    // log is the only place that can say so.
    @Test
    fun `what could not be read is said to be left in the report`() {
        val read: String? = knownOrSaid("the published profile") { error("the file is gone") }

        assertNull(read)
        verify {
            Log.w(any(), match<String> { "the published profile" in it && "report" in it }, any())
        }
    }
}
