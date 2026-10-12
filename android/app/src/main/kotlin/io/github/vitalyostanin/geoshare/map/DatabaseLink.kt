package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant

/** How long the screen waits on the cache alone before saying the database is silent. */
val LINK_GRACE: Duration = Duration.ofSeconds(10)

/** How long an action waits for the database before saying it did not answer. */
val ANSWER_WAIT: Duration = Duration.ofSeconds(15)

/**
 * Whether the database is answering, out of what the store says of each
 * snapshot of the group document — the same judgement the page's `link.js`
 * makes.
 *
 * Firestore says nothing of a lost connection: a listener goes on being
 * called, only with what the device held before, marked as coming from the
 * cache. A network that is up is no answer either, since a device behind a
 * tunnel can reach the network and not the database. So the link is judged
 * by the snapshots themselves: the server confirming one is the database
 * answering, and [LINK_GRACE] of nothing but the cache is its silence.
 *
 * [confirmedAt] is when the server last confirmed a snapshot, [cachedSince]
 * when the cache alone began to answer, or null while the server does.
 */
data class DatabaseLink(val confirmedAt: Instant?, val cachedSince: Instant?) {
    /** The link after one more snapshot, served from the cache or confirmed by the server. */
    fun next(fromCache: Boolean, now: Instant): DatabaseLink = if (fromCache) {
        DatabaseLink(confirmedAt = confirmedAt, cachedSince = cachedSince ?: now)
    } else {
        DatabaseLink(confirmedAt = now, cachedSince = null)
    }

    /** Whether the database has been silent for longer than [LINK_GRACE]. */
    fun lost(now: Instant): Boolean = cachedSince != null && !now.isBefore(cachedSince + LINK_GRACE)

    /** When [lost] turns true, or null while nothing is being waited for. */
    fun lostAt(): Instant? = cachedSince?.plus(LINK_GRACE)

    companion object {
        /**
         * The link before any snapshot: the device has asked and heard
         * nothing, so the waiting starts at once and a database that never
         * answers is told of too.
         */
        fun from(now: Instant): DatabaseLink = DatabaseLink(confirmedAt = null, cachedSince = now)
    }
}

/**
 * What the screens are told of the link: silent since [since], with what they
 * show last confirmed at [knownAt]; or answering, when [since] is null.
 */
data class LinkState(val since: Instant? = null, val knownAt: Instant? = null) {
    val lost: Boolean get() = since != null
}

/** The link as the screens below the activity see it; answering until told otherwise. */
val LocalLink = compositionLocalOf { LinkState() }

/** A write the database did not answer within [ANSWER_WAIT]; Firestore still holds it and sends it later. */
class UnansweredException(cause: Throwable? = null) :
    Exception("the database did not answer", cause)

/** How a write asked for from a window came out. */
enum class WriteOutcome { DONE, REFUSED, UNANSWERED }

/**
 * The write, or [UnansweredException] once [ANSWER_WAIT] has passed without
 * the server acknowledging it. The write itself is not called off — Firestore
 * keeps it and sends it when the database is back — only the screen stops
 * waiting for it.
 */
suspend fun <T> withinAnswerWait(wait: Duration = ANSWER_WAIT, write: suspend () -> T): T = try {
    withTimeout(wait.toMillis()) { write() }
} catch (late: TimeoutCancellationException) {
    throw UnansweredException(late)
}
