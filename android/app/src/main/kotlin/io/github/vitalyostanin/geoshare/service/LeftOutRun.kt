package io.github.vitalyostanin.geoshare.service

import java.time.Duration
import java.time.Instant

/**
 * How often the group is told that this phone's answers are being left out.
 *
 * Once a minute. Under a transmitter an answer arrives every seven seconds,
 * and telling the database about each of them would spend its free daily
 * allowance in an afternoon; what a reader wants from the field is whether
 * this is happening and roughly how much of it, and a minute answers that.
 */
internal val LEFT_OUT_TOLD_EVERY: Duration = Duration.ofMinutes(1)

/** What the group is told about a run of left-out answers. */
data class LeftOutNews(val count: Int, val reason: String, val since: Instant)

/**
 * The run of answers left out since the last position was written: how many,
 * why the last one was, when the run began and when the group was last told
 * about it.
 *
 * Kept so that the card of a phone under a transmitter can say what is
 * happening to it. Everything here is cleared by a position being written,
 * because a written position is the run ending.
 */
class LeftOutRun {
    private var count = 0

    private var since: Instant? = null

    private var toldAt: Instant? = null

    /**
     * Counts one more answer left out for [reason], and says what to tell the
     * group when it is time to tell it: at most once every
     * [LEFT_OUT_TOLD_EVERY], however fast the answers come.
     */
    fun note(reason: String, now: Instant): LeftOutNews? {
        count += 1

        val began = since ?: now.also { since = it }
        val told = toldAt

        if (told != null && Duration.between(told, now) < LEFT_OUT_TOLD_EVERY) {
            return null
        }

        toldAt = now

        return LeftOutNews(count, reason, began)
    }

    /**
     * Forgets the run, because a position has been written.
     *
     * The document itself needs no clearing: it is written whole, and the
     * three fields simply stop being in it.
     */
    fun forget() {
        count = 0
        since = null
        toldAt = null
    }
}
