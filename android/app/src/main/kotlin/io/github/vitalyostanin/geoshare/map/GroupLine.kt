package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.mayBeMadeAdmin
import io.github.vitalyostanin.geoshare.membership.mayBeRemoved
import io.github.vitalyostanin.geoshare.membership.mayStopRunning
import io.github.vitalyostanin.geoshare.membership.runsTheGroup
import io.github.vitalyostanin.geoshare.service.positionId
import java.time.Instant

/**
 * One member as the panel that shows the group whole has to draw them.
 *
 * Everything here is read from what the group already writes: the roster, the
 * profiles every member may read, and the grants this device has been given.
 * Nothing new travels towards a device for it, which is why this is the part of
 * the panel that can be built before a device can be woken.
 */
data class GroupLine(
    val memberId: String,
    /** The name the member published, or nothing where they have published none. */
    val displayName: String?,
    val runsTheGroup: Boolean,
    /** Whether this is the person holding the device, who is shown first and never removed here. */
    val isYou: Boolean,
    /**
     * How many of this member's devices this one may watch.
     *
     * Zero for a member who shares with somebody else, or with nobody: the
     * grant says which devices there are and who may read it, and a device
     * without the permission is told neither. So this counts what is visible
     * from here rather than what the member owns, and the panel says as much.
     */
    val phonesShared: Int,
    /** Whether the administrator looking at the panel may take them off the roster. */
    val removable: Boolean,
    /** Whether the administrator looking at the panel may hand them the running of the group. */
    val mayBeMadeAdmin: Boolean = false,
    /** Whether the administrator looking at the panel may relieve them of it. */
    val mayStopRunning: Boolean = false,
    /**
     * The member's devices as they last said how they are, whether or not this
     * device may see where they are: an administrator reads these to tell a
     * device that went quiet from one whose marker is stuck.
     */
    val phones: List<PhoneLine> = emptyList(),
)

/** One device of a member, named the way the map names it. */
data class PhoneLine(
    /** The device's `{uid}.{deviceId}`, the address its siren is called at. */
    val phoneId: String,
    val name: String,
    /** When the device was last in touch, by the server's clock. */
    val onlineAt: Instant?,
    val batteryPercent: Int?,
    val charging: Boolean,
)

/**
 * A member's devices, in the order of their names. Each is called what the map
 * calls it (phoneNames), else by the start of its id.
 *
 * [seenAt] is when the position this device is shown at was last known, by
 * positionId. A device writes its own state only every so often, while its
 * position is refreshed far more often, so the state alone would say "22
 * minutes ago" under a marker the map calls fresh. The later of the two is
 * taken, as the map does.
 */
fun phoneLines(
    memberId: String,
    phones: Map<String, PhoneStatus>,
    labels: Labels,
    seenAt: Map<String, Instant> = emptyMap(),
): List<PhoneLine> {
    val names = phoneNames(memberId, phones, labels)

    return phones
        .map { (deviceId, phone) ->
            val phoneId = positionId(memberId, deviceId)

            PhoneLine(
                phoneId = phoneId,
                name = names[deviceId] ?: deviceId.take(PHONE_ID_SHOWN),
                onlineAt = listOfNotNull(phone.onlineAt, seenAt[phoneId]).maxOrNull(),
                batteryPercent = phone.batteryPercent,
                charging = phone.charging,
            )
        }.sortedBy { it.name }
}

/** As much of a device's id as stands in for a name nobody wrote. */
private const val PHONE_ID_SHOWN = 8

/**
 * The group as one list, the person looking at it first.
 *
 * Their own line leads because the panel is read by somebody checking on the
 * others: the first question is whose device this is, and the rest of the list
 * is the answer to the second. After that the order is by the name shown, so
 * that a list read twice reads the same way — an order following the roster
 * would follow the order people happened to join in, which no reader can
 * predict.
 */
fun groupOverview(
    roster: Roster,
    profiles: Map<String, MemberProfile>,
    grants: Map<String, MemberGrant>,
    ownMemberId: String,
    labels: Labels = Labels(),
    seenAt: Map<String, Instant> = emptyMap(),
): List<GroupLine> = roster.members
    .map { memberId ->
        GroupLine(
            memberId = memberId,
            displayName = profiles[memberId]?.displayName,
            runsTheGroup = runsTheGroup(roster, memberId),
            isYou = memberId == ownMemberId,
            phonesShared = grants[memberId]?.devices?.size ?: 0,
            // Nobody removes themselves here: leaving a group is not the same
            // act as taking somebody out of it, and the rules refuse a roster
            // left with nobody to run it.
            removable = memberId != ownMemberId && mayBeRemoved(roster, memberId, ownMemberId),
            mayBeMadeAdmin = mayBeMadeAdmin(roster, memberId, ownMemberId),
            mayStopRunning = mayStopRunning(roster, memberId, ownMemberId),
            phones = phoneLines(memberId, grants[memberId]?.phones.orEmpty(), labels, seenAt),
        )
    }.sortedWith(compareByDescending(GroupLine::isYou).thenBy { it.displayName ?: it.memberId })
