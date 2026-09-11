package io.github.vitalyostanin.geoshare.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.auth"

/**
 * The sign-in as the screen sees it: one state to render and two calls to make.
 *
 * The account is chosen through Credential Manager, which shows the accounts
 * already on the phone, and the identity token it returns is exchanged for a
 * Firebase session. Firebase keeps that session across restarts, so the chooser
 * appears once rather than on every launch.
 */
class SignInController(
    /**
     * The project's web client id, generated into the resources from
     * google-services.json. It names the project the token is meant for; the
     * Android client id, which names this application, is checked by Google
     * services without passing through here.
     */
    private val serverClientId: String,
    private val credentials: CredentialManager,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val current = MutableStateFlow<AuthState>(AuthState.Starting)
    val state: StateFlow<AuthState> = current.asStateFlow()

    private val listener =
        FirebaseAuth.AuthStateListener { updated ->
            val user = updated.currentUser

            current.value =
                if (user == null) {
                    AuthState.SignedOut()
                } else {
                    Log.i(TAG, "signed in as ${user.uid}")
                    AuthState.SignedIn(memberId = user.uid, displayName = user.displayName)
                }
        }

    /**
     * Starts reporting the session. The listener fires once on registration,
     * which is what turns [AuthState.Starting] into a real state.
     */
    fun start() = auth.addAuthStateListener(listener)

    fun stop() = auth.removeAuthStateListener(listener)

    /**
     * Asks for an account and exchanges it for a session.
     *
     * [context] has to be the activity the chooser is shown over, not the
     * application: the chooser is a dialog of the calling activity.
     */
    suspend fun signIn(context: Context) {
        current.value = AuthState.SigningIn

        try {
            val token = requestIdentityToken(context)

            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            // The state is not set here: the listener above answers for the
            // session, and it is the same answer after a restart.
        } catch (cancellation: CancellationException) {
            // The screen went away mid-flow. Not a failure to report, and
            // swallowing it would leave the coroutine running.
            throw cancellation
        } catch (error: Exception) {
            val failure = classifySignInFailure(error)

            Log.w(TAG, "sign-in failed as $failure", error)
            current.value = AuthState.SignedOut(failure)
        }
    }

    /**
     * Ends the session and forgets the choice, so that the next sign-in offers
     * the accounts again instead of silently reusing the last one.
     */
    suspend fun signOut() {
        auth.signOut()

        try {
            credentials.clearCredentialState(ClearCredentialStateRequest())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // The session is already gone; a provider that would not forget the
            // account only means one extra tap next time.
            Log.w(TAG, "could not clear the stored credential state", error)
        }
    }

    private suspend fun requestIdentityToken(context: Context): String {
        val option =
            GetGoogleIdOption
                .Builder()
                .setServerClientId(serverClientId)
                // Every account on the phone is offered: on first run none of
                // them has authorized this application yet, and filtering by
                // that would show an empty chooser.
                .setFilterByAuthorizedAccounts(false)
                .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = credentials.getCredential(context, request).credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("The chooser returned ${credential.type}, which carries no identity token")
        }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
