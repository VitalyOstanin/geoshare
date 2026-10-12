package io.github.vitalyostanin.geoshare.map

/**
 * Where an account picture may come from: the host the sign-in provider keeps
 * them on, the same one the page's content policy lets images in from.
 *
 * Any member writes the address of their own picture, and every device that
 * shows them fetches it. An address anywhere else would have each of those
 * devices call on a server of the member's choosing, and tell it where they
 * are online from and when.
 */
private val ACCOUNT_PICTURE = Regex("https://[a-z0-9.-]*\\.googleusercontent\\.com/.*")

/** [address] where it is an account picture, or nothing where it is not one. */
fun accountPicture(address: String?): String? = address?.takeIf { ACCOUNT_PICTURE.matches(it) }
