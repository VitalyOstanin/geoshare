package io.github.vitalyostanin.geoshare.service

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.vitalyostanin.geoshare.log.AppLog
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SubscriptionsTest {
    @Test
    fun `being told this device may not ask is a refusal`() {
        assertTrue(refusedOutright(refusal()))
    }

    // A device in a lift and a device taken off a roster both stop hearing
    // answers, and only one of them is left showing something untrue.
    @Test
    fun `a connection that dropped is not one`() {
        assertFalse(
            refusedOutright(
                FirebaseFirestoreException(
                    "the server could not be reached",
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                ),
            ),
        )
    }

    @Test
    fun `neither is anything the database did not say`() {
        assertFalse(refusedOutright(IOException("the socket is gone")))
    }

    // The point of the whole arrangement: a member taken off the roster sees
    // the group empty rather than the list they were on a minute ago. Heard
    // well inside the second this waits, because the subscription itself is
    // not taken out again for five.
    @Test
    fun `what was last heard is dropped once the device is refused`() {
        assertEquals(
            listOf("the roster", "nothing"),
            heardWithinASecond(forgetting = { "nothing" }),
        )
    }

    // A caller with nothing on a screen says nothing to drop, and then the
    // refusal changes what is known not at all.
    @Test
    fun `a subscription with nothing to forget is left as it was`() {
        assertEquals(listOf("the roster"), heardWithinASecond(forgetting = null))
    }

    // A fault of the application's own comes back like a refusal, and the
    // log has to say which of the two it was.
    @Test
    fun `a fault of the application is logged as one, not as a refusal`() {
        val wasToSystemLog = AppLog.toSystemLog
        AppLog.toSystemLog = true
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        try {
            runBlocking {
                val told =
                    flow<String> { error("a state that should not occur") }.comingBack("roster")

                withTimeoutOrNull(1_000) { told.collect {} }
            }

            verify { Log.e(any(), match { "roster" in it && "a fault here" in it }, any()) }
            verify(exactly = 0) { Log.w(any(), any<String>(), any()) }
        } finally {
            AppLog.toSystemLog = wasToSystemLog
            unmockkStatic(Log::class)
        }
    }

    /**
     * What a subscription refused after its first answer says within a second.
     *
     * A second because the wait before it is taken out again is five: anything
     * heard inside the one is the refusal being answered rather than the
     * subscription coming back.
     */
    private fun heardWithinASecond(forgetting: (() -> String)?): List<String> = runBlocking {
        val heard = mutableListOf<String>()
        val told =
            flow {
                emit("the roster")

                throw refusal()
            }.comingBack("roster", forgetting = forgetting)

        withTimeoutOrNull(1_000) { told.collect { heard.add(it) } }

        heard
    }

    private fun refusal(): FirebaseFirestoreException = FirebaseFirestoreException(
        "this phone may not ask",
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
    )
}
