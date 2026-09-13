package io.github.vitalyostanin.geoshare.membership

// Who is in the group, and what that lets them do.
//
// The roster used to be an array only the console could change. It is now a
// document the members themselves write, within what the rules allow, so the
// application has to know which of the four positions the person in front of
// it is standing in.

/** Who is in the group and who runs it, as the group document holds them. */
data class Roster(val members: List<String> = emptyList(), val admins: List<String> = emptyList())

/** Where the person signed in stands with respect to the group. */
enum class Standing {
    /** No group has been made yet: this account can make one. */
    Ungrouped,

    /** A group exists and this account is not on its roster. */
    Outsider,

    /** On the roster, and that is all: they see the group, not its workings. */
    Member,

    /** On the roster and running it: invitations are theirs to make. */
    Administrator,

    /**
     * On the roster of a group nobody runs.
     *
     * The household that started before invitations existed names no
     * administrators, and the rules let nobody change such a roster. Saying so
     * is better than offering controls that every write would be refused.
     */
    Unrun,
}

/** Where this account stands, given the group document as it was read. */
fun standingIn(roster: Roster?, uid: String): Standing = when {
    roster == null -> Standing.Ungrouped
    uid !in roster.members -> Standing.Outsider
    uid in roster.admins -> Standing.Administrator
    roster.admins.isEmpty() -> Standing.Unrun
    else -> Standing.Member
}

/**
 * The roster a founder writes, which is themselves and nothing else.
 *
 * A group made around anybody else would be a group somebody was put in
 * without asking, and the rules refuse one.
 */
fun rosterFounded(uid: String): Roster = Roster(members = listOf(uid), admins = listOf(uid))

/**
 * The roster without a member, and without them running it either.
 *
 * Leaving the administrators alone would leave somebody who is no longer in
 * the group still able to change who is, which the rules refuse outright: the
 * write would fail rather than half succeed.
 */
fun rosterWithout(roster: Roster, uid: String): Roster = Roster(
    members = roster.members.filterNot { it == uid },
    admins = roster.admins.filterNot { it == uid },
)

/**
 * Whether a member may be taken off the roster by the administrator looking at
 * it.
 *
 * Not the last administrator, and not while they are the only one: a group
 * whose administrators have all gone is a group nobody can add to or remove
 * from, which is the console again.
 */
fun mayBeRemoved(roster: Roster, uid: String): Boolean =
    uid in roster.members && rosterWithout(roster, uid).admins.isNotEmpty()
