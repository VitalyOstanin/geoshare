package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.service.positionId

/** As much of a name as fits on a card. The page cuts it to the same. */
const val LABEL_LIMIT = 40

/** How many names of each kind this device keeps. The rules say the same. */
private const val PHONE_LABELS = 500
private const val MEMBER_LABELS = 50

/**
 * What this member calls the devices and the people of the group, for
 * themselves alone: "new device" rather than a model number, "grandma" rather
 * than the name on her account.
 *
 * A device is named by its full address, account and device joined by a dot, so
 * that two devices of one model are two names.
 */
data class Labels(
    val phones: Map<String, String> = emptyMap(),
    val members: Map<String, String> = emptyMap(),
)

/**
 * The names out of the two maps of the document, with whatever is not a name
 * left out: the rules hold how many there are and not what each one is.
 */
fun labelsFrom(phones: Any?, members: Any?): Labels = Labels(
    phones = namesFrom(phones, PHONE_LABELS),
    members = namesFrom(members, MEMBER_LABELS),
)

private fun namesFrom(value: Any?, limit: Int): Map<String, String> = (value as? Map<*, *>)
    .orEmpty()
    .entries
    .mapNotNull { (key, name) ->
        val address = key as? String ?: return@mapNotNull null
        val text = labelText(name as? String ?: return@mapNotNull null) ?: return@mapNotNull null

        address to text
    }.take(limit)
    .toMap()

/** What is kept of a name somebody typed: nothing for an empty one, which resets the name. */
fun labelText(typed: String): String? = typed.trim().take(LABEL_LIMIT).ifEmpty { null }

/** The profiles with the names this member gave the people put in place of their own. */
fun labelledProfiles(
    profiles: Map<String, MemberProfile>,
    labels: Labels,
): Map<String, MemberProfile> = profiles.mapValues { (memberId, profile) ->
    labels.members[memberId]?.let { profile.copy(displayName = it) } ?: profile
}

/**
 * A member's devices, each called what this member called it, else what its
 * owner called it, else its model; null for a device that has none of them.
 *
 * Devices left with one name between them — two of one model nobody named —
 * are told apart by a number after it, counted in the order of their ids, so
 * that the map, the panel and the application all number them alike.
 *
 * `phones` is what the member's devices say about themselves, by device id;
 * `seen` is a last name for a device known only from its position, or null.
 */
fun phoneNames(
    memberId: String,
    phones: Map<String, PhoneStatus>,
    labels: Labels,
    seen: Map<String, String?> = emptyMap(),
): Map<String, String?> {
    val names = (phones.keys + seen.keys).associateWith { deviceId ->
        val phone = phones[deviceId]

        labels.phones[positionId(memberId, deviceId)]
            ?: phone?.name
            ?: phone?.model
            ?: seen[deviceId]
    }

    val sharing = mutableMapOf<String, MutableList<String>>()

    for ((deviceId, name) in names) {
        if (name != null) {
            sharing.getOrPut(name) { mutableListOf() }.add(deviceId)
        }
    }

    val numbered = names.toMutableMap()
    // A number is skipped where it would give a name another device already
    // has: "Pixel (1)" named so by its owner stays the only one.
    val taken = sharing.filterValues { it.size == 1 }.keys.toMutableSet()

    for ((name, deviceIds) in sharing) {
        if (deviceIds.size > 1) {
            var number = 0

            for (deviceId in deviceIds.sorted()) {
                do {
                    number++
                } while ("$name ($number)" in taken)

                taken += "$name ($number)"
                numbered[deviceId] = "$name ($number)"
            }
        }
    }

    return numbered
}

/**
 * The positions with each device called what phoneNames calls it.
 *
 * A position carries no name of its own: the model lives in the device's own
 * document, and a device that has not written one yet stays unnamed.
 *
 * [searches] carries a further fallback, below the grant: a device under a
 * standing search may be read by somebody its owner never shared with, who has
 * neither a label of their own for it nor the grant that would say what its
 * owner called it — the search is all such a reader has, and
 * [Search.phoneName] is what it carries for exactly that reason.
 */
fun labelledPositions(
    positions: List<MemberPosition>,
    labels: Labels,
    grants: Map<String, MemberGrant>,
    searches: Map<String, Search> = emptyMap(),
): List<MemberPosition> {
    val seen = mutableMapOf<String, MutableMap<String, String?>>()

    for (position in positions) {
        val deviceId = position.positionId.substringAfter('.')
        val models = seen.getOrPut(position.memberId) { mutableMapOf() }

        models[deviceId] = searches[position.positionId]?.phoneName?.takeIf { it.isNotBlank() }
    }

    val names = seen.mapValues { (memberId, models) ->
        phoneNames(memberId, grants[memberId]?.phones ?: emptyMap(), labels, models)
    }

    return positions.map { position ->
        val deviceId = position.positionId.substringAfter('.')
        val named = names.getValue(position.memberId)[deviceId]

        if (named == null || named == position.deviceName) {
            position
        } else {
            position.copy(deviceName = named)
        }
    }
}
