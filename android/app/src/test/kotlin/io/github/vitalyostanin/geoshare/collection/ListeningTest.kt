package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class ListeningTest {
    @Test
    fun `the first attempt follows the refusal quickly`() {
        assertEquals(LISTEN_AGAIN_AFTER, listenAgainAfter(0))
    }

    // A rule being deployed or an account signing in is over in seconds, and
    // the early attempts are the ones that catch those.
    @Test
    fun `every attempt waits twice as long as the one before`() {
        assertEquals(Duration.ofSeconds(10), listenAgainAfter(1))
        assertEquals(Duration.ofSeconds(20), listenAgainAfter(2))
        assertEquals(Duration.ofSeconds(40), listenAgainAfter(3))
    }

    @Test
    fun `the wait stops growing at the ceiling`() {
        assertEquals(LISTEN_AGAIN_AT_MOST, listenAgainAfter(7))
        assertEquals(LISTEN_AGAIN_AT_MOST, listenAgainAfter(8))
    }

    // A phone left running for days keeps asking on the same schedule rather
    // than arriving at a wait no arithmetic can hold.
    @Test
    fun `a subscription refused all day is still asked about every five minutes`() {
        assertEquals(LISTEN_AGAIN_AT_MOST, listenAgainAfter(1_000))
        assertEquals(LISTEN_AGAIN_AT_MOST, listenAgainAfter(Long.MAX_VALUE))
    }

    @Test
    fun `the schedule is the caller's to set where it has reason to`() {
        assertEquals(
            Duration.ofSeconds(4),
            listenAgainAfter(2, first = Duration.ofSeconds(1), atMost = Duration.ofMinutes(1)),
        )
        assertEquals(
            Duration.ofSeconds(30),
            listenAgainAfter(9, first = Duration.ofSeconds(1), atMost = Duration.ofSeconds(30)),
        )
    }
}
