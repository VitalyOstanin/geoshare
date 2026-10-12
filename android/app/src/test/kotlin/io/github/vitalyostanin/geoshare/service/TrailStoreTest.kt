package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Trail
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

/** The trail is written off the caller's thread and read back as it was. */
class TrailStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    // Asked for at the call: the folder exists only once the test has started.
    private val context = mockk<Context> { every { filesDir } answers { folder.root } }

    @Test
    fun `the write waits for its own thread and leaves the trail on disk`() {
        val waiting = mutableListOf<Runnable>()
        val store = TrailStore(context) { waiting += it }
        val trail =
            Trail(
                listOf(
                    Fix(
                        61.03268,
                        4.40300,
                        Instant.parse("2026-09-13T12:00:00Z"),
                        accuracyMetres = 8.0,
                    ),
                ),
            )

        store.write(trail)

        assertTrue("nothing written on the caller's thread", store.read().fixes.isEmpty())

        waiting.forEach(Runnable::run)

        assertEquals(trail, store.read())
    }
}
