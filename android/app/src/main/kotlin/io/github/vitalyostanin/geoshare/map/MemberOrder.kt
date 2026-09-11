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

private const val SHORT_ID_LENGTH = 6
