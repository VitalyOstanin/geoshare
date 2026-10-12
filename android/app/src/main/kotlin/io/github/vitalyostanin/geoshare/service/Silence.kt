package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.IDLE_REPEAT
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val TAG = "geoshare.service"

/**
 * How long the provider and the writes have gone quiet, and the two loops
 * that keep the card from going grey while they do.
 *
 * Kept apart from the service so that the timers live in one place: the
 * service says how to ask and how to write, and this decides when.
 */
internal class Silence {
    /**
     * When the provider last said anything at all.
     *
     * Kept apart from the last position written down: a fix the filter
     * dropped is still the provider answering, and what the rescue below
     * watches for is silence rather than stillness.
     */
    var heardAt: Instant = Instant.now()

    /**
     * When the last write actually reached the database.
     *
     * Not the same thing as the moment of the fix in it: the quiet mode writes
     * a position taken half an hour ago on purpose, and what keeps the card
     * from going grey is when the document was written rather than when the
     * device was there.
     */
    var wroteAt: Instant? = null

    /**
     * Repeats the last position while the mode asks for nothing.
     *
     * The quiet mode is the one the device reaches by staying at home, and it
     * collects nothing at all: without this the household would go grey on
     * each other's maps every evening, which is the opposite of what standing
     * still means. Counted from the last write rather than from entering the
     * mode, because the device arrives here with a write already behind it.
     */
    suspend fun keepSaying(sayAgain: suspend () -> Boolean) {
        while (true) {
            val wrote = wroteAt
            val quiet = if (wrote == null) IDLE_REPEAT else Duration.between(wrote, Instant.now())

            if (quiet < IDLE_REPEAT) {
                delay((IDLE_REPEAT - quiet).toMillis())

                continue
            }

            if (!sayAgain()) {
                // Nothing written yet to repeat, or the write did not reach
                // the database. Either way the next round tries again.
                delay(IDLE_REPEAT.toMillis())
            }
        }
    }

    /**
     * Asks another way while the provider says nothing.
     *
     * The subscription is the only thing that normally writes anything, and it
     * can go quiet for reasons the service has no way of hearing about: the
     * fast mode asks the satellites alone, and a device on a desk indoors gets
     * no answer from them at all. The card then goes grey on everybody else's
     * map while this service is running perfectly well, which is the one
     * failure that looks exactly like the application being broken.
     *
     * So the silence is measured, and past [silence] the device asks once
     * with a coarser accuracy — the network answer, a hundred metres wide, is
     * worth more than an exact one nobody ever hears. The write goes through
     * whatever the filter would have said: what is being reported is not a new
     * place but the fact that the device is still there to report it.
     */
    suspend fun rescue(
        silence: Duration,
        mode: CollectionMode,
        ask: suspend () -> Fix?,
        write: suspend (Fix) -> Unit,
        foundNothing: suspend () -> Unit,
    ) {
        while (true) {
            val quiet = Duration.between(heardAt, Instant.now())

            if (quiet < silence) {
                delay((silence - quiet).toMillis())

                continue
            }

            AppLog.w(TAG, "nothing heard for $quiet in mode $mode; asking a cheaper way")

            // Counted from the asking rather than from an answer, so that a
            // rescue which finds nothing either waits its turn like the rest
            // instead of turning into a loop.
            heardAt = Instant.now()

            val fix = ask()

            if (fix != null) {
                write(fix)

                continue
            }

            foundNothing()
        }
    }
}
