package io.github.vitalyostanin.geoshare.map

/** How many members the panel shows without being asked for the rest. */
const val MEMBERS_SHOWN = 2

/**
 * The order the members are offered in.
 *
 * The other members come first because the panel exists to jump to them; one's
 * own position is already the one the map opens on. Within that, the order is
 * by id rather than by distance or freshness: the panel is a set of buttons,
 * and buttons that swap places between two glances get pressed by mistake.
 */
fun orderedForNavigation(
    positions: List<MemberPosition>,
    ownMemberId: String,
): List<MemberPosition> =
    positions.sortedWith(compareBy({ it.memberId == ownMemberId }, { it.memberId }))

/** Enough characters of an id to tell two members apart at a glance. */
fun shortName(memberId: String): String = memberId.take(SHORT_ID_LENGTH)

/**
 * What a member is called on screen, which the web page works out the same way.
 *
 * One's own marker says "you" rather than one's own name: the name is on the
 * account button in the corner already, and on a map of a household the
 * question is where everybody else is. A member who has not signed in since
 * profiles existed has no name to show, and the first characters of the id are
 * what told them apart before.
 */
fun memberName(
    profile: MemberProfile?,
    memberId: String,
    own: Boolean,
    /** What one's own marker says; it comes from the resources, which this file does not read. */
    ownName: String,
): String = if (own) ownName else profile?.displayName ?: shortName(memberId)

private const val SHORT_ID_LENGTH = 6
