package io.github.vitalyostanin.geoshare.map

import java.time.Instant

/**
 * What a member says about themselves, so that a marker can carry a person
 * rather than the first characters of an account id.
 *
 * Written by the member it describes and by nobody else, which is why the name
 * is theirs to set and the picture is the one their account already has: the
 * clients never upload an image, they point at the one the provider hosts.
 */
data class MemberProfile(
    val memberId: String,
    val displayName: String,
    /** The account picture, as the provider hosts it; absent for an account without one. */
    val pictureUrl: String?,
    /** When the member last touched the profile; see [rememberProfileChanges]. */
    val updatedAt: Instant? = null,
)
