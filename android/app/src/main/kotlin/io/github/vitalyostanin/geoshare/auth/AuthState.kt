package io.github.vitalyostanin.geoshare.auth

/**
 * What the application knows about who is using it.
 *
 * The member id is the Firebase account id: the same string the security rules
 * compare against, and the name of the document this device writes its position
 * into.
 */
sealed interface AuthState {
    /** Before Firebase has said whether a stored session exists. */
    data object Starting : AuthState

    data class SignedOut(val failure: SignInFailure? = null) : AuthState

    /** The chooser is open, or the token is being exchanged. */
    data object SigningIn : AuthState

    data class SignedIn(
        val memberId: String,
        val displayName: String?,
        /** The account picture, as the provider hosts it; absent for an account without one. */
        val pictureUrl: String?,
        /** The Google address, as the provider reports it; what an invitation is made out to. */
        val email: String? = null,
    ) : AuthState
}
