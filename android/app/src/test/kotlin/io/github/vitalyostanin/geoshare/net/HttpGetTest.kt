package io.github.vitalyostanin.geoshare.net

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class HttpGetTest {
    @Test
    fun `a stream longer than the limit is read up to the limit and no further`() {
        // Longer than one read buffer, so the cap has to hold across chunks.
        val limit = DEFAULT_BUFFER_SIZE + 10
        val body = ByteArray(limit * 2) { it.toByte() }

        val read = ByteArrayInputStream(body).readUpTo(limit)

        assertArrayEquals(body.copyOf(limit), read)
    }
}
