package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Leave
import io.github.vitalyostanin.geoshare.membership.Roster
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant

/** How often the siren banner decides whether the phone is still sounding. */
private const val SIREN_BANNER_TICK_MS = 1_000L

/** What the help button over the map explains: the follow modes and the sources. */
@Composable
internal fun MapHelpSheet(onClose: () -> Unit) {
    HelpSheet(
        title = stringResource(R.string.help_title_map),
        entries =
        listOf(
            HelpEntry(
                R.drawable.ic_follow_none,
                HelpLook.Plain,
                stringResource(R.string.help_follow_none),
            ),
            HelpEntry(
                R.drawable.ic_follow_one,
                HelpLook.Plain,
                stringResource(R.string.help_follow_one),
            ),
            HelpEntry(
                R.drawable.ic_follow_all,
                HelpLook.Plain,
                stringResource(R.string.help_follow_all),
            ),
            HelpEntry(
                R.drawable.ic_sources,
                HelpLook.Plain,
                stringResource(R.string.help_sources),
            ),
        ),
        onClose = onClose,
    )
}

/**
 * Told to the member over the map whenever one of their own phones is being
 * looked for, and whenever the database has gone silent.
 *
 * A member carrying two phones may have both looked for at once, so this is a
 * list rather than one banner.
 */
@Composable
internal fun OwnBanners(
    searches: Map<String, Search>,
    now: Instant,
    ownMemberId: String,
    names: Map<String, String>,
    profiles: Map<String, MemberProfile>,
    ownName: String,
    onFound: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ownSearches =
        remember(searches, now, ownMemberId) {
            standingSearches(searches, now).values.filter { it.memberId == ownMemberId }
        }

    val link = LocalLink.current

    if (ownSearches.isEmpty() && !link.lost) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        if (link.lost) {
            LinkBanner(link)
        }

        ownSearches.forEach { search ->
            SoughtBanner(
                search = search,
                phoneName = names[search.phoneId] ?: search.phoneName ?: shortName(search.phoneId),
                openedByName =
                memberName(
                    profile = profiles[search.openedBy],
                    memberId = search.openedBy,
                    own = search.openedBy == ownMemberId,
                    ownName = ownName,
                ),
                onFound = { onFound(search.phoneId) },
            )
        }
    }
}

/**
 * The siren sounding on this very phone, with the one button that matters
 * while it does; the notification carries the same one for a phone that is
 * locked or showing something else.
 */
@Composable
internal fun OwnSirenBanner(
    ownPhoneId: String?,
    actions: SettingsActions,
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    ownName: String,
    modifier: Modifier = Modifier,
) {
    val ownSirenFlow =
        remember(ownPhoneId) {
            ownPhoneId?.let(actions.sirenOf) ?: flowOf(null)
        }
    val ownSiren by ownSirenFlow.collectAsState(initial = null)
    val sirenNow by produceState(Instant.now(), ownSiren) {
        while (ownSiren != null) {
            value = Instant.now()
            delay(SIREN_BANNER_TICK_MS)
        }
    }
    val sirenScope = rememberCoroutineScope()
    val siren = ownSiren ?: return
    val state = sirenState(siren, sirenNow)

    if (state !is SirenState.Sounding) {
        return
    }

    SirenBanner(
        callerName =
        memberName(
            profile = profiles[siren.calledBy],
            memberId = siren.calledBy,
            own = siren.calledBy == ownMemberId,
            ownName = ownName,
        ),
        until = state.until,
        onStop = { sirenScope.launch { actions.onStopSiren(siren.phoneId) } },
        modifier = modifier,
    )
}

/**
 * Everything under the map: who else has it open, and the strip of members
 * to move it onto, or the line saying there is nobody to show yet.
 */
