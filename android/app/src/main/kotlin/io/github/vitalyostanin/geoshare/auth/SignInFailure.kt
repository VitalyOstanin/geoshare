package io.github.vitalyostanin.geoshare.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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

    /** The group's Firebase project itself refuses this way of signing in, not this device. */
    Misconfigured,

    /** The email-and-password sign-in was refused: no such account, or a wrong password. */
    WrongCredentials,

    /** Firebase is throttling this device after too many failed attempts. */
    TooManyAttempts,

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

/**
 * The error codes for an email-and-password sign-in whose address or password
 * Firebase does not accept. The two are not told apart on purpose: naming the
 * wrong one would confirm which addresses have an account.
 */
private val WRONG_CREDENTIALS_ERROR_CODES = setOf(
    "ERROR_INVALID_CREDENTIAL",
    "ERROR_WRONG_PASSWORD",
    "ERROR_USER_NOT_FOUND",
    "ERROR_INVALID_EMAIL",
    "ERROR_USER_DISABLED",
)

/**
 * The failure behind a thrown error, in the terms the screen speaks.
 *
 * [byPassword] is whether the error answers a sign-in with an address and a
 * password. Only then is a refused credential the member's address or password:
 * the browser sign-in with Google throws the same exceptions for a credential
 * its own flow made malformed or let expire, and for an account an
 * administrator disabled, and telling the member their password was wrong
 * would send them looking for a password they never typed.
 */
fun classifySignInFailure(error: Throwable, byPassword: Boolean = false): SignInFailure = when {
    error is FirebaseAuthException && error.errorCode in CANCELLED_ERROR_CODES ->
        SignInFailure.Cancelled

    error is FirebaseAuthException && error.errorCode in MISCONFIGURED_ERROR_CODES ->
        SignInFailure.Misconfigured

    byPassword && error.isRefusedCredential() -> SignInFailure.WrongCredentials

    error is FirebaseTooManyRequestsException ||
        (error is FirebaseAuthException && error.errorCode == "ERROR_TOO_MANY_REQUESTS") ->
        SignInFailure.TooManyAttempts

    // Covers both a dead connection to Firebase itself and, wrapped as
    // WEB_NETWORK_REQUEST_FAILED, one to the browser page it opened.
    error is FirebaseNetworkException -> SignInFailure.Offline

    error is IOException -> SignInFailure.Offline

    else -> SignInFailure.Unknown
}

private fun Throwable.isRefusedCredential(): Boolean =
    this is FirebaseAuthInvalidCredentialsException ||
        this is FirebaseAuthInvalidUserException ||
        (this is FirebaseAuthException && errorCode in WRONG_CREDENTIALS_ERROR_CODES)
