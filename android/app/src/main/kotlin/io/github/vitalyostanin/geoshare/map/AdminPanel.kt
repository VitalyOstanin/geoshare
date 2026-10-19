package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import io.github.vitalyostanin.geoshare.service.deviceIdOf
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

/**
 * The group seen whole, by the member who runs it.
 *
 * A screen of its own rather than more of the settings: what it answers is not
 * "how does this device behave" but "what does the household look like from
 * here", and the two questions are asked months apart by different people.
 *
 * Everything on it is read from what the group already writes — the roster,
 * the profiles, the grants this device was given — so nothing new has to travel
 * towards anybody's device for it to be drawn. The commands that do reach into
 * another device belong here too, and they wait on being able to wake a device
 * that is not listening.
 */
@Composable
fun AdminPanel(
    roster: Roster,
    profiles: Map<String, MemberProfile>,
    /** What each member gives out, as far as this device has been let see. */
    grants: Map<String, MemberGrant>,
    ownMemberId: String,
    /** What this member calls the devices, for themselves. */
    labels: Labels,
    /** The clock the ages on the panel are read against. */
    now: Instant,
    /** The invitation just made, held until the screen is left. */
    invitation: MadeInvitation?,
    /** This device's Hosting site, which the invitation link is built against. */
    site: String?,
    /** Whether the last write to the group was refused. */
    trouble: Boolean,
    /** The group's searches, by the device each is looking for. */
    searches: Map<String, Search>,
    /** When each device's shown position was last known, so a device's age agrees with the map. */
    seenAt: Map<String, Instant>,
    /** Makes an invitation good for the address typed into the dialog that asks for one. */
    onMakeInvitation: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetRunning: (String, Boolean) -> Unit,
    /** Gives the group a name, or changes the one it has; answers how the write came out. */
    onRenameGroup: suspend (String) -> WriteOutcome,
    /**
     * Opens a search for one device, or renews one already standing; answers
     * how the write came out.
     */
    onSearch: suspend (
        phoneId: String,
        phoneName: String?,
        renew: Boolean,
        hours: Int,
    ) -> WriteOutcome,
    /** Ends a search: the device has been found. */
    onEndSearch: suspend (phoneId: String) -> Unit,
    /** This device's own `{uid}.{deviceId}`: the one device nobody here rings. */
    ownPhoneId: String?,
    /** A device's siren as the database has it, followed for as long as asked. */
    sirenOf: (phoneId: String) -> Flow<Siren?>,
    /** Rings a device; answers how the write came out: done, refused or unanswered. */
    onCallSiren: suspend (phoneId: String) -> WriteOutcome,
    onStopSiren: suspend (phoneId: String) -> Unit,
    /** A device's request to forget its place as the database has it, followed for as long as asked. */
    resetOf: (phoneId: String) -> Flow<Reset?>,
    /** Asks a device to forget its place; answers how the write came out. */
    onAskReset: suspend (phoneId: String) -> WriteOutcome,
    /** Whether an invitation made here still stands, followed for as long as asked. */
    invitationLive: (code: String) -> Flow<Boolean?>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = groupOverview(roster, profiles, grants, ownMemberId, labels, seenAt)
    val liveSearches = remember(searches, now) { standingSearches(searches, now) }
    // Read once and used both at the top of the panel and in the invitation
    // dialog's own title: what the invitation names is the same group this
    // screen is already showing, under the same caption.
    val caption = groupCaption(roster, profiles, stringResource(R.string.group_name_unnamed))
    val state = rememberAdminPanelState()
    val scope = rememberCoroutineScope()
    val live = rememberAdminLive(lines, ownPhoneId, sirenOf, resetOf)
    val invitationSection = AdminInvitationState(invitation, invitationLive, site, trouble)
    val shown =
        AdminShown(
            roster = roster,
            profiles = profiles,
            lines = lines,
            liveSearches = liveSearches,
            caption = caption,
            ownMemberId = ownMemberId,
            ownPhoneId = ownPhoneId,
            now = now,
            invitationSection = invitationSection,
            live = live,
        )
    val writes = AdminWrites(onRenameGroup, onSearch, onCallSiren, onAskReset)

    AdminPanelBody(shown, state, scope, onSetRunning, onEndSearch, onStopSiren, onClose, modifier)
    AdminPanelDialogs(state, shown, scope, writes, onMakeInvitation, onRemoveMember, onSetRunning)
}

/**
 * What the row says of the call this screen made: which device, the seconds
 * left while it sounds, or that it never answered or was stopped. Nothing
 * once it sounded out, nor [SIREN_TOLD] after it ended any other way.
 */
@Composable
internal fun sirenText(siren: Siren, state: SirenState, phones: List<PhoneLine>): String? {
    // A call long over says nothing any more: a row is about now.
    if (!sirenTold(siren, Instant.now())) {
        return null
    }

    val phone =
        phones.firstOrNull { it.phoneId == siren.phoneId }?.name
            ?: shortName(deviceIdOf(siren.phoneId))

    return when (state) {
        SirenState.Waiting -> stringResource(R.string.siren_status_waiting, phone)

        is SirenState.Sounding ->
            stringResource(
                R.string.siren_status_sounding,
                phone,
                Duration.between(Instant.now(), state.until).seconds.coerceAtLeast(0),
            )

        SirenState.NoAnswer -> stringResource(R.string.siren_status_no_answer, phone)

        is SirenState.Stopped -> stringResource(R.string.siren_status_stopped, phone)

        SirenState.Over -> null
    }
}

/** What the clipboard calls what it is holding, where a system shows the name. */
internal const val CLIPBOARD_SAYS = "geoshare"