@Composable
internal fun BelowTheMap(
    lookingNames: List<String>,
    members: List<MemberPosition>,
    names: Map<String, String>,
    faces: Map<String, Bitmap>,
    openCards: Set<String>,
    now: Instant,
    searches: Map<String, Search>,
    showAll: Boolean,
    awaitingTarget: Boolean,
    outOfReach: Int,
    onSelect: (String) -> Unit,
    onShowAll: (Boolean) -> Unit,
    onCentre: (String, Float) -> Unit,
) {
    Column {
        // Above the buttons rather than on a card: an open map is a fact about
        // the household, not about the member whose card happens to be open,
        // and a member with no position of their own still watches.
        if (lookingNames.isNotEmpty()) {
            Text(
                text = stringResource(R.string.map_watching, lookingNames.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (members.isEmpty()) {
            Text(
                text =
                if (outOfReach > 0) {
                    pluralStringResource(R.plurals.map_out_of_reach, outOfReach, outOfReach)
                } else {
                    stringResource(R.string.map_nobody_yet)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
        } else {
            MemberButtons(
                members = members,
                names = names,
                faces = faces,
                openCards = openCards,
                now = now,
                searches = searches,
                showAll = showAll,
                awaitingTarget = awaitingTarget,
                onSelect = onSelect,
                onShowAll = onShowAll,
                modifier = Modifier.navigationBarsPadding(),
                onCentre = onCentre,
            )

            // A member whose position is refused simply has no marker, which
            // reads as a member who is not sharing — and so does a phone that
            // cannot reach the group at all. One line is the difference.
            if (outOfReach > 0) {
                Text(
                    text = pluralStringResource(R.plurals.map_out_of_reach, outOfReach, outOfReach),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * The device list and the admin panel, each drawn over the settings it was
 * opened from, so that the way back out is the screen it was opened from,
 * whichever of the two ways back is used.
 *
 * [runsTheGroup] is weighed again on every draw rather than only when the
 * panel was opened: an administrator can stop being one while it is up —
 * somebody else takes the role away, or this phone is taken off the roster and
 * the refused subscription empties it — and a panel left standing would go on
 * offering writes that are now refused.
 */
@Composable
internal fun OverTheSettings(
    myPhonesOpen: Boolean,
    adminOpen: Boolean,
    runsTheGroup: Boolean,
    settings: SettingsState,
    actions: SettingsActions,
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    ownPhoneId: String?,
    labels: Labels,
    now: Instant,
    searches: Map<String, Search>,
    groups: GroupSwitch,
    onCloseMyPhones: () -> Unit,
    onCloseAdmin: () -> Unit,
) {
    // The phones are read from this member's own grant, which the member
    // always may read.
    if (myPhonesOpen) {
        MyPhonesScreen(
            phones =
            phoneLines(
                ownMemberId,
                settings.grants[ownMemberId]?.phones.orEmpty(),
                labels,
            ),
            ownPhoneId = ownPhoneId,
            now = now,
            sirenOf = actions.sirenOf,
            onCallSiren = actions.onCallSiren,
            onStopSiren = actions.onStopSiren,
            resetOf = actions.resetOf,
            onAskReset = actions.onAskReset,
            onForgetPhone = actions.onForgetPhone,
            groupNames =
            groups.choices.filter { it.leave != Leave.NOT_ON_ROSTER }.map { it.caption },
            searches = searches,
            mayLookFor = runsTheGroup,
            onSearch = actions.onSearch,
            onEndSearch = actions.onEndSearch,
            onClose = onCloseMyPhones,
        )
    }

    if (adminOpen && runsTheGroup) {
        AdminPanel(
            roster = settings.roster ?: Roster(),
            profiles = profiles,
            grants = settings.grants,
            ownMemberId = ownMemberId,
            labels = labels,
            now = now,
            invitation = settings.invitation,
            site = settings.site,
            trouble = settings.groupTrouble,
            searches = searches,
            onMakeInvitation = actions.onMakeInvitation,
            onRemoveMember = actions.onRemoveMember,
            onSetRunning = actions.onSetRunning,
            onRenameGroup = actions.onRenameGroup,
            onSearch = actions.onSearch,
            onEndSearch = actions.onEndSearch,
            ownPhoneId = ownPhoneId,
            sirenOf = actions.sirenOf,
            onCallSiren = actions.onCallSiren,
            onStopSiren = actions.onStopSiren,
            resetOf = actions.resetOf,
            onAskReset = actions.onAskReset,
            invitationStanding = actions.invitationStanding,
            onClose = onCloseAdmin,
        )
    }
}
