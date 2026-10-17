package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Carrying
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.Judgement
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.PutDown
import io.github.vitalyostanin.geoshare.collection.Reported
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** What becomes of a fix once it is judged, apart from the service that holds the state. */
class JudgingTest {
    @Test
    fun `a fix left out as a jump becomes the candidate and lengthens the run`() {
        val run = JumpRun().after(AT_HOME, leftOutAsJump = true, used = true)
        val longer = run.after(ACROSS_TOWN, leftOutAsJump = true, used = true)

        assertSame(AT_HOME, run.candidate)
        assertEquals(1, run.inARow)
        assertSame(ACROSS_TOWN, longer.candidate)
        assertEquals(2, longer.inARow)
    }

    @Test
    fun `a fix taken from a source in use ends the run`() {
        val run = JumpRun(AT_HOME, 4).after(ACROSS_TOWN, leftOutAsJump = false, used = true)

        assertNull(run.candidate)
        assertEquals(0, run.inARow)
    }

    // The member switched the source off: that says nothing about whether the
    // satellites are being lied to, so a run that was building up is kept.
    @Test
    fun `a fix from a source switched off leaves the run as it was`() {
        val before = JumpRun(AT_HOME, 2)

        val after = before.after(ACROSS_TOWN, leftOutAsJump = false, used = false)

        assertEquals(before, after)
    }

    @Test
    fun `a jump is left out as a jump while jumps are kept out`() {
        val judged = judgement(jump = JumpReason.Speed)

        assertEquals(Outcome.LeftOutAsJump, outcomeOf(judged, jumpsFiltered = true, used = true))
    }

    @Test
    fun `a jump goes on when the member keeps jumps in`() {
        val judged = judgement(jump = JumpReason.Speed)

        assertEquals(Outcome.GoesOn, outcomeOf(judged, jumpsFiltered = false, used = true))
    }

    @Test
    fun `a jump the fix before agrees with goes on`() {
        val judged = judgement(jump = JumpReason.Width, agreed = true)

        assertEquals(Outcome.GoesOn, outcomeOf(judged, jumpsFiltered = true, used = true))
    }

    @Test
    fun `a jump back to a familiar place goes on`() {
        val judged = judgement(jump = JumpReason.Speed, home = true)

        assertEquals(Outcome.GoesOn, outcomeOf(judged, jumpsFiltered = true, used = true))
    }

    @Test
    fun `a fix from a source switched off is left out by its source`() {
        assertEquals(
            Outcome.LeftOutBySource,
            outcomeOf(judgement(), jumpsFiltered = true, used = false),
        )
    }

    // The jump counts towards asking the other radios, which a source switch
    // must not hide.
    @Test
    fun `a jump from a source switched off is still a jump`() {
        val judged = judgement(jump = JumpReason.Speed)

        assertEquals(Outcome.LeftOutAsJump, outcomeOf(judged, jumpsFiltered = true, used = false))
    }

    @Test
    fun `a fix from a source in use with nothing against it goes on`() {
        assertEquals(Outcome.GoesOn, outcomeOf(judgement(), jumpsFiltered = true, used = true))
    }

    @Test
    fun `news passes the filter however little has changed`() {
        assertTrue(passesTheFilter(true, AT_HOME, AT_HOME, CollectionMode.Standby))
    }

    @Test
    fun `a repeat of the last position does not pass an ordinary filter`() {
        assertFalse(passesTheFilter(false, AT_HOME, AT_HOME, CollectionMode.Standby))
    }

    @Test
    fun `a position far from the last one passes an ordinary filter`() {
        assertTrue(passesTheFilter(false, AT_HOME, ACROSS_TOWN, CollectionMode.Standby))
    }

    @Test
    fun `the first position ever passes an ordinary filter`() {
        assertTrue(passesTheFilter(false, null, AT_HOME, CollectionMode.Standby))
    }

    @Test
    fun `an answer from the satellites belongs in the shape of the run`() {
        assertTrue(belongsInTheShape(AT_HOME.copy(from = "gps"), trusted = false))
    }

