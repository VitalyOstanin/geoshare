package io.github.vitalyostanin.geoshare.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWebException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class SignInFailureTest {
    @Test
    fun `closing the browser page is a cancellation`() {
        assertEquals(
            SignInFailure.Cancelled,
            classifySignInFailure(
                FirebaseAuthWebException("ERROR_WEB_CONTEXT_CANCELED", "canceled"),
            ),
        )
    }

    @Test
    fun `declining the requested permissions is also a cancellation`() {
        assertEquals(
            SignInFailure.Cancelled,
            classifySignInFailure(FirebaseAuthException("ERROR_USER_CANCELLED", "cancelled")),
        )
    }

    @Test
    fun `a firebase network error is read as a dead network`() {
        assertEquals(
            SignInFailure.Offline,
            classifySignInFailure(FirebaseNetworkException("no connection")),
        )
    }

    @Test
    fun `a socket error is read as a dead network`() {
        assertEquals(SignInFailure.Offline, classifySignInFailure(UnknownHostException("host")))
        assertEquals(SignInFailure.Offline, classifySignInFailure(IOException("reset")))
    }

    @Test
    fun `a project without google sign-in enabled is a misconfiguration`() {
        assertEquals(
            SignInFailure.Misconfigured,
            classifySignInFailure(FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "disabled")),
        )
        assertEquals(
            SignInFailure.Misconfigured,
            classifySignInFailure(
                FirebaseAuthException("ERROR_UNAUTHORIZED_DOMAIN", "not allowlisted"),
            ),
        )
    }

    private val refusedCredentials =
        listOf(
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND",
            "ERROR_INVALID_EMAIL",
            "ERROR_USER_DISABLED",
        ).map { FirebaseAuthException(it, "refused") } +
            FirebaseAuthInvalidCredentialsException("x", "bad") +
            FirebaseAuthInvalidUserException("x", "none")

    @Test
    fun `an unaccepted address or password is wrong credentials`() {
        refusedCredentials.forEach { error ->
            assertEquals(
                SignInFailure.WrongCredentials,
                classifySignInFailure(error, byPassword = true),
            )
        }
    }

    @Test
    fun `a refused credential from the Google sign-in says nothing of a password`() {
        // The browser flow throws these for a malformed or expired credential
        // of its own and for an account an administrator disabled.
        refusedCredentials.forEach { error ->
            assertEquals(SignInFailure.Unknown, classifySignInFailure(error))
        }
    }

    @Test
    fun `throttling is too many attempts`() {
        assertEquals(
            SignInFailure.TooManyAttempts,
            classifySignInFailure(FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "slow")),
        )
        assertEquals(
            SignInFailure.TooManyAttempts,
            classifySignInFailure(FirebaseTooManyRequestsException("slow")),
        )
    }

    @Test
    fun `anything else stays unknown`() {
        assertEquals(
            SignInFailure.Unknown,
            classifySignInFailure(FirebaseAuthWebException("ERROR_WEB_INTERNAL_ERROR", "internal")),
        )
        assertEquals(SignInFailure.Unknown, classifySignInFailure(IllegalStateException("token")))
    }
}
