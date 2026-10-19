package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** A database saying no, a database out of reach, and a fault of our own, told apart. */
class FailureTest {
    @Test
    fun `the rules saying no is a refusal`() {
        assertEquals(
            Failure.REFUSED,
            failureOf(FirebaseFirestoreException("denied", Code.PERMISSION_DENIED)),
        )
    }

    @Test
    fun `a server out of reach or a wait that ran out is unreachable`() {
        assertEquals(
            Failure.UNREACHABLE,
            failureOf(FirebaseFirestoreException("down", Code.UNAVAILABLE)),
        )
        assertEquals(Failure.UNREACHABLE, failureOf(UnansweredException()))
        assertEquals(Failure.UNREACHABLE, failureOf(IOException("reset")))
    }

    @Test
    fun `a fault of the application's own is neither`() {
        assertEquals(Failure.UNEXPECTED, failureOf(IllegalStateException("no state")))
        assertEquals(Failure.UNEXPECTED, failureOf(NullPointerException()))
    }

    @Test
    fun `writing a failure down does not fail whatever its kind`() {
        val wasToSystemLog = AppLog.toSystemLog
        AppLog.toSystemLog = false

        try {
            logFailure(
                "test",
                "a refusal",
                FirebaseFirestoreException("denied", Code.PERMISSION_DENIED),
            )
            logFailure("test", "no answer", IOException("reset"))
            logFailure("test", "a fault", IllegalStateException("no state"))
        } finally {
            AppLog.toSystemLog = wasToSystemLog
        }
    }

    @Test
    fun `a call that succeeds gives its value`() {
        assertEquals(7, runCatchingFailure { 7 }.getOrElse { 0 })
    }

    @Test
    fun `an ordinary exception goes to the caller's answer`() {
        val refusal = FirebaseFirestoreException("denied", Code.PERMISSION_DENIED)

        val answered = runCatchingFailure<Int> { throw refusal }.exceptionOrNull()

        assertSame(refusal, answered)
    }

    @Test
    fun `a cancellation thrown in the block is thrown on`() {
        val cancellation = CancellationException("the screen went away")

        val thrown =
            assertThrows(CancellationException::class.java) {
                runCatchingFailure { throw cancellation }.getOrElse { }
            }

        assertSame(cancellation, thrown)
    }

    @Test
    fun `a coroutine cancelled while waiting stops without the answer running`() = runBlocking {
        var answered = false
        var after = false

        val job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                runCatchingFailure { awaitCancellation() }.onFailure { answered = true }
                after = true
            }
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertFalse(answered)
        assertFalse(after)
    }

    @Test
    fun `an error is not kept`() {
        assertThrows(StackOverflowError::class.java) {
            runCatchingFailure { throw StackOverflowError() }
        }
    }
}
