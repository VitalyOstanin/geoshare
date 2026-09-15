package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FoundByTest {
    // The note a fix carried while the receiver really was measuring: eight
    // satellites heard, and loudly.
    private val measured = "HDACC=0.0|SourceType=0|maxCn0=27|meanCn0=24|satellites=8"

    // The same phone answering from an assistance service, under the name of
    // the satellites, with nothing heard at the time.
    private val assisted = "QUICKGPS=true|SourceType=128|maxCn0=0|meanCn0=0|satellites=4"

    // Counted and silent: the receiver knows what should be overhead and
    // measured nothing off any of it, and says nothing about assistance.
    private val unheard = "HDACC=0.0|SourceType=0|maxCn0=0|meanCn0=0|satellites=4"

    @Test
    fun `an answer measured off the sky is the satellites`() {
        assertEquals(FoundBy.Satellites, foundBy(fix(from = "fused", alongside = measured)))
    }

    // The whole reason the provider's name is not what gets written: this
    // arrives as `gps` and was worked out over the network.
    @Test
    fun `an assistance answer is not the satellites however it is named`() {
        assertEquals(FoundBy.Assistance, foundBy(fix(from = "gps", alongside = assisted)))
    }

    @Test
    fun `satellites counted and none heard are said apart`() {
        assertEquals(FoundBy.Unheard, foundBy(fix(from = "gps", alongside = unheard)))
    }

    // Checked on the phone rather than assumed: an answer from the network
    // there is `Location[network … hAcc=699.999]` and carries no note at all,
    // so there is nothing in it to tell the Wi-Fi from a tower. Both are named
    // as the two radios together.
    @Test
    fun `an answer from the network with nothing attached is the other radios`() {
        assertEquals(FoundBy.OtherRadios, foundBy(fix(from = "network", alongside = null)))
    }

    // The platform's store of last known places answers for the network with
    // whatever was last put there, which can be the satellites' own answer.
    // The note is the measurement and the name is only a label.
    @Test
    fun `a satellite answer under the name of the network is still the satellites`() {
        assertEquals(FoundBy.Satellites, foundBy(fix(from = "network", alongside = measured)))
    }

    @Test
    fun `a provider that said nothing at all is not guessed at`() {
        assertEquals(FoundBy.Unsaid, foundBy(fix(from = "fused", alongside = null)))
    }

    // Anybody can turn this on in the developer settings, and it is the one
    // kind of pretence the platform admits to. It settles the question before
    // the note is read: a position put there by software carries whatever note
    // that software chose.
    @Test
    fun `a position another application put there says so first`() {
        assertEquals(
            FoundBy.Pretended,
            foundBy(fix(from = "gps", alongside = measured, pretended = true)),
        )
    }

    // The case nearly every written position falls into. The fused provider
    // hands over no note at all on these phones — checked in the sky file —
    // while the satellites are asked for directly and answer with one. Both
    // place the phone at the same spot at the same moment, which is the two
    // names of one answer rather than two answers agreeing.
    @Test
    fun `a fused answer standing on the satellites' own is the satellites`() {
        assertEquals(
            FoundBy.Satellites,
            foundBy(
                fix(from = "fused", alongside = null),
                satellites = fix(from = "gps", alongside = measured),
            ),
        )
    }

    @Test
    fun `a fused answer somewhere else is not credited to the satellites`() {
        assertEquals(
            FoundBy.Unsaid,
            foundBy(
                fix(from = "fused", alongside = null),
                satellites =
                fix(from = "gps", alongside = measured).copy(latitude = 61.02, longitude = 4.35),
            ),
        )
    }

    @Test
    fun `a satellite answer from before this one says nothing about it`() {
        assertEquals(
            FoundBy.Unsaid,
            foundBy(
                fix(from = "fused", alongside = null),
                satellites =
                fix(from = "gps", alongside = measured)
                    .copy(takenAt = Instant.parse("2026-09-15T11:58:00Z")),
            ),
        )
    }

    // The store of last known places answers for the satellites with whatever
    // was put there, and under a false signal that can be an assistance
    // answer. Agreement with it is not a measurement.
    @Test
    fun `agreeing with an assistance answer is not agreeing with a measurement`() {
        assertEquals(
            FoundBy.Unsaid,
            foundBy(
                fix(from = "fused", alongside = null),
                satellites = fix(from = "gps", alongside = assisted),
            ),
        )
    }

    private fun fix(from: String?, alongside: String?, pretended: Boolean = false) = Fix(
        latitude = 61.0,
        longitude = 4.0,
        takenAt = Instant.parse("2026-09-15T12:00:00Z"),
        accuracyMetres = 20.0,
        from = from,
        reported = Reported(pretended = pretended, alongside = alongside),
    )
}
