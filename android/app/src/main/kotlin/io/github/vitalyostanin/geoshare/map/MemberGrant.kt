package io.github.vitalyostanin.geoshare.map

import java.time.Instant

/**
 * What a member gives out: which phones they carry, and who may watch them.
 *
 * Kept apart from the profile, which everybody in the group reads. Both of
 * these answer "where is this person" — the phone list is what a reader
 * subscribes to, and the permissions say who may — so a member of the
 * household who has been given nothing sees neither.
 *
 * Read by the member themselves and by the readers they named; anybody else
 * gets a refusal, which is the same answer the positions give them.
 */
data class MemberGrant(
    val memberId: String,
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
