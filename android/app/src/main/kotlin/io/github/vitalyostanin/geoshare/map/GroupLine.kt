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
 * profiles every member may read, and the grants this phone has been given.
 * Nothing new travels towards a phone for it, which is why this is the part of
 * the panel that can be built before a phone can be woken.
 */
data class GroupLine(
    val memberId: String,
    /** The name the member published, or nothing where they have published none. */
    val displayName: String?,
    val runsTheGroup: Boolean,
    /** Whether this is the person holding the phone, who is shown first and never removed here. */
    val isYou: Boolean,
    /**
     * How many of this member's phones this one may watch.
     *
     * Zero for a member who shares with somebody else, or with nobody: the
     * grant says which phones there are and who may read it, and a phone
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
     * The member's phones as they last said how they are, whether or not this
     * phone may see where they are: an administrator reads these to tell a
     * phone that went quiet from one whose marker is stuck.
     */
    val phones: List<PhoneLine> = emptyList(),
)

/** One phone of a member, named the way the map names it. */
data class PhoneLine(
    /** The phone's `{uid}.{deviceId}`, the address its siren is called at. */
    val phoneId: String,
    val name: String,
    /** When the phone was last in touch, by the server's clock. */
    val onlineAt: Instant?,
    val batteryPercent: Int?,
    val charging: Boolean,
)

/**
 * A member's phones, each called what the viewer called it, else what its
 * owner called it, else its model, and in the order of those names.
 */
fun phoneLines(
    memberId: String,
    phones: Map<String, PhoneStatus>,
    labels: Labels,
): List<PhoneLine> = phones
    .map { (deviceId, phone) ->
        PhoneLine(
            phoneId = positionId(memberId, deviceId),
            name =
            labels.phones[positionId(memberId, deviceId)]
                ?: phone.name
                ?: phone.model
                ?: deviceId.take(PHONE_ID_SHOWN),
            onlineAt = phone.onlineAt,
            batteryPercent = phone.batteryPercent,
            charging = phone.charging,
        )
    }.sortedBy { it.name }

/** As much of a phone's id as stands in for a name nobody wrote. */
private const val PHONE_ID_SHOWN = 8

/**
 * The group as one list, the person looking at it first.
 *
 * Their own line leads because the panel is read by somebody checking on the
 * others: the first question is whose phone this is, and the rest of the list
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
            phones = phoneLines(memberId, grants[memberId]?.phones.orEmpty(), labels),
        )
    }.sortedWith(compareByDescending(GroupLine::isYou).thenBy { it.displayName ?: it.memberId })
