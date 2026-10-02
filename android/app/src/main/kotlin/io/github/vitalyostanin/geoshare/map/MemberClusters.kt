package io.github.vitalyostanin.geoshare.map

/**
 * A marker as the map has placed it: which position it stands for, and where
 * on the screen its pin has come out.
 *
 * Screen coordinates rather than degrees, because whether two pins cover one
 * another is a question about the drawing and not about the ground: two people
 * in one flat are one pin at every zoom, and the same two are far apart once
 * the map is close enough.
 */
data class PlacedMember(val id: String, val x: Double, val y: Double)

/**
 * Positions whose pins would land on top of each other, as the one thing that
 * gets drawn for them.
 *
 * The first id is the one the group is gathered around; the rest follow in the
 * order their ids sort in, which is what keeps the faces in a group from
 * swapping places between two ticks.
 */
data class MemberCluster(val ids: List<String>)

/**
 * Gathers the positions whose pins overlap.
 *
 * Anchored rather than chained: a member joins the group they are within reach
 * of, and a third one further along starts a group of their own instead of
 * dragging the first two across the map. A chain would let a row of members
 * standing a pin apart each collapse into a single marker somewhere in the
 * middle of them all.
 *
 * The order is taken from the ids rather than from the list, because the list
 * comes from the database and can arrive in any order; the same members in the
 * same places have to produce the same groups every tick, or the map would
 * redraw itself for nothing.
 *
 * [alone] is the member whose card is open, kept out of every group: the
 * circle around them is drawn where they are, and a pin standing in the middle
 * of a crowd would point somewhere else.
 */
fun clusterMembers(
    placed: List<PlacedMember>,
    radius: Double,
    alone: String? = null,
): List<MemberCluster> {
    val ordered = placed.sortedBy { it.id }
    val taken = mutableSetOf<String>()
    val clusters = mutableListOf<MemberCluster>()

    for (anchor in ordered) {
        if (!taken.add(anchor.id)) {
            continue
        }

        val group = mutableListOf(anchor.id)

        for (other in ordered) {
            if (anchor.id == alone || other.id == alone) {
                continue
            }

            if (other.id in taken || !within(anchor, other, radius)) {
                continue
            }

            taken.add(other.id)
            group.add(other.id)
        }

        clusters.add(MemberCluster(group))
    }

    return clusters
}

/** Squared on both sides, to keep a root out of a comparison that does not need one. */
private fun within(anchor: PlacedMember, other: PlacedMember, radius: Double): Boolean {
    val dx = anchor.x - other.x
    val dy = anchor.y - other.y

    return dx * dx + dy * dy <= radius * radius
}
