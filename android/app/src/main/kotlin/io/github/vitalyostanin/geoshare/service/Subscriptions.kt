package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FirebaseFirestoreException
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
 * the listener and nothing arrives again, so a device refused once goes on
 * knowing nothing until the application is started afresh. What the flows
 * below do instead is fail — the refusal closes them — and this is what
 * subscribes again afterwards.
 *
 * Every attempt is said out loud, and said with how long the next wait is:
 * a subscription quietly coming back every five minutes for a day is a cost
 * nobody asked about, and the log is where such a thing is noticed.
 *
 * A caller that has something on a screen says what it knows once it is told
 * outright that it is no longer to be told. Without that, the last answer
 * stands: a member taken off a roster goes on seeing who was in the group,
 * because nothing arrives to replace what they were shown before the refusal
 * — and a device started afresh is shown it again out of the client's own
 * store, which answers from what it last held before the server refuses.
 */
fun <T> Flow<T>.comingBack(
    named: String,
    /** What the subscription amounts to once this device is refused outright. */
    forgetting: (() -> T)? = null,
): Flow<T> = retryWhen { cause, attempt ->
    val waiting = listenAgainAfter(attempt)

    AppLog.w(
        TAG,
        "the $named subscription was refused; asking again in ${waiting.seconds} s",
        cause,
    )

    // Before the wait rather than after it: the screen is wrong for as long as
    // it stands uncorrected, and the wait is five minutes by the end.
    if (forgetting != null && refusedOutright(cause)) {
        emit(forgetting())
    }

    delay(waiting.toMillis())

    true
}

/**
 * Whether the answer was "this device is not to be told" rather than "not just
 * now".
 *
 * The two are worth telling apart, because only one of them says anything
 * about what this device knows. A connection that dropped leaves the last
 * answer as true as it was a second ago; a refusal says the question is no
 * longer this device's to ask, and the answer it is still showing belongs to a
 * group it has been taken out of.
 */
fun refusedOutright(cause: Throwable): Boolean {
    val answer = cause as? FirebaseFirestoreException ?: return false

    return answer.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
}
