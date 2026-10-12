package io.github.vitalyostanin.geoshare.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import java.io.IOException

/**
 * Why the sign-in produced no member.
 *
 * A sign-in fails in ways that call for different answers: waiting out a dead
 * network, or nothing at all when the browser page was closed on purpose.
 * Without the distinction the screen can only say "try again", which is wrong
 * advice more often than not.
 */
enum class SignInFailure {
    /** The browser page was closed, or the member chose to go back. Nothing to fix. */
    Cancelled,

    Offline,

    /** The group's Firebase project itself refuses Google sign-in, not this device. */
    Misconfigured,

    Unknown,
}

/**
 * The error codes `FirebaseAuthException.errorCode` carries for the ways the
 * browser page is closed without finishing: by the member, or by the system
 * declining the permissions the page asked for.
 */
private val CANCELLED_ERROR_CODES = setOf("ERROR_WEB_CONTEXT_CANCELED", "ERROR_USER_CANCELLED")

/**
 * The error codes for a project whose own Firebase settings, not this build or
 * this device, are what keep Google sign-in from working.
 */
private val MISCONFIGURED_ERROR_CODES =
    setOf("ERROR_OPERATION_NOT_ALLOWED", "ERROR_UNAUTHORIZED_DOMAIN")

/** The failure behind a thrown error, in the terms the screen speaks. */
fun classifySignInFailure(error: Throwable): SignInFailure = when {
    error is FirebaseAuthException && error.errorCode in CANCELLED_ERROR_CODES ->
        SignInFailure.Cancelled

    error is FirebaseAuthException && error.errorCode in MISCONFIGURED_ERROR_CODES ->
        SignInFailure.Misconfigured

    // Covers both a dead connection to Firebase itself and, wrapped as
    // WEB_NETWORK_REQUEST_FAILED, one to the browser page it opened.
    error is FirebaseNetworkException -> SignInFailure.Offline

    error is IOException -> SignInFailure.Offline

    else -> SignInFailure.Unknown
}
