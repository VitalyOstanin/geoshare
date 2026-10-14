package io.github.vitalyostanin.geoshare.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What became of one fix: how it was judged, and whether it was written.
 */
internal data class Turn<J>(val judged: J, val wrote: Boolean)

/**
 * Judges a fix and, when it is worth writing, writes and remembers it, all
 * before the next fix is judged.
 *
 * The rules measure every fix from the last one written, so a fix judged
 * while the one before it is still on its way to the database is measured
 * from a position the device has already left: two answers arriving together
 * from the subscription and from the rescue would both be weighed against
 * the older place, and the second could be written as a repeat of nothing.
 * Holding the turn through the write is what keeps the memory the rules read
 * from the one the write left behind.
 *
 * Whatever is done about a fix that was left out happens after the turn is
 * given back: asking the other radios writes a fix of its own, and that one
 * has to take its turn like the rest.
 */
internal suspend fun <J> inTurn(
    turn: Mutex,
    judge: suspend () -> J,
    worthWriting: (J) -> Boolean,
    write: suspend (J) -> Boolean,
): Turn<J> = turn.withLock {
    val judged = judge()

    Turn(judged, worthWriting(judged) && write(judged))
}
