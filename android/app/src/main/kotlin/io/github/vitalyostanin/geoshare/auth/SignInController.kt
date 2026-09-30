package io.github.vitalyostanin.geoshare.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.STAND_IN_PASSWORD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.auth"

/**
 * The sign-in as the screen sees it: one state to render and two calls to make.
 *
 * The account is chosen on Google's own sign-in page, opened in the system
 * browser, rather than through a chooser this application draws: one build
 * serves every group's Firebase project, and the on-device chooser (Credential
 * Manager) only works for the one project whose Android OAuth client was
 * registered for this package and signing certificate. The browser page needs
 * nothing beyond the signing certificate's SHA-1 in the project's Firebase
 * settings, which is what every group's project already carries.
 */
class SignInController(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    private val current = MutableStateFlow<AuthState>(AuthState.Starting)
    val state: StateFlow<AuthState> = current.asStateFlow()

    private val listener =
        FirebaseAuth.AuthStateListener { updated ->
            val user = updated.currentUser

            current.value =
                if (user == null) {
                    AuthState.SignedOut()
                } else {
                    AppLog.i(TAG, "signed in as ${user.uid}")
                    AuthState.SignedIn(
                        memberId = user.uid,
                        displayName = user.displayName,
                        pictureUrl = user.photoUrl?.toString(),
                        email = user.email,
                    )
                }
        }

    /**
     * Starts reporting the session. The listener fires once on registration,
     * which is what turns [AuthState.Starting] into a real state.
     */
    fun start() = auth.addAuthStateListener(listener)

    fun stop() = auth.removeAuthStateListener(listener)

    /**
     * Signs in as one of the accounts the stand-in sign-in was filled with.
     *
     * Only reachable while this build talks to the emulators: there is no
     * Google account behind one, and a password is how its sign-in is asked.
     */
    suspend fun signInAsStandIn(email: String) {
        current.value = AuthState.SigningIn

        try {
            auth.signInWithEmailAndPassword(email, STAND_IN_PASSWORD).await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppLog.w(TAG, "the stand-in sign-in as $email failed", error)
            current.value = AuthState.SignedOut(classifySignInFailure(error))
        }
    }

    /**
     * Opens Google's sign-in page in the system browser and waits for the
     * session it starts.
     *
     * [activity] is what the browser is opened over and what its result comes
     * back to; a killed-and-recreated activity is handled by `pendingAuthResult`
     * below rather than by keeping this call itself alive across the restart.
     */
    suspend fun signIn(activity: Activity) {
        current.value = AuthState.SigningIn

        try {
            val provider =
                OAuthProvider
                    .newBuilder("google.com")
                    // Otherwise the page silently reuses the browser's last
                    // Google session, and a member with more than one account
                    // never gets to choose which of them this phone shares as.
                    .addCustomParameter("prompt", "select_account")
                    .build()

            // The browser can outlive this activity: the system is free to
            // kill it while the page is open and recreate it once the member
            // returns. Firebase keeps the flow's own result waiting for
            // exactly that case rather than losing it, which is why it is
            // asked for before a new flow is started.
            (auth.pendingAuthResult ?: auth.startActivityForSignInWithProvider(activity, provider))
                .await()
            // The state is not set here: the listener above answers for the
            // session, and it is the same answer after a restart.
        } catch (cancellation: CancellationException) {
            // The screen went away mid-flow. Not a failure to report, and
            // swallowing it would leave the coroutine running.
            throw cancellation
        } catch (error: Exception) {
            val failure = classifySignInFailure(error)

            AppLog.w(TAG, "sign-in failed as $failure", error)
            current.value = AuthState.SignedOut(failure)
        }
    }

    /** Ends the session, which the listener above answers by asking to sign in again. */
    fun signOut() = auth.signOut()
}
