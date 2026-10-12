package io.github.vitalyostanin.geoshare.map

import java.time.Instant

/**
 * What a member gives out: which devices they carry, and who may watch them.
 *
 * Kept apart from the profile, which everybody in the group reads. Both of
 * these answer "where is this person" — the device list is what a reader
 * subscribes to, and the permissions say who may — so a member of the
 * household who has been given nothing sees neither.
 *
 * Read by the member themselves and by the readers they named; anybody else
 * gets a refusal, which is the same answer the positions give them.
 */
data class MemberGrant(
    val memberId: String,
    /**
     * The devices this member carries, by the identity each of them made for
     * itself. This is how a reader knows which position documents to read:
     * the collection of positions cannot be listed, because a permission with
     * an expiry is not something a query can prove.
     */
    val devices: List<String> = emptyList(),
    /**
     * Who this member lets read their position, and until when. Kept here
     * rather than only in the positions, so that every device of the member
     * writes the same permissions and a new device inherits them.
     */
    val sharedWith: Map<String, Instant> = emptyMap(),
    /**
     * What each of the member's devices last said about itself, by the id of
     * the device. Read from a document of its own, which an administrator may
     * read too; a grant made up of nothing else is one this device could not
     * read, and names no devices to follow.
     */
    val phones: Map<String, PhoneStatus> = emptyMap(),
)
