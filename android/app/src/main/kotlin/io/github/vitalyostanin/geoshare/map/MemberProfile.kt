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
    /**
     * The phones this member carries, by the identity each of them made for
     * itself. This is how a reader knows which position documents to read:
     * the collection of positions cannot be listed, because a permission with
     * an expiry is not something a query can prove.
     */
    val devices: List<String> = emptyList(),
    /**
     * Who this member lets read their position, and until when. Kept here
     * rather than only in the positions, so that every phone of the member
     * writes the same permissions and a new phone inherits them.
     */
    val sharedWith: Map<String, Instant> = emptyMap(),
)
