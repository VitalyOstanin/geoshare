package io.github.vitalyostanin.geoshare.map

/** How many members the panel shows without being asked for the rest. */
const val MEMBERS_SHOWN = 2

/**
 * How many positions the panel shows as buttons of their own.
 *
 * The button that opens the rest takes the room of a member, so a single
 * member left behind it is shown instead of it: the row costs the same, and
 * the member is reached without the press that opens the list.
 */
fun membersOnTheStrip(count: Int): Int = if (count <= MEMBERS_SHOWN + 1) count else MEMBERS_SHOWN

/**
 * The order the positions are offered in.
 *
 * The other members come first because the panel exists to jump to them; one's
 * own position is already the one the map opens on. Within that, the order is
 * by id rather than by distance or freshness: the panel is a set of buttons,
 * and buttons that swap places between two glances get pressed by mistake.
 *
 * The devices of one member follow each other, because the account is the first
 * part of the id they are sorted by.
 */
fun orderedForNavigation(
    positions: List<MemberPosition>,
    ownMemberId: String,
): List<MemberPosition> = positions.sortedWith(
    compareBy({ it.memberId == ownMemberId }, { it.memberId }, { it.positionId }),
)

/** Enough characters of an id to tell two members apart at a glance. */
fun shortName(memberId: String): String = memberId.take(SHORT_ID_LENGTH)

/**
 * The end of what a member without a published name is called in a line of
 * text, after the words saying they have none: enough to tell two such
 * members apart, and to find them in the console.
 */
fun idTail(memberId: String): String = memberId.take(ID_TAIL_LENGTH)

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
    /**
     * What a member without a name is called, for a line of text; left out
     * where there is room only for a few characters, as on a marker.
     */
    unnamed: String? = null,
): String = if (own) ownName else profile?.displayName ?: unnamed ?: shortName(memberId)

/**
 * Where the map opens: the viewer's own device last heard from, rather than an
 * old phone left at home that happens to come first; anyone's position when
 * the viewer has none on the map.
 */
fun openingPosition(members: List<MemberPosition>, ownMemberId: String): MemberPosition? = members
    .filter { it.memberId == ownMemberId }
    .maxByOrNull { it.knownAt }
    ?: members.firstOrNull()

private const val SHORT_ID_LENGTH = 6
private const val ID_TAIL_LENGTH = 4
