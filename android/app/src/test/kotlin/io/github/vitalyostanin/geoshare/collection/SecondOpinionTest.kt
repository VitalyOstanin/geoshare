package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SecondOpinionTest {
    private val at = Instant.parse("2026-09-14T14:53:04Z")

    private fun fix(
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        takenAt: Instant = at,
        alongside: String? = null,
    ) = Fix(
        latitude = latitude,
        longitude = longitude,
        takenAt = takenAt,
        accuracyMetres = accuracy,
        reported = Reported(alongside = alongside),
    )

    // The pair of lines that this rule exists for, as the phone wrote them
    // down: one second apart, fifty-five kilometres apart.
    private val spoofed =
        fix(61.11817, 5.38151, 22.7, alongside = "SourceType=0|maxCn0=39|meanCn0=32|satellites=24")
    private val towers = fix(61.03268, 4.40300, 45.6)

    @Test
    fun `the towers contradicting the satellites is caught`() {
        val said = secondOpinion(spoofed, towers, at)!!

        assertTrue(said.disagrees)
        assertTrue("fifty-five kilometres apart", said.apartMetres > 50_000.0)
        assertTrue(said.apartMetres > said.allowedMetres)
    }

    @Test
    fun `the rule stands where every other rule is silent`() {
        assertEquals(JumpReason.Contradicted, whyAJump(null, spoofed, null, towers, at))
    }

    // The circle drawn in the forest: every fix of it agrees with the one
    // before it, and the phone was eight kilometres away the whole time.
    @Test
    fun `a slowly drawn false road is caught by the radios and by nothing else`() {
        val onTheCircle = fix(61.03740, 4.55470, 24.0)
        val reallyThere = fix(61.03268, 4.40300, 97.0, takenAt = at.minusSeconds(20))

        assertNull(whyAJump(onTheCircle, onTheCircle, null, null, at))
        assertEquals(
            JumpReason.Contradicted,
            whyAJump(onTheCircle, onTheCircle, null, reallyThere, at),
        )
    }

    @Test
    fun `two answers of a street corner are not a disagreement`() {
        val satellites = fix(61.03268, 4.40300, 12.0)
        val network = fix(61.03310, 4.40420, 90.0)

        assertFalse(secondOpinion(satellites, network, at)!!.disagrees)
        assertNull(whyAJump(null, satellites, null, network, at))
    }

    // The gap between the questions is paid for rather than ignored: four
    // minutes at the fastest anybody is believed to travel is thirteen
    // kilometres, and a phone that really did travel is not called a liar.
    @Test
    fun `whatever the phone could have travelled between the questions is allowed`() {
        val arrived = fix(61.03268, 4.58300, 20.0)
        val leftFrom = fix(61.03268, 4.40300, 50.0, takenAt = at.minus(Duration.ofMinutes(4)))

        val said = secondOpinion(arrived, leftFrom, at)!!

        assertTrue("ten kilometres apart", said.apartMetres > 9_000.0)
        assertTrue("the allowance covers a fast journey", said.allowedMetres > 13_000.0)
        assertFalse(said.disagrees)
    }

    @Test
    fun `an answer too old to be about this moment is not compared`() {
        val old = fix(61.03268, 4.40300, 45.6, takenAt = at.minus(Duration.ofMinutes(9)))

        assertNull(secondOpinion(spoofed, old, at))
        assertNull(whyAJump(null, spoofed, null, old, at))
    }

    // A tower answer that places the phone in a region says nothing about a
    // doorway, and would accuse the satellites on the strength of nothing.
    @Test
    fun `an answer that wide places the phone nowhere and is not asked`() {
        val vague = fix(61.03268, 4.40300, NETWORK_WIDTH_LIMIT_METRES + 1.0)

        assertNull(secondOpinion(spoofed, vague, at))
    }

    // The store of last known places answers for the network with whatever was
    // last put in it, which under a false signal is the false position itself.
    @Test
    fun `the satellites under the name of the network are not a second opinion`() {
        val sameAnswerAgain =
            fix(61.11817, 5.38151, 19.0, alongside = "SourceType=0|maxCn0=36|satellites=13")

        assertNull(secondOpinion(spoofed, sameAnswerAgain, at))
    }

    // A phone with no SIM has no towers to ask, and this is the only second
    // opinion it ever gets: the assistance service answers over the network,
    // from the Wi-Fi around the phone, under the name of the satellites.
    @Test
    fun `an assistance answer counts as a second opinion though it is named gps`() {
        val assisted =
            fix(61.03268, 4.40300, 100.0, alongside = "QUICKGPS=true|SourceType=128|maxCn0=0")

        val said = secondOpinion(spoofed, assisted, at)!!

        assertTrue(said.disagrees)
        assertEquals(JumpReason.Contradicted, whyAJump(null, spoofed, null, assisted, at))
    }

    @Test
    fun `a phone whose radios were never asked is not accused of anything`() {
        assertNull(secondOpinion(spoofed, null, at))
        assertNull(whyAJump(null, spoofed, null, null, at))
    }

    // Agreement cannot buy a way out of this one: the fixes of a false road
    // all agree with each other, which is what makes it a road.
    @Test
    fun `a contradiction is not something a second fix can confirm away`() {
        assertFalse(mayBeConfirmed(JumpReason.Contradicted))
    }
}
