package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration

/** A write carried into every group: when it counts as taken and when as failed. */
class EveryGroupTest {
    @Test
    fun `a group that refuses does not hold up the others`() = runBlocking {
        val refusing = link("first", IllegalStateException("denied"))
        val taking = link("second")

        val taken = EveryGroup { listOf(refusing, taking) }.tellStatus(MODE, 80, false)

        assertEquals(listOf("second"), taken)
        val status = taking.status
        coVerify { status.write(MODE, 80, false) }
    }

    @Test
    fun `when no group takes the write the last refusal is what fails`() = runBlocking {
        val first = IllegalStateException("denied")
        val last = IllegalStateException("unavailable")
        val links = listOf(link("first", first), link("second", last))

        try {
            EveryGroup { links }.tellStatus(MODE, 80, false)
            fail("the write should have failed")
        } catch (refusal: IllegalStateException) {
            assertSame(last, refusal)
            // And the other group's answer travels with it.
            assertEquals(listOf(first), refusal.suppressed.toList())
        }
    }

    @Test
    fun `a write with no group to go to fails`() = runBlocking {
        try {
            EveryGroup { emptyList() }.tellStatus(MODE, 80, false)
            fail("the write should have failed")
        } catch (refusal: IllegalStateException) {
            assertEquals("no group to say how the device is in", refusal.message)
        }
    }

    @Test
    fun `a cancelled write is not taken for a refusal`() = runBlocking {
        val cancelled = link("first", CancellationException("stopped"))
        val next = link("second")

        try {
            EveryGroup { listOf(cancelled, next) }.tellStatus(MODE, 80, false)
            fail("the cancellation should have come through")
        } catch (_: CancellationException) {
        }

        val status = next.status
        coVerify(exactly = 0) { status.write(any(), any(), any()) }
    }

    @Test
    fun `a group that does not answer holds up neither the others nor the service`() = runBlocking {
        val silent = link("first", silent = true)
        val taking = link("second")

        val taken =
            withTimeout(OUTER.toMillis()) {
                EveryGroup(wait = WAIT) { listOf(silent, taking) }.tellStatus(MODE, 80, false)
            }

        // Firestore keeps a write it could not send and sends it when the
        // database is back, so the group that did not answer counts as taken.
        assertEquals(listOf("first", "second"), taken)
        val status = taking.status
        coVerify { status.write(MODE, 80, false) }
    }

    @Test
    fun `a write no group answered still counts as made`() = runBlocking {
        val silent = link("first", silent = true)

        val taken =
            withTimeout(OUTER.toMillis()) {
                EveryGroup(wait = WAIT) { listOf(silent) }.tellStatus(MODE, 80, false)
            }

        assertEquals(listOf("first"), taken)
    }

    private fun link(
        projectId: String,
        refusal: Exception? = null,
        silent: Boolean = false,
    ): GroupLink {
        val status = mockk<PhoneStatusWriter>()

        if (silent) {
            coEvery { status.write(any(), any(), any()) } coAnswers { awaitCancellation() }
        } else if (refusal == null) {
            coEvery { status.write(any(), any(), any()) } just Runs
        } else {
            coEvery { status.write(any(), any(), any()) } throws refusal
        }

        return mockk {
            every { connection.projectId } returns projectId
            every { this@mockk.status } returns status
        }
    }

    private companion object {
        val MODE = CollectionMode.entries.first()
        val WAIT: Duration = Duration.ofMillis(50)
        val OUTER: Duration = Duration.ofSeconds(5)
    }
}
