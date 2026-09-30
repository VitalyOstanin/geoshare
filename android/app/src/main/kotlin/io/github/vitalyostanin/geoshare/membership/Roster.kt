package io.github.vitalyostanin.geoshare.membership

// Who is in the group, and what that lets them do.
//
// The roster used to be an array only the console could change. It is now a
// document the members themselves write, within what the rules allow, so the
// application has to know which of the four positions the person in front of
// it is standing in.

/**
 * Who is in the group and who runs it, as the group document holds them.
 *
 * [founder] is whoever the document names as its owner, or else the account
 * the group is named after — the rules read it the same way. It runs the group
 * without being listed among [admins], and nobody else takes it off the
 * roster.
 *
 * [name] is what people call the group (ADR-0016): given when it is founded,
 * changed later by an administrator, and absent until either happens.
 */
data class Roster(
    val members: List<String> = emptyList(),
    val admins: List<String> = emptyList(),
    val founder: String? = null,
    val name: String? = null,
)

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
    runsTheGroup(roster, uid) -> Standing.Administrator
    roster.admins.isEmpty() && roster.founder !in roster.members -> Standing.Unrun
    else -> Standing.Member
}

/**
 * Whether a member runs the group: named among its administrators, or its
 * founder.
 *
 * The founder runs it without being named, the way the rules have it: the
 * person who owns the project reads the whole database from the console
 * anyway, and a list of administrators that left them out would only take the
 * screens away from them. On the roster, though — leaving the group takes the
 * power with it.
 */
fun runsTheGroup(roster: Roster, uid: String): Boolean =
    uid in roster.members && (uid in roster.admins || uid == roster.founder)

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
    founder = roster.founder,
    name = roster.name,
)

/**
 * Whether a member may be taken off the roster by the administrator [by]
 * looking at it.
 *
 * Asked the way the rules ask it, so the screen offers nothing a write would
 * be refused:
 * - not the last administrator while the founder is not there to run the
 *   group: a group nobody runs is a group nobody can add to or remove from,
 *   which is the console again;
 * - not the founder, by anybody but themselves;
 * - not another administrator, by anybody but the founder — anybody else
 *   first takes the role, and the place is a second write.
 */
fun mayBeRemoved(roster: Roster, uid: String, by: String? = null): Boolean {
    val byFounder = roster.founder != null && by == roster.founder

    val left = rosterWithout(roster, uid)

    return uid in roster.members &&
        (uid != roster.founder || byFounder) &&
        (uid !in roster.admins || byFounder) &&
        (left.admins.isNotEmpty() || roster.founder in left.members)
}

/**
 * Whether a member may be handed the running of the group by [by]: anybody who
 * runs it hands it to anybody on the roster who does not run it yet.
 */
fun mayBeMadeAdmin(roster: Roster, uid: String, by: String): Boolean =
    runsTheGroup(roster, by) && uid in roster.members && !runsTheGroup(roster, uid)

/**
 * Whether a member may be relieved of running the group by [by].
 *
 * Asked the way the rules ask it:
 * - the founder is not offered: they run the group without being named, so
 *   taking the name away would change nothing on the screen, and nobody else
 *   may take it;
 * - the last administrator keeps the role while the founder is not there to
 *   run the group, for the reason the last one is not removed either.
 *
 * An administrator may give up the role themselves; the screen asks twice,
 * since only another administrator can hand it back.
 */
fun mayStopRunning(roster: Roster, uid: String, by: String): Boolean = runsTheGroup(roster, by) &&
    uid in roster.admins &&
    uid != roster.founder &&
    (roster.admins.any { it != uid } || roster.founder in roster.members)
