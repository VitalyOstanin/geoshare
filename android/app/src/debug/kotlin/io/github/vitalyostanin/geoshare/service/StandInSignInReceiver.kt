package io.github.vitalyostanin.geoshare.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.runCatchingFailure
import io.github.vitalyostanin.geoshare.project.Connections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "geoshare.standin"

const val ACTION_STAND_IN_SIGN_IN = "io.github.vitalyostanin.geoshare.debug.STAND_IN_SIGN_IN"
const val ACTION_STAND_IN_SIGN_OUT = "io.github.vitalyostanin.geoshare.debug.STAND_IN_SIGN_OUT"
const val EXTRA_EMAIL = "email"

/** Long enough for the emulator on the same machine, short of the broadcast's own limit. */
private const val SIGN_IN_TIMEOUT_MS = 8_000L

/**
 * Which stand-in account a sign-in from the shell asks for: the one named, or
 * the first of the household when none is, and nothing for an address the
 * seed never made.
 */
internal fun standInAccountFor(requested: String?): String? = when {
    requested.isNullOrBlank() -> STAND_IN_ACCOUNTS.first()
    requested in STAND_IN_ACCOUNTS -> requested
    else -> null
}

/**
 * Signs a stand-in account in or out on a broadcast from the shell (ADR-0044).
 *
 * For checks that drive the application from outside without a screen: the
 * sign-in screen's buttons do the same, but a run with nobody to press them
 * needs a command. Guarded twice: it exists in the debug build alone, and the
 * sender must hold DUMP, which the shell does and an installed application
 * cannot. It also does nothing unless this build already talks to the
 * stand-in services, so on a phone it never reaches a real project.
 *
 * The answer comes back as the broadcast's result: code -1 (RESULT_OK) with
 * the account's uid, or 0 with the reason.
 */
class StandInSignInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!StandInServices.inUse) {
            refuse("this build does not talk to the stand-in services")
            return
        }

        val connection = Connections.selected.value ?: Connections.all.value.firstOrNull()
        if (connection == null) {
            refuse("no group is connected")
            return
        }

        when (intent.action) {
            ACTION_STAND_IN_SIGN_OUT -> {
                connection.auth.signOut()
                AppLog.i(TAG, "signed out from the shell")
                setResult(Activity.RESULT_OK, "signed out", null)
            }

            ACTION_STAND_IN_SIGN_IN -> signIn(connection.auth, intent.getStringExtra(EXTRA_EMAIL))

            else -> refuse("unknown action ${intent.action}")
        }
    }

    private fun signIn(auth: FirebaseAuth, requested: String?) {
        val email = standInAccountFor(requested)
        if (email == null) {
            refuse("$requested is not a stand-in account")
            return
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            val outcome = withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
                runCatchingFailure {
                    auth.signInWithEmailAndPassword(email, STAND_IN_PASSWORD).await()
                }
            }

            when {
                outcome == null -> {
                    AppLog.w(TAG, "the sign-in from the shell as $email timed out")
                    pending.setResult(0, "sign-in timed out", null)
                }

                outcome.isSuccess -> {
                    val uid = outcome.getOrNull()?.user?.uid.orEmpty()
                    AppLog.i(TAG, "signed in from the shell as $email ($uid)")
                    pending.setResult(Activity.RESULT_OK, "signed in $email $uid", null)
                }

                else -> {
                    val failure = outcome.exceptionOrNull()
                    AppLog.w(TAG, "the sign-in from the shell as $email failed", failure)
                    pending.setResult(0, "sign-in failed: ${failure?.message}", null)
                }
            }
            pending.finish()
        }
    }

    private fun refuse(reason: String) {
        AppLog.w(TAG, "refused a request from the shell: $reason")
        setResult(0, reason, null)
    }
}
