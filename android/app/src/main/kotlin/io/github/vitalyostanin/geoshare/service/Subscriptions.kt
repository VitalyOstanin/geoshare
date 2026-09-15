package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.listenAgainAfter
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

private const val TAG = "geoshare.listening"

/**
 * The same subscription, taken out again every time it is refused.
 *
 * The database client treats a refusal as the end of the question: it drops
 * the listener and nothing arrives again, so a phone refused once goes on
 * knowing nothing until the application is started afresh. What the flows
 * below do instead is fail — the refusal closes them — and this is what
 * subscribes again afterwards.
 *
 * Every attempt is said out loud, and said with how long the next wait is:
 * a subscription quietly coming back every five minutes for a day is a cost
 * nobody asked about, and the log is where such a thing is noticed.
 */
fun <T> Flow<T>.comingBack(named: String): Flow<T> = retryWhen { cause, attempt ->
    val waiting = listenAgainAfter(attempt)

    AppLog.w(
        TAG,
        "the $named subscription was refused; asking again in ${waiting.seconds} s",
        cause,
    )

    delay(waiting.toMillis())

    true
}
