package io.github.vitalyostanin.geoshare.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.OAuthProvider
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.STAND_IN_PASSWORD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.auth"

/** What Firebase answers a browser sign-in started while it thinks one is still open. */
private const val ALREADY_PRESENTED = "ERROR_WEB_CONTEXT_ALREADY_PRESENTED"

/**
 * How long a return to the screen waits for a browser sign-in to finish before
 * giving up on it: a page that did finish signs in well within it.
 */
private const val RETURN_GRACE_MILLIS = 5_000L

/*
 * The message Firebase's own sign-in activity sends when the member comes back
 * from the browser page without finishing it, spelled the way that activity
 * spells it. Not public API: a release that renames either leaves the message
 * unheard, and the sign-in goes back to failing until the app is restarted —
 * the behaviour this replaces, not a new one.
 */
private const val AUTH_INTENT_ACTION =
    "com.google.firebase.auth.ACTION_RECEIVE_FIREBASE_AUTH_INTENT"
private const val EXTRA_CANCELED = "com.google.firebase.auth.internal.EXTRA_CANCELED"

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
class SignInController(
    private val auth: FirebaseAuth,
    private val started: SignInMark = NoSignInMark,
) {
    private val current = MutableStateFlow<AuthState>(AuthState.Starting)
    val state: StateFlow<AuthState> = current.asStateFlow()

    /**
     * Which call of [signIn] is the latest. A flow given up on below still
     * fails afterwards, and its failure must not overwrite the state of the
     * flow that replaced it.
     */
    private var attempt = 0

    private val listener =
        FirebaseAuth.AuthStateListener { updated ->
            val user = updated.currentUser

            current.value =
                if (user == null) {
                    // The listener also answers every return to the screen,
                    // and "nobody yet" would wipe out an attempt the browser
                    // has not answered: that one ends on its own.
                    if (current.value == AuthState.SigningIn) return@AuthStateListener

                    val startedEarlier = started.has()

                    if (startedEarlier) {
                        AppLog.w(TAG, "an earlier screen's sign-in never finished")
                        started.forget()
                    }

                    signedOutAfter(startedEarlier)
                } else {
                    started.forget()
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
        val mine = ++attempt
        current.value = AuthState.SigningIn
        started.note()

        try {
            // Built on this group's own instance: the one-argument builder
            // reaches for the default one, and the page would sign in to the
            // first group's project instead of this one.
            val provider =
                OAuthProvider
                    .newBuilder("google.com", auth)
                    // Otherwise the page silently reuses the browser's last
                    // Google session, and a member with more than one account
                    // never gets to choose which of them this device shares as.
                    .addCustomParameter("prompt", "select_account")
                    .build()

            // The browser can outlive this activity: the system is free to
            // kill it while the page is open and recreate it once the member
            // returns. Firebase keeps the flow's own result waiting for
            // exactly that case rather than losing it, which is why it is
            // asked for before a new flow is started.
            val pending = auth.pendingAuthResult

            if (pending != null) {
                pending.await()
            } else {
                startOverAnyOpenFlow(activity, provider)
            }
            // The state is not set here: the listener above answers for the
            // session, and it is the same answer after a restart.
        } catch (cancellation: CancellationException) {
            // The screen went away mid-flow. Not a failure to report, and
            // swallowing it would leave the coroutine running.
            throw cancellation
        } catch (error: Exception) {
            val failure = classifySignInFailure(error)

            AppLog.w(TAG, "sign-in failed as $failure", error)

            if (mine == attempt) {
                started.forget()
                current.value = AuthState.SignedOut(failure)
            }
        }
    }

    /**
     * Starts the browser flow, first giving up on one Firebase still holds open.
     *
     * Firebase keeps one flow open per process and lets it go when its own
     * activity hears how the page ended. That activity can be closed without
     * hearing it: the member returns through the launcher, and the launcher
     * clears everything above this application's single-task activity. The
     * flow then stays open until the process dies, and every sign-in after it
     * fails as already in progress. Telling Firebase the page was left is what
     * its activity would have said had it been resumed.
     */
    private suspend fun startOverAnyOpenFlow(activity: Activity, provider: OAuthProvider) {
        try {
            startFlow(activity, provider).await()
        } catch (error: FirebaseAuthException) {
            if (error.errorCode != ALREADY_PRESENTED) throw error

            AppLog.w(TAG, "a browser sign-in was still held open; giving up on it")
            giveUpOpenFlow(activity)
            startFlow(activity, provider).await()
        }
    }

    /**
     * Gives up on a browser sign-in the member came back from without its answer.
     *
     * Coming back through the launcher closes Firebase's own activity without
     * letting it say the page was left, and a page that broke ("missing initial
     * state") never answers at all: the attempt would wait for good, with
     * nothing on the screen or in the log. Called on every return to the
     * screen; after [RETURN_GRACE_MILLIS] it says what that activity would
     * have said, which ends the attempt as cancelled.
     */
    suspend fun cameBack(context: Context) {
        if (current.value != AuthState.SigningIn) return

        delay(RETURN_GRACE_MILLIS)

        if (current.value == AuthState.SigningIn && auth.currentUser == null) {
            AppLog.w(TAG, "came back without the sign-in page's answer; giving up on it")
            giveUpOpenFlow(context)
        }
    }

    private fun startFlow(activity: Activity, provider: OAuthProvider): Task<AuthResult> =
        auth.startActivityForSignInWithProvider(activity, provider)

    private fun giveUpOpenFlow(context: Context) {
        val left = Intent(AUTH_INTENT_ACTION).putExtra(EXTRA_CANCELED, true)

        // Synchronous so that the flow is let go before the next one asks.
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(left)
    }

    /** Ends the session, which the listener above answers by asking to sign in again. */
    fun signOut() = auth.signOut()
}
