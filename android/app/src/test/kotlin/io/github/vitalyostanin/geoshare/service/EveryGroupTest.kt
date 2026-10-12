package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

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
        val last = IllegalStateException("unavailable")
        val links = listOf(link("first", IllegalStateException("denied")), link("second", last))

        try {
            EveryGroup { links }.tellStatus(MODE, 80, false)
            fail("the write should have failed")
        } catch (refusal: IllegalStateException) {
            assertSame(last, refusal)
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

    private fun link(projectId: String, refusal: Exception? = null): GroupLink {
        val status = mockk<PhoneStatusWriter>()

        if (refusal == null) {
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
    }
}
