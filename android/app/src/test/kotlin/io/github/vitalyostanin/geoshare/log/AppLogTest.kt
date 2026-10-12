package io.github.vitalyostanin.geoshare.log

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Whether a position said by the application reaches the system log. */
class AppLogTest {
    private val wasToSystemLog = AppLog.toSystemLog

    @Before
    fun listenToTheSystemLog() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun putItBack() {
        AppLog.toSystemLog = wasToSystemLog
        unmockkStatic(Log::class)
    }

    @Test
    fun `a release build keeps positions out of the system log`() {
        AppLog.toSystemLog = false

        AppLog.i(TAG, POSITION)
        AppLog.w(TAG, POSITION)
        AppLog.e(TAG, POSITION)

        verify(exactly = 0) { Log.i(any(), match { POSITION_NUMBER in it }) }
        verify(exactly = 0) { Log.w(any(), match<String> { POSITION_NUMBER in it }, any()) }
        verify(exactly = 0) { Log.e(any(), match { POSITION_NUMBER in it }, any()) }
    }

    @Test
    fun `a debug build says the same in the system log`() {
        AppLog.toSystemLog = true

        AppLog.i(TAG, POSITION)

        verify { Log.i(TAG, POSITION) }
    }

    private companion object {
        const val TAG = "geoshare.test"
        const val POSITION_NUMBER = "60.93863"
        const val POSITION = "fix $POSITION_NUMBER, 4.31413"
    }
}
