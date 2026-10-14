package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Answered
import io.github.vitalyostanin.geoshare.collection.CLOCK_TOLERANCE
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.clockDisagrees
import io.github.vitalyostanin.geoshare.collection.distanceMetres
import io.github.vitalyostanin.geoshare.collection.impliedSpeedMps
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.word
import io.github.vitalyostanin.geoshare.log.AppLog
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val TAG = "geoshare.service"

/** What the filter made of a fix, as far as the log line about it goes. */
internal data class FixVerdict(
    val jump: JumpReason?,
    val agreed: Boolean,
    val home: Boolean,
    val trusted: Boolean,
)

/**
 * Writes down a fix as it arrived, beside what it would take to be true.
 *
 * A line per fix rather than per write: the ones left out are the ones worth
 * reading later, and the distance and the speed are what tell a walk from an
 * answer given by a jammed receiver.
 */
internal class FixLog(
    private val radio: RadioWatch,
    private val skyNotes: SkyNotes,
    private val jumpsOn: () -> Boolean,
    /**
     * What the filter was looking at when a jump it caught needs describing,
     * gathered at the moment [caughtBy] is asked for.
     */
    private val evidence: () -> LeftOutEvidence,
) {
    /** Notes [fix], measured from [last], the position the filter stands on. */
    fun note(fix: Fix, last: Fix?, mode: CollectionMode, verdict: FixVerdict) {
        val place = String.format(Locale.ROOT, "%.5f,%.5f", fix.latitude, fix.longitude)
        val width = fix.accuracyMetres?.let { String.format(Locale.ROOT, " ±%.0f m", it) }.orEmpty()
        // The clock is asked about the satellites only. An answer from the
        // towers is handed over seconds after it was taken, and its age would
        // read as a receiver whose time has been tampered with.
        val clock = if (verdict.trusted) "" else aClockApart(fix.takenAt)

        AppLog.i(
            TAG,
            "fix $place$width${sourceOf(fix)} in mode $mode, ${since(fix, last)}" +
                "${jumpNote(fix, verdict)}$clock",
        )

        radio.saying()?.let { AppLog.i(TAG, it) }
        radio.satellitesSaying()?.let { AppLog.i(TAG, it) }
        radio.skySaying()?.let { AppLog.i(TAG, it) }
        radio.driftSaying()?.let { AppLog.i(TAG, it) }
        radio.navigationSaying()?.let { AppLog.i(TAG, it) }
        radio.orbitsSaying()?.let { AppLog.i(TAG, it) }

        // The epoch behind the line just written, in its own file. Everything
        // above is one sentence about a minute of averages; this is the sky
        // itself, satellite by satellite, for the questions that come later.
        radio.noteSky(fix.takenAt, fix)
        skyNotes.noteWhatTheSatellitesLastSaid()
    }

    /**
     * Which way of knowing answered. Written down beside the place because a
     * position from the towers and a position from the satellites are worth
     * reading differently, and until now the log said nothing about which of
     * the two had just arrived.
     *
     * The name is followed by what the provider's own note said about how it
     * arrived at the answer, where it said anything: the name `gps` has been
     * seen on a position an assistance service handed over with not one
     * satellite audible, and the two read the same until this is put beside
     * them.
     */
    private fun sourceOf(fix: Fix): String {
        val said = answeredBy(noteOf(fix.reported?.alongside))
        val how = if (said == Answered.Unsaid) "" else ", ${said.word}"

        return fix.from?.let { " from $it$how" }.orEmpty()
    }

    private fun since(fix: Fix, last: Fix?): String {
        if (last == null) {
            // Not necessarily the first fix to arrive: while every answer is
            // being left out, none of them becomes the position the next one
            // is measured against.
            return "with no position to measure it from"
        }

        val moved = distanceMetres(last, fix)
        val pace =
            impliedSpeedMps(last, fix)?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "no"

        return String.format(Locale.ROOT, "%.0f m from the last at %s m/s", moved, pace)
    }

    private fun jumpNote(fix: Fix, verdict: FixVerdict): String {
        val jump = verdict.jump ?: return ""
        val caught = "; a jump ${caughtBy(jump, fix, evidence())}"

        return when {
            verdict.home -> "$caught, taken: the phone has been here before"
            verdict.agreed -> "$caught, taken because the fix before it agrees"
            jumpsOn() -> "$caught, left out"
            else -> "$caught, kept because the filter is off"
        }
    }

    /**
     * What to add about the clock of the fix, when there is anything to add.
     *
     * A receiver takes its time from the satellites, so a false signal can
     * carry a false clock, and that shows before the coordinates do. It cannot
     * stand alone as a verdict — a device setting its own clock from the same
     * satellites moves with the lie — but it costs nothing to write down.
     */
    private fun aClockApart(takenAt: Instant): String {
        val now = Instant.now()

        if (!clockDisagrees(takenAt, now, CLOCK_TOLERANCE)) {
            return ""
        }

        return "; stamped ${Duration.between(takenAt, now).seconds} s from the phone's own clock"
    }
}
