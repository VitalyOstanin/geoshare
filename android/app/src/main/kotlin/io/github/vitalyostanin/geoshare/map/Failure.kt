package io.github.vitalyostanin.geoshare.map

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.vitalyostanin.geoshare.log.AppLog
import java.io.IOException

/** What a failed call to the database or the sign-in amounts to. */
enum class Failure {
    /** The service answered no: the rules, the account, the request itself. */
    REFUSED,

    /** No answer came back: the network, a server that is down, a wait that ran out. */
    UNREACHABLE,

    /** Nothing the service said: a fault in this application's own code. */
    UNEXPECTED,
}

/** The codes with which Firestore says it could not be reached rather than that it refuses. */
private val UNREACHABLE_CODES = setOf(
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    FirebaseFirestoreException.Code.CANCELLED,
)

/**
 * Sorts a failure into the three the application tells apart.
 *
 * Kept in one place so that a fault of the application's own — a null where
 * none was expected, a state that should not occur — is not reported in the
 * same words as the database saying no.
 */
fun failureOf(error: Throwable): Failure = when (error) {
    is UnansweredException, is IOException -> Failure.UNREACHABLE

    is FirebaseFirestoreException ->
        if (error.code in UNREACHABLE_CODES) Failure.UNREACHABLE else Failure.REFUSED

    is FirebaseException -> Failure.REFUSED

    else -> Failure.UNEXPECTED
}

/**
 * Writes down a failed call to the database under the level its kind deserves.
 *
 * A refusal or a silence is the service's doing and costs a warning; a fault
 * of the application's own is an error, and says so in the line, so that it is
 * not read as the service having answered no.
 */
fun logFailure(tag: String, what: String, error: Throwable) {
    if (failureOf(error) == Failure.UNEXPECTED) {
        AppLog.e(tag, "$what: a fault here, not the service's answer", error)
    } else {
        AppLog.w(tag, what, error)
    }
}
