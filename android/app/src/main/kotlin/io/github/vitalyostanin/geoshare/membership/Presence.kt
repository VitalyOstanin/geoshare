package io.github.vitalyostanin.geoshare.membership

/** What one look at the group document came back with. */
sealed interface RosterRead {
    /** The document as it stands, or null for a group not made yet. */
    data class Read(val roster: Roster?) : RosterRead

    /** The rules would not show it to this account. */
    data object Refused : RosterRead

    /** No answer at all: the network, or the database, is not there. */
    data object Unreachable : RosterRead
}

/** Whether this device is still in a group it is connected to. */
enum class Presence {
    IN,

    /** It was on the roster and is no longer: an administrator took it off. */
    REMOVED,

    /**
     * The database answered, and the device is not on the roster and never
     * was: a group not made yet, or one it has not joined.
     */
    OUTSIDE,

    /** Nothing said yet, or nothing that tells. */
    UNKNOWN,
}

/**
 * Where [uid] stands after [read], given whether this device has seen it on
 * the roster before.
 *
 * The rules show the group document only to its members, so a device that
 * has not joined yet is refused exactly like one taken off. Having been in is
 * the only thing that tells the two apart.
 */
fun presenceOf(read: RosterRead, uid: String, wasIn: Boolean): Presence = when (read) {
    is RosterRead.Read ->
        when {
            read.roster != null && uid in read.roster.members -> Presence.IN
            wasIn -> Presence.REMOVED
            else -> Presence.OUTSIDE
        }

    RosterRead.Refused -> if (wasIn) Presence.REMOVED else Presence.OUTSIDE

    RosterRead.Unreachable -> Presence.UNKNOWN
}

/**
 * What a device is taken to be in a group before the database has said
 * anything: in it, as far as anybody knows, when it has been seen on the
 * roster before; otherwise nothing until the answer comes.
 *
 * A group once stood in is written into at once, without the network
 * included. One never stood in waits the fraction of a second the answer
 * takes, rather than be written into and refuse it.
 */
fun presenceBeforeAnswer(wasIn: Boolean): Presence? = if (wasIn) Presence.UNKNOWN else null
