package io.github.vitalyostanin.geoshare.map

/** As much of a name as fits on a card. The page cuts it to the same. */
const val LABEL_LIMIT = 40

/** How many names of each kind this phone keeps. The rules say the same. */
private const val PHONE_LABELS = 500
private const val MEMBER_LABELS = 50

/**
 * What this member calls the phones and the people of the group, for
 * themselves alone: "new phone" rather than a model number, "grandma" rather
 * than the name on her account.
 *
 * A phone is named by its full address, account and phone joined by a dot, so
 * that two phones of one model are two names.
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
 * The positions with each phone called what this member called it, else what
 * its owner called it, else its model.
 */
fun labelledPositions(
    positions: List<MemberPosition>,
    labels: Labels,
    grants: Map<String, MemberGrant>,
): List<MemberPosition> = positions.map { position ->
    val deviceId = position.positionId.substringAfter('.')
    val named =
        labels.phones[position.positionId]
            ?: grants[position.memberId]?.phones?.get(deviceId)?.name

    if (named == null || named == position.deviceName) {
        position
    } else {
        position.copy(deviceName = named)
    }
}
