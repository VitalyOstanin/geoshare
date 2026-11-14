package io.github.vitalyostanin.geoshare.auth

/**
 * Whether the account signed in is plainly not the one the managed configuration expects.
 * Addresses are compared whole and without regard to case, since a mail address is typed by
 * whoever configured the device and Google hands back its own spelling. With no expected account,
 * or no address to compare, there is no verdict.
 */
fun differsFromExpectedAccount(expected: String?, signedIn: String?): Boolean {
    val wanted = expected?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val actual = signedIn?.trim()?.takeIf { it.isNotEmpty() } ?: return false

    return !wanted.equals(actual, ignoreCase = true)
}
