package io.github.vitalyostanin.geoshare.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import java.io.IOException

/**
 * Why the sign-in produced no member.
 *
 * A sign-in fails in ways that call for different answers: waiting out a dead
 * network, adding an account, or nothing at all when the chooser was dismissed
 * on purpose. Without the distinction the screen can only say "try again",
 * which is wrong advice in three cases out of four.
 */
enum class SignInFailure {
    /** The chooser was dismissed. Nothing to report and nothing to fix. */
    Cancelled,

    /** No Google account on the device, or none offered to this application. */
    NoAccount,

    Offline,

    /** The application is not registered for the Google services on this device. */
    Misconfigured,

    Unknown,
}

/** The failure behind a thrown error, in the terms the screen speaks. */
fun classifySignInFailure(error: Throwable): SignInFailure = when (error) {
    is GetCredentialCancellationException -> SignInFailure.Cancelled

    is NoCredentialException -> SignInFailure.NoAccount

    // "Retryable" in the library's own words: the flow was interrupted
    // rather than refused, which is what a dead network looks like here.
    is GetCredentialInterruptedException -> SignInFailure.Offline

    is FirebaseNetworkException -> SignInFailure.Offline

    is IOException -> SignInFailure.Offline

    is GetCredentialProviderConfigurationException -> SignInFailure.Misconfigured

    is GetCredentialUnsupportedException -> SignInFailure.Misconfigured

    else -> SignInFailure.Unknown
}
