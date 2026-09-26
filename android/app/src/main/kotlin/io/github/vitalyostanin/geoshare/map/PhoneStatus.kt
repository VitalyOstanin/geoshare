package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import io.github.vitalyostanin.geoshare.service.PhoneField
import java.time.Instant

/** How many phones of one member this phone will follow. The rules say the same. */
private const val PHONES_PER_MEMBER = 10

/** As much of a name as fits on a card. The writers cut it to the same. */
private const val NAME_LIMIT = 40

private const val FULL_PERCENT = 100

/**
 * What one phone last said about itself, apart from where it is.
 *
 * Kept beside the position rather than inside it: none of it is a place, and
 * the filter that holds a position back when the answers look forged has
 * nothing to say about whether the phone is in touch or on a cable.
 */
data class PhoneStatus(
    val model: String? = null,
    val name: String? = null,
    /** When the phone was last in touch, by the server's clock. */
    val onlineAt: Instant? = null,
    val mode: String? = null,
    val batteryPercent: Int? = null,
    val charging: Boolean = false,
)

/**
 * The phones in one member's document, by the id of each, with whatever in an
 * entry is not what a phone writes left out: the rules hold the number of
 * phones and not their fields.
 */
fun phonesFrom(data: Map<String, Any?>?): Map<String, PhoneStatus> = data.orEmpty()
    .entries
    .take(PHONES_PER_MEMBER)
    .mapNotNull { (deviceId, entry) ->
        val fields = entry as? Map<*, *> ?: return@mapNotNull null
        val moment = (fields[PhoneField.ONLINE_AT] as? Timestamp)?.let {
            Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong())
        }
        val percent = (fields[PhoneField.BATTERY_PERCENT] as? Number)?.toInt()

        deviceId to
            PhoneStatus(
                model = shortText(fields[PhoneField.MODEL]),
                name = shortText(fields[PhoneField.NAME]),
                onlineAt = moment,
                mode = fields[PhoneField.MODE] as? String,
                batteryPercent = percent?.takeIf { it in 0..FULL_PERCENT },
                charging = fields[PhoneField.CHARGING] == true,
            )
    }.toMap()

private fun shortText(value: Any?): String? =
    (value as? String)?.takeIf { it.isNotEmpty() }?.take(NAME_LIMIT)

/**
 * The positions with what their phones said about themselves laid over them,
 * where a phone said so after its position was written.
 *
 * The later of the two wins: a position carries the charge it was written
 * with, and while the answers are being thrown away that is the charge of an
 * hour ago. The moment the phone was last in touch moves on with it, which is
 * what keeps the marker of a working phone from going grey merely because its
 * position is being held back.
 */
fun withPhones(
    positions: List<MemberPosition>,
    grants: Map<String, MemberGrant>,
): List<MemberPosition> = positions.map { position ->
    val deviceId = position.positionId.substringAfter('.')
    val phone = grants[position.memberId]?.phones?.get(deviceId)
    val onlineAt = phone?.onlineAt

    if (onlineAt == null || !onlineAt.isAfter(position.knownAt)) {
        position
    } else {
        position.copy(
            writtenAt = onlineAt,
            mode = phone.mode ?: position.mode,
            batteryPercent = phone.batteryPercent ?: position.batteryPercent,
            charging = phone.charging,
        )
    }
}

/**
 * What the rest of the application reads as a member's grant: the phones to
 * follow are those the grant names together with those the member's own
 * document names, and only where the grant could be read. The grant is what
 * says this phone may watch; an administrator reads everybody's phones without
 * being allowed to see where any of them is. A grant is still read for its list
 * because a phone of an older build names itself there and nowhere else.
 */
fun combinedGrants(
    named: Map<String, MemberGrant>,
    phones: Map<String, Map<String, PhoneStatus>>,
): Map<String, MemberGrant> = (named.keys + phones.keys).associateWith { memberId ->
    val own = phones[memberId].orEmpty()
    val grant = named[memberId]

    if (grant == null) {
        MemberGrant(memberId = memberId, phones = own)
    } else {
        grant.copy(devices = (grant.devices + own.keys).distinct(), phones = own)
    }
}
