package io.github.vitalyostanin.geoshare.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class SignInFailureTest {
    @Test
    fun `a dismissed chooser is a cancellation`() {
        assertEquals(
            SignInFailure.Cancelled,
            classifySignInFailure(GetCredentialCancellationException()),
        )
    }

    @Test
    fun `an empty chooser means there is no account`() {
        assertEquals(SignInFailure.NoAccount, classifySignInFailure(NoCredentialException()))
    }

    @Test
    fun `an interrupted flow is read as a dead network`() {
        assertEquals(
            SignInFailure.Offline,
            classifySignInFailure(GetCredentialInterruptedException()),
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
    fun `a provider that refuses this application is a misconfiguration`() {
        assertEquals(
            SignInFailure.Misconfigured,
            classifySignInFailure(GetCredentialProviderConfigurationException()),
        )
        assertEquals(
            SignInFailure.Misconfigured,
            classifySignInFailure(GetCredentialUnsupportedException()),
        )
    }

    @Test
    fun `anything else stays unknown`() {
        assertEquals(
            SignInFailure.Unknown,
            classifySignInFailure(GetCredentialUnknownException()),
        )
        assertEquals(SignInFailure.Unknown, classifySignInFailure(IllegalStateException("token")))
    }
}
