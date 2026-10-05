package io.github.vitalyostanin.geoshare.auth

/**
 * Whether a browser sign-in was started for one group and has not ended.
 *
 * Kept outside the screen: the system may make the screen again, or the whole
 * application, while the sign-in page is open in the browser, and the attempt
 * held in memory goes with it. A page that broke never answers, and the screen
 * made again would otherwise open as if nothing had been tried.
 */
interface SignInMark {
    fun has(): Boolean

    fun note()

    fun forget()
}

/** For a sign-in nobody needs to remember, such as the stand-in one in tests. */
object NoSignInMark : SignInMark {
    override fun has() = false

    override fun note() = Unit

    override fun forget() = Unit
}

/** The first state of a screen without a session: unfinished if an earlier screen's sign-in never ended. */
internal fun signedOutAfter(startedEarlier: Boolean): AuthState.SignedOut =
    AuthState.SignedOut(if (startedEarlier) SignInFailure.Cancelled else null)
