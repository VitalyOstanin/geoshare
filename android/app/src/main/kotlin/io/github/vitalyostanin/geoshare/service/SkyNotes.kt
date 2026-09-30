package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.Accuracy
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.distanceMetres
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val TAG = "geoshare.service"

/**
 * How far the satellites' own answer has to be from the written one before the
 * disagreement is worth a line.
 *
 * Two hundred metres. Below that the two are answering about the same place
 * with different instruments — a position from the towers is a few hundred
 * metres wide on a good day — and above it they are answering about different
 * places, which is the case worth reading about.
 */
private const val SATELLITES_APART_METRES = 200.0

/** How often that disagreement is said out loud while it goes on. */
private val DISAGREEMENT_SAID_EVERY: Duration = Duration.ofMinutes(1)

/**
 * How long the receiver may answer nothing before the sky it is hearing is put
 * down without a position, and how often it is put down after that.
 *
 * Half a minute, against the fifteen seconds a watched phone asks at: a
 * receiver that is working never reaches it, so nothing is written while there
 * is nothing to explain. A receiver being talked over reaches it at once and
 * keeps reaching it, which is when the rows are worth their room.
 */
private val SKY_NOTED_WHILE_SILENT: Duration = Duration.ofSeconds(30)

/**
 * Everything the service writes about what the satellites themselves are
 * saying, apart from the position chosen to keep — a second opinion put
 * beside the written fix, a note when the receiver is heard but produces
 * nothing, and a warning when that second opinion and the written position
 * are a long way apart.
 *
 * Reads the service's own state — the last written fix, and when the
 * provider was last heard — through [lastFix] and [heardAt] rather than a
 * copy taken at construction, since both change for as long as the service
 * collects and a copy here would go stale the moment it was made. [radio] is
 * a lambda for a different reason: the service holds it behind its own
 * `by lazy`, and passing the instance in here would force that lazy open the
 * moment this class is built rather than the moment the radio is actually
 * asked something.
 */
internal class SkyNotes(
    private val context: Context,
    private val radio: () -> RadioWatch,
    private val lastFix: () -> Fix?,
    private val heardAt: () -> Instant,
) {
    /** When the satellites were last said to disagree with what is written. */
    private var disagreedAt: Instant = Instant.EPOCH

    /** Which of the satellites' answers has already been put in the record. */
    private var satellitesLastSaid: Duration? = null

    /**
     * The satellites' last answer, put in the record beside the written one.
     *
     * Read from the store rather than asked for, so it costs nothing and works
     * in the modes where the receiver is asleep — which is where the question
     * matters most, since a phone nobody is watching is a phone whose position
     * nobody is checking either.
     *
     * The same answer is not written twice: the store keeps handing over the
     * same fix until the receiver works out another one, and a file full of
     * repeats would hide how often the receiver actually answered.
     */
    fun noteWhatTheSatellitesLastSaid() {
        val satellites = lastSatellitePosition(context) ?: return
        val stamp =
            satellites.sinceBoot ?: satellites.takenAt.toEpochMilli().let(Duration::ofMillis)

        if (stamp == satellitesLastSaid) {
            return
        }

        satellitesLastSaid = stamp

        SkyLog.note(satellites.takenAt, satellites)
        noteTheDisagreement(satellites)
    }

    /**
     * What the satellites alone say, while the receiver is on anyway.
     *
     * Nothing is written to the database from this and nothing is filtered by
     * it: it is a second opinion put in the record beside the first. The fused
     * provider will not say whether an answer came from the sky or from the
     * Wi-Fi, and the difference is the whole question — a position from the
     * towers that is a kilometre out is a stale database, and a position from
     * the satellites that is a kilometre out is somebody transmitting.
     */
    suspend fun watchTheSatellites(mode: CollectionMode) {
        if (mode.accuracy != Accuracy.High) {
            return
        }

        satellitePositions(context).collect { fix ->
            SkyLog.note(fix.takenAt, fix)
            noteTheDisagreement(fix)
        }
    }

    /**
     * Writes down what the receiver is hearing while it works out nothing from
     * it.
     *
     * Only where the receiver is being asked often enough for silence to mean
     * something: in the slow modes a minute without an answer is the ordinary
     * state of a phone in a pocket, and rows about it would fill the file with
     * a phone behaving exactly as intended.
     *
     * The phone this was written for hears twenty-one satellites and produces
     * no position at all, and until this ran, that phone wrote nothing into the
     * sky file — the one case where the sky is the entire evidence was the one
     * case with no evidence kept.
     */
    suspend fun noteTheSkyWhileSilent(mode: CollectionMode) {
        if (mode.accuracy != Accuracy.High) {
            return
        }

        while (true) {
            delay(SKY_NOTED_WHILE_SILENT.toMillis())

            val quiet = Duration.between(heardAt(), Instant.now())

            if (quiet < SKY_NOTED_WHILE_SILENT) {
                continue
            }

            radio().noteSkyWithoutFix(Instant.now(), mode.name, quiet)

            // The satellites go on talking while the receiver works out
            // nothing from them, and a phone that is being talked over is
            // exactly the phone whose fixes have stopped: without this, the
            // count of what they said would only be written while things were
            // going well.
            radio().navigationSaying()?.let { AppLog.i(TAG, it) }
        }
    }

    /**
     * Says so when the satellites and the written position are not in the same
     * place, and not more often than a reader can use.
     *
     * The distance is the finding. Two answers about one phone that differ by
     * more than either of them claims to be wide cannot both be right, and
     * which of the two is wrong is not decided here — only that they disagree,
     * with the numbers to show it.
     */
    private fun noteTheDisagreement(satellites: Fix) {
        val written = lastFix() ?: return
        val apart = distanceMetres(written, satellites)

        if (apart < SATELLITES_APART_METRES) {
            return
        }

        if (Duration.between(disagreedAt, Instant.now()) < DISAGREEMENT_SAID_EVERY) {
            return
        }

        disagreedAt = Instant.now()

        AppLog.w(
            TAG,
            String.format(
                Locale.ROOT,
                "the satellites put the phone %.0f m from the position being written: " +
                    "%.5f,%.5f ±%.0f m against %.5f,%.5f ±%.0f m",
                apart,
                satellites.latitude,
                satellites.longitude,
                satellites.accuracyMetres ?: 0.0,
                written.latitude,
                written.longitude,
                written.accuracyMetres ?: 0.0,
            ),
        )
    }
}
