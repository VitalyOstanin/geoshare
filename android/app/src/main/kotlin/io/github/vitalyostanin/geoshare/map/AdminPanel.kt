package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.ADDRESS_LIMIT
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.looksLikeAnAddress
import io.github.vitalyostanin.geoshare.membership.runsTheGroup
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * The group seen whole, by the member who runs it.
 *
 * A screen of its own rather than more of the settings: what it answers is not
 * "how does this phone behave" but "what does the household look like from
 * here", and the two questions are asked months apart by different people.
 *
 * Everything on it is read from what the group already writes — the roster,
 * the profiles, the grants this phone was given — so nothing new has to travel
 * towards anybody's phone for it to be drawn. The commands that do reach into
 * another phone belong here too, and they wait on being able to wake a phone
 * that is not listening.
 */
@Composable
fun AdminPanel(
    roster: Roster,
    profiles: Map<String, MemberProfile>,
    /** What each member gives out, as far as this phone has been let see. */
    grants: Map<String, MemberGrant>,
    ownMemberId: String,
    /** What this member calls the phones, for themselves. */
    labels: Labels,
    /** The clock the ages on the panel are read against. */
    now: Instant,
    /** The invitation just made, held until the screen is left. */
    invitation: MadeInvitation?,
    /** This phone's Hosting site, which the invitation link is built against. */
    site: String?,
    /** Whether the last write to the group was refused. */
    trouble: Boolean,
    /** The group's searches, by the phone each is looking for. */
    searches: Map<String, Search>,
    /** Makes an invitation good for the address typed into the dialog that asks for one. */
    onMakeInvitation: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetRunning: (String, Boolean) -> Unit,
    /** Gives the group a name, or changes the one it has; answers how the write came out. */
    onRenameGroup: suspend (String) -> WriteOutcome,
    /**
     * Opens a search for one phone, or renews one already standing; answers
     * how the write came out.
     */
    onSearch: suspend (
        phoneId: String,
        phoneName: String?,
        renew: Boolean,
        hours: Int,
    ) -> WriteOutcome,
    /** Ends a search: the phone has been found. */
    onEndSearch: suspend (phoneId: String) -> Unit,
    /** This phone's own `{uid}.{deviceId}`: the one phone nobody here rings. */
    ownPhoneId: String?,
    /** A phone's siren as the database has it, followed for as long as asked. */
    sirenOf: (phoneId: String) -> Flow<Siren?>,
    /** Rings a phone; answers how the write came out: done, refused or unanswered. */
    onCallSiren: suspend (phoneId: String) -> WriteOutcome,
    onStopSiren: suspend (phoneId: String) -> Unit,
    /** A phone's request to forget its place as the database has it, followed for as long as asked. */
    resetOf: (phoneId: String) -> Flow<Reset?>,
    /** Asks a phone to forget its place; answers how the write came out. */
    onAskReset: suspend (phoneId: String) -> WriteOutcome,
    /** Whether an invitation made here still stands, followed for as long as asked. */
    invitationStanding: (code: String) -> Flow<Boolean?>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = groupOverview(roster, profiles, grants, ownMemberId, labels)
    val standing = remember(searches, now) { standingSearches(searches, now) }
    // Read once and used both at the top of the panel and in the invitation
    // dialog's own title: what the invitation names is the same group this
    // screen is already showing, under the same caption.
    val caption = groupCaption(roster, profiles, stringResource(R.string.group_name_unnamed))
    var givingUp by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    // The dialog for the group's own name: open or not, what is typed in it
    // so far, and how the last write to it came out.
    var renaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var renameTrouble by remember { mutableStateOf<WriteOutcome?>(null) }
    var renameBusy by remember { mutableStateOf(false) }
    // The dialog that asks for the address to invite: open or not, and what
    // has been typed into it so far. Reopened by "make another invitation" as
    // well as by the button that offers the first one, so it is held apart
    // from [invitation] rather than inferred from it.
    var inviting by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    // The member about to be removed and the name the dialog calls them by.
    var removing by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Which phone is being asked about, in which of the two ways, and the
    // length picked so far -- held apart from whether the write is refused,
    // so a refusal on one phone's search does not linger once a different one
    // is opened.
    val searching = rememberAskedWrite<SearchAsk>()
    var searchHours by remember { mutableStateOf(DEFAULT_SEARCH_HOURS) }
    val scope = rememberCoroutineScope()

    // The siren of every phone this panel offers to ring, followed while it
    // is open: a call rung from anywhere else shows on its row as well.
    val ringable =
        remember(lines, ownPhoneId) {
            lines.flatMap { it.phones }.map { it.phoneId }.filter { it != ownPhoneId }.toSet()
        }
    val sirens by remember(ringable) { sirensOf(ringable, sirenOf) }
        .collectAsState(initial = emptyMap())
    val ringing = rememberAskedWrite<SirenAsk>()
    val sirenNow by sirenClock(now, sirens)

    // The requests to forget a place on the same phones, followed the same
    // way, so a reset asked from anywhere shows on its row too.
    val resets by remember(ringable) { resetsOf(ringable, resetOf) }
        .collectAsState(initial = emptyMap())
    val resetting = rememberAskedWrite<PhoneLine>()

    fun writeRename() {
        val name = groupNameText(renameText) ?: return

        renameBusy = true
        scope.launch {
            val outcome = onRenameGroup(name)

            renameBusy = false

            if (outcome == WriteOutcome.DONE) {
                renaming = false
                renameTrouble = null
            } else {
                renameTrouble = outcome
            }
        }
    }

    fun askSearch(phone: PhoneLine, memberName: String, renew: Boolean) {
        searching.ask(SearchAsk(phone, memberName, renew))
        searchHours = DEFAULT_SEARCH_HOURS
    }

    Surface(modifier = modifier.fillMaxSize()) {
        // Scrolled and inset the way the settings screen is, and for the same
        // reasons: one row per member runs off a phone, and the title has to
        // clear the clock.
        Column(
            modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                if (runsTheGroup(roster, ownMemberId)) {
                    TextButton(
                        onClick = {
                            renameText = roster.name.orEmpty()
                            renameTrouble = null
                            renaming = true
                        },
                    ) {
                        Text(text = stringResource(R.string.group_name_rename))
                    }
                }

                HelpButton(onClick = { helpOpen = true })

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
            }

            HorizontalDivider()

            for (line in lines) {
                val lineName = line.displayName ?: unnamedMember(line.memberId)

                AdminMemberRow(
                    line = line,
                    now = now,
                    searches = standing,
                    ownPhoneId = ownPhoneId,
                    onMakeAdmin = { onSetRunning(line.memberId, true) },
                    onStopRunning = {
                        if (line.isYou) givingUp = true else onSetRunning(line.memberId, false)
                    },
                    onRemove = {
                        removing =
                            line.memberId to
                            lineName
                    },
                    onSearch = { phone ->
                        askSearch(
                            phone,
                            lineName,
                            renew = false,
                        )
                    },
                    onEndSearch = { phoneId -> scope.launch { onEndSearch(phoneId) } },
                    sirens = sirens,
                    sirenNow = sirenNow,
                    onSiren = { phone ->
                        ringing.ask(
                            SirenAsk(
                                name = lineName,
                                phones = listOf(phone),
                                selected = phone.phoneId,
                            ),
                        )
                    },
                    onStopSiren = { phoneId -> scope.launch { onStopSiren(phoneId) } },
                    resets = resets,
                    onReset = { phone -> resetting.ask(phone) },
                )
            }

            // Every search standing now, under the rows rather than in them,
            // with the one thing the phone's own row does not offer: renewing
            // it without ending it first.
            for (search in standing.values) {
                val phone = phoneLine(search, lines)
                val soughtName =
                    lines.firstOrNull { it.memberId == search.memberId }?.displayName
                        ?: unnamedMember(search.memberId)

                AdminSearchSummary(
                    search = search,
                    phoneName = phone.name,
                    memberName = soughtName,
                    openedByYou = search.openedBy == ownMemberId,
                    openedByName =
                    profiles[search.openedBy]?.displayName
                        ?: unnamedMember(search.openedBy),
                    onRenew = {
                        askSearch(
                            phone,
                            soughtName,
                            renew = true,
                        )
                    },
                    onEndSearch = { scope.launch { onEndSearch(search.phoneId) } },
                )
            }

            HorizontalDivider()

            // Keyed as a whole: collectAsState keeps the last value across a
            // new flow, and a fresh code would go on reading as the spent one.
            val invitationLeft by
                key(invitation?.code) {
                    remember {
                        invitation?.let { invitationStanding(it.code) } ?: flowOf(null)
                    }.collectAsState(initial = null)
                }

            AdminInvitation(
                invitation = invitation,
                standing = invitationLeft,
                now = now,
                link =
                invitation?.let {
                    invitationLinkText(
                        made = it,
                        site = site,
                        inviter = profiles[ownMemberId]?.displayName,
                        groupName = roster.name,
                    )
                },
                onOpenInvite = {
                    inviteEmail = ""
                    inviting = true
                },
            )

            if (trouble) {
                Text(
                    text = stringResource(R.string.group_refused),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (helpOpen) {
        AdminHelpSheet(onClose = { helpOpen = false })
    }

    if (renaming) {
        RenameGroupDialog(
            text = renameText,
            onTextChange = { renameText = it },
            trouble = renameTrouble,
            busy = renameBusy,
            onSave = ::writeRename,
            onDismiss = { renaming = false },
        )
    }

    if (inviting) {
        InviteDialog(
            caption = caption,
            email = inviteEmail,
            onEmailChange = { inviteEmail = it },
            onCreate = { address ->
                inviting = false
                onMakeInvitation(address)
            },
            onDismiss = { inviting = false },
        )
    }

    if (givingUp) {
        GiveUpDialog(
            onGiveUp = {
                givingUp = false
                onSetRunning(ownMemberId, false)
            },
            onDismiss = { givingUp = false },
        )
    }

    removing?.let { (memberId, name) ->
        RemoveMemberDialog(
            name = name,
            onRemove = {
                removing = null
                onRemoveMember(memberId)
            },
            onDismiss = { removing = null },
        )
    }

    ringing.asked?.let { ask ->
        SirenDialog(
            memberName = ask.name,
            phones = ask.phones,
            selected = ask.selected,
            onSelect = { ringing.change(ask.copy(selected = it)) },
            now = now,
            trouble = ringing.trouble,
            busy = ringing.busy,
            onGo = { ringing.write { onCallSiren(it.selected) } },
            onCancel = ringing::cancel,
        )
    }

    resetting.asked?.let { phone ->
        ResetDialog(
            phoneName = phone.name,
            trouble = resetting.trouble,
            busy = resetting.busy,
            onGo = { resetting.write { onAskReset(it.phoneId) } },
            onCancel = resetting::cancel,
        )
    }

    searching.asked?.let { ask ->
        SearchDialog(
            memberName = ask.memberName,
            phoneName = ask.phone.name,
            renew = ask.renew,
            hours = searchHours,
            onHoursChange = { searchHours = it },
            now = now,
            trouble = searching.trouble,
            busy = searching.busy,
            onGo = {
                searching.write { onSearch(it.phone.phoneId, it.phone.name, it.renew, searchHours) }
            },
            onCancel = searching::cancel,
        )
    }
}

/**
 * What the row says of the call this screen made: which phone, the seconds
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
            ?: siren.phoneId.substringAfter('.').take(NAME_SHOWN_INSTEAD)

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

/** Which phone is being asked about, whose member it is, and in which of the two ways. */
private data class SearchAsk(val phone: PhoneLine, val memberName: String, val renew: Boolean)

/** Whose phone is about to be rung, and which of their phones is picked. */
private data class SirenAsk(val name: String, val phones: List<PhoneLine>, val selected: String)

/**
 * The phone a standing search is about, out of what this screen already knows
 * of the member's phones — or, failing that, out of the search document
 * itself, for a phone whose own status has not reached this screen.
 */
private fun phoneLine(search: Search, lines: List<GroupLine>): PhoneLine = lines
    .firstOrNull { it.memberId == search.memberId }
    ?.phones
    ?.firstOrNull { it.phoneId == search.phoneId }
    ?: PhoneLine(
        phoneId = search.phoneId,
        name = search.phoneName ?: search.phoneId.substringAfter('.').take(NAME_SHOWN_INSTEAD),
        onlineAt = null,
        batteryPercent = null,
        charging = false,
    )

/** How much of an account id stands in for a name nobody has published. */
internal const val NAME_SHOWN_INSTEAD = 6

/** What the clipboard calls what it is holding, where a system shows the name. */
internal const val CLIPBOARD_SAYS = "geoshare"
