package io.github.vitalyostanin.geoshare.map

/**
 * What the markers and the cards call each position: the person on a card, and
 * on a marker the person with the device beside them where it matters.
 *
 * Worked out apart from the map: the fallbacks come from the resources, and
 * the map draws pictures, not strings from a resource file. A member carrying
 * two devices gets two markers, and a pair of markers with the same name on
 * them says nothing about which device is where; the name of the device is
 * added only then, because most members have one.
 */
internal data class ShownNames(
    /** The person each position belongs to, by position id. */
    val people: Map<String, String>,
    /** What each marker says, by position id. */
    val markers: Map<String, String>,
)

internal fun shownNames(
    positions: List<MemberPosition>,
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    /** What one's own marker says; it comes from the resources. */
    ownName: String,
): ShownNames {
    val carried = positions.groupingBy { it.memberId }.eachCount()
    val people =
        positions.associate { position ->
            position.positionId to
                memberName(
                    profile = profiles[position.memberId],
                    memberId = position.memberId,
                    own = position.memberId == ownMemberId,
                    ownName = ownName,
                )
        }
    val markers =
        positions.associate { position ->
            val name = people.getValue(position.positionId)
            val device = position.deviceName

            position.positionId to
                if (carried.getOrDefault(position.memberId, 1) > 1 && !device.isNullOrBlank()) {
                    "$name · $device"
                } else {
                    name
                }
        }

    return ShownNames(people, markers)
}