    @Test
    fun `an answer with no provider named belongs in the shape of the run`() {
        assertTrue(belongsInTheShape(AT_HOME, trusted = false))
    }

    @Test
    fun `an answer from the towers does not belong in the shape of the run`() {
        assertFalse(belongsInTheShape(AT_HOME.copy(from = "network"), trusted = false))
    }

    @Test
    fun `a trusted answer does not belong in the shape of the run`() {
        assertFalse(belongsInTheShape(AT_HOME.copy(from = "gps"), trusted = true))
    }

    @Test
    fun `an answer an assistance service handed over does not belong in the shape`() {
        val assisted = AT_HOME.copy(from = "gps", reported = Reported(alongside = "quickGps=true"))

        assertFalse(belongsInTheShape(assisted, trusted = false))
    }

    @Test
    fun `no answer from the other radios has nothing to put in its place`() {
        assertEquals(
            OtherRadiosVerdict.NothingToPutInItsPlace,
            verdictOnTheOtherRadios(null, AT_HOME, emptyList(), null, STILL, NOW, AWAKE),
        )
    }

    @Test
    fun `an answer thousands of metres wide has nothing to put in its place`() {
        val district = ACROSS_TOWN.copy(accuracyMetres = 9_000.0)

        assertEquals(
            OtherRadiosVerdict.NothingToPutInItsPlace,
            verdictOnTheOtherRadios(district, AT_HOME, emptyList(), null, STILL, NOW, AWAKE),
        )
    }

    @Test
    fun `a wider answer about the written place repeats what is written`() {
        val written = AT_HOME.copy(accuracyMetres = 20.0)
        val wider = AT_HOME.copy(accuracyMetres = 1_600.0)

        assertEquals(
            OtherRadiosVerdict.RepeatsWhatIsWritten,
            verdictOnTheOtherRadios(wider, written, emptyList(), null, STILL, NOW, AWAKE),
        )
    }

    // The sensors say nobody touched the device, and the radios place it
    // across town: whatever built that answer, it was not carried there.
    @Test
    fun `an answer across town from a device nobody touched walked away`() {
        val putDown = PutDown(at = AT_HOME, since = AT_HOME.takenAt, steps = 1_000)
        val across = ACROSS_TOWN.copy(accuracyMetres = 30.0)

        assertEquals(
            OtherRadiosVerdict.WalkedAway,
            verdictOnTheOtherRadios(across, AT_HOME, emptyList(), putDown, STILL, NOW, AWAKE),
        )
    }

    @Test
    fun `an answer across town from a device that was carried stands`() {
        val putDown = PutDown(at = AT_HOME, since = AT_HOME.takenAt, steps = 1_000)
        val across = ACROSS_TOWN.copy(accuracyMetres = 30.0)
        val carried = Carrying(stirred = true)

        assertEquals(
            OtherRadiosVerdict.Stands,
            verdictOnTheOtherRadios(across, AT_HOME, emptyList(), putDown, carried, NOW, AWAKE),
        )
    }

    @Test
    fun `an answer about a position never written stands`() {
        val first = AT_HOME.copy(accuracyMetres = 40.0)

        assertEquals(
            OtherRadiosVerdict.Stands,
            verdictOnTheOtherRadios(first, null, emptyList(), null, STILL, NOW, AWAKE),
        )
    }

    private fun judgement(
        jump: JumpReason? = null,
        agreed: Boolean = false,
        home: Boolean = false,
    ) = Judgement(FoundBy.Satellites, settling = false, jump = jump, agreed = agreed, home = home)

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-17T12:00:00Z")
        val AWAKE: Duration = Duration.ofHours(5)
        val STILL = Carrying(stirred = false, steps = 0)
        val AT_HOME = Fix(latitude = 61.03268, longitude = 4.40300, takenAt = NOW)
        val ACROSS_TOWN = Fix(latitude = 61.11817, longitude = 4.38151, takenAt = NOW)
    }
}
