package io.github.vitalyostanin.geoshare.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
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

    @Test
    fun `anything else stays unknown`() {
        assertEquals(
            SignInFailure.Unknown,
            classifySignInFailure(FirebaseAuthWebException("ERROR_WEB_INTERNAL_ERROR", "internal")),
        )
        assertEquals(SignInFailure.Unknown, classifySignInFailure(IllegalStateException("token")))
    }
}
