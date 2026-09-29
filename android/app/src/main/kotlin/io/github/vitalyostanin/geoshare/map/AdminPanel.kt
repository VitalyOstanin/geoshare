package io.github.vitalyostanin.geoshare.map

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Roster
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    /** The code just made, held until the screen is left. */
    invitation: String?,
    /** Whether the last write to the group was refused. */
    trouble: Boolean,
    /** The group's searches, by the phone each is looking for. */
    searches: Map<String, Search>,
    onMakeInvitation: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetRunning: (String, Boolean) -> Unit,
    /** Opens a search for one phone, or renews one already standing; answers whether it stuck. */
    onSearch: suspend (phoneId: String, phoneName: String?, renew: Boolean, hours: Int) -> Boolean,
    /** Ends a search: the phone has been found. */
    onEndSearch: suspend (phoneId: String) -> Unit,
    /** This phone's own `{uid}.{deviceId}`: the one phone nobody here rings. */
    ownPhoneId: String?,
    /** A phone's siren as the database has it, followed for as long as asked. */
    sirenOf: (phoneId: String) -> Flow<Siren?>,
    /** Rings a phone; answers whether the call stuck. */
    onCallSiren: suspend (phoneId: String) -> Boolean,
    onStopSiren: suspend (phoneId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = groupOverview(roster, profiles, grants, ownMemberId, labels)
    val standing = remember(searches, now) { standingSearches(searches, now) }
    var givingUp by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    // The member about to be removed and the name the dialog calls them by.
    var removing by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Which phone is being asked about, in which of the two ways, and the
    // length picked so far -- held apart from whether the write is refused,
    // so a refusal on one phone's search does not linger once a different one
    // is opened.
    var searchAsk by remember { mutableStateOf<SearchAsk?>(null) }
    var searchHours by remember { mutableStateOf(DEFAULT_SEARCH_HOURS) }
    var searchTrouble by remember { mutableStateOf(false) }
    var searchBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // The siren of every phone this panel offers to ring, followed while it
    // is open: a call rung from anywhere else shows on its row as well.
    val ringable =
        remember(lines, ownPhoneId) {
            lines.flatMap { it.phones }.map { it.phoneId }.filter { it != ownPhoneId }.toSet()
        }
    val sirens by remember(ringable) { sirensOf(ringable, sirenOf) }
        .collectAsState(initial = emptyMap())
    var sirenAsk by remember { mutableStateOf<SirenAsk?>(null) }
    var sirenTrouble by remember { mutableStateOf(false) }
    var sirenBusy by remember { mutableStateOf(false) }
    // The countdown reads seconds, and the panel's own clock ticks far slower.
    val sirenNow by produceState(now, sirens) {
        value = Instant.now()

        while (anyLive(sirens.values, value)) {
            delay(SIREN_TICK_MS)
            value = Instant.now()
        }
    }

    fun writeSiren() {
        val ask = sirenAsk ?: return

        sirenBusy = true
        scope.launch {
            val allowed = onCallSiren(ask.selected)

            sirenBusy = false

            if (allowed) {
                sirenAsk = null
                sirenTrouble = false
            } else {
                sirenTrouble = true
            }
        }
    }

    fun askSearch(phone: PhoneLine, memberName: String, renew: Boolean) {
        searchAsk = SearchAsk(phone, memberName, renew)
        searchHours = DEFAULT_SEARCH_HOURS
        searchTrouble = false
    }

    fun writeSearch() {
        val ask = searchAsk ?: return

        searchBusy = true
        scope.launch {
            val allowed = onSearch(ask.phone.phoneId, ask.phone.name, ask.renew, searchHours)

            searchBusy = false

            if (allowed) {
                searchAsk = null
                searchTrouble = false
            } else {
                searchTrouble = true
            }
        }
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
                    text = stringResource(R.string.admin_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                HelpButton(onClick = { helpOpen = true })

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
            }

            HorizontalDivider()

            for (line in lines) {
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
                            (line.displayName ?: line.memberId.take(NAME_SHOWN_INSTEAD))
                    },
                    onSearch = { phone ->
                        askSearch(
                            phone,
                            line.displayName ?: line.memberId.take(NAME_SHOWN_INSTEAD),
                            renew = false,
                        )
                    },
                    onEndSearch = { phoneId -> scope.launch { onEndSearch(phoneId) } },
                    sirens = sirens,
                    sirenNow = sirenNow,
                    onSiren = { phone ->
                        sirenTrouble = false
                        sirenAsk =
                            SirenAsk(
                                name = line.displayName ?: line.memberId.take(NAME_SHOWN_INSTEAD),
                                phones = listOf(phone),
                                selected = phone.phoneId,
                            )
                    },
                    onStopSiren = { phoneId -> scope.launch { onStopSiren(phoneId) } },
                )
            }

            // Every search standing now, under the rows rather than in them,
            // with the one thing the phone's own row does not offer: renewing
            // it without ending it first.
            for (search in standing.values) {
                val phone = phoneLine(search, lines)

                AdminSearchSummary(
                    search = search,
                    phoneName = phone.name,
                    memberName =
                    lines.firstOrNull { it.memberId == search.memberId }?.displayName
                        ?: search.memberId.take(NAME_SHOWN_INSTEAD),
                    openedByYou = search.openedBy == ownMemberId,
                    openedByName =
                    profiles[search.openedBy]?.displayName
                        ?: search.openedBy.take(NAME_SHOWN_INSTEAD),
                    onRenew = {
                        askSearch(
                            phone,
                            lines.firstOrNull { it.memberId == search.memberId }?.displayName
                                ?: search.memberId.take(NAME_SHOWN_INSTEAD),
                            renew = true,
                        )
                    },
                    onEndSearch = { scope.launch { onEndSearch(search.phoneId) } },
                )
            }

            HorizontalDivider()

            AdminInvitation(invitation = invitation, onMakeInvitation = onMakeInvitation)

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
        HelpSheet(
            title = stringResource(R.string.help_title_screen),
            entries =
            listOf(
                HelpEntry(R.drawable.ic_siren, HelpLook.Alarm, stringResource(R.string.help_siren)),
                HelpEntry(
                    R.drawable.ic_search_alarm,
                    HelpLook.Alarm,
                    stringResource(R.string.help_search),
                ),
                HelpEntry(
                    R.drawable.ic_search_alarm,
                    HelpLook.AlarmFilled,
                    stringResource(R.string.help_search_standing),
                ),
            ),
            onClose = { helpOpen = false },
        )
    }

    // Giving up one's own role cannot be undone from here: only another
    // administrator can hand it back.
    if (givingUp) {
        AlertDialog(
            onDismissRequest = { givingUp = false },
            title = { Text(text = stringResource(R.string.group_give_up_title)) },
            text = { Text(text = stringResource(R.string.group_give_up_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        givingUp = false
                        onSetRunning(ownMemberId, false)
                    },
                ) {
                    Text(text = stringResource(R.string.group_give_up))
                }
            },
            dismissButton = {
                TextButton(onClick = { givingUp = false }) {
                    Text(text = stringResource(R.string.group_keep_running))
                }
            },
        )
    }

    // Removal is not undone from here either: the member comes back only by a
    // new invitation.
    removing?.let { (memberId, name) ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(text = stringResource(R.string.group_remove_title, name)) },
            text = { Text(text = stringResource(R.string.group_remove_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removing = null
                        onRemoveMember(memberId)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.group_remove_go),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) {
                    Text(text = stringResource(R.string.search_cancel))
                }
            },
        )
    }

    sirenAsk?.let { ask ->
        SirenDialog(
            memberName = ask.name,
            phones = ask.phones,
            selected = ask.selected,
            onSelect = { sirenAsk = ask.copy(selected = it) },
            now = now,
            trouble = sirenTrouble,
            busy = sirenBusy,
            onGo = ::writeSiren,
            onCancel = { sirenAsk = null },
        )
    }

    searchAsk?.let { ask ->
        SearchDialog(
            memberName = ask.memberName,
            phoneName = ask.phone.name,
            renew = ask.renew,
            hours = searchHours,
            onHoursChange = { searchHours = it },
            now = now,
            trouble = searchTrouble,
            busy = searchBusy,
            onGo = ::writeSearch,
            onCancel = { searchAsk = null },
        )
    }
}

/**
 * What the row says of the call this screen made: which phone, the seconds
 * left while it sounds, or that it never answered. Nothing once it is over.
 */
@Composable
internal fun sirenText(siren: Siren, state: SirenState, phones: List<PhoneLine>): String? {
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

/** How often the siren's countdown on the row is redrawn. */
private const val SIREN_TICK_MS = 1_000L

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

/**
 * One member, with what the household knows about them from here, each of
 * their phones its own line and its own pair of round buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminMemberRow(
    line: GroupLine,
    now: Instant,
    /** The group's searches, by the phone each stands for. */
    searches: Map<String, Search>,
    /** This phone's own `{uid}.{deviceId}`: the one phone nobody here rings. */
    ownPhoneId: String?,
    onMakeAdmin: () -> Unit,
    onStopRunning: () -> Unit,
    onRemove: () -> Unit,
    onSearch: (PhoneLine) -> Unit,
    onEndSearch: (phoneId: String) -> Unit,
    /** The calls standing on the group's phones, by phone. */
    sirens: Map<String, Siren>,
    sirenNow: Instant,
    onSiren: (PhoneLine) -> Unit,
    onStopSiren: (phoneId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp)) {
        Text(
            text = line.displayName ?: line.memberId.take(NAME_SHOWN_INSTEAD),
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = aboutThem(line),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Each phone is its own row: whether it is in touch is the question
        // the panel is opened with, and the two round buttons that reach into
        // it — the siren and the search — belong to that one phone and no
        // other.
        for (phone in line.phones) {
            AdminPhoneRow(
                phone = phone,
                now = now,
                search = searches[phone.phoneId],
                // A phone does not ring itself, the same rule as elsewhere in
                // the application. Anybody but oneself may be looked for,
                // administrators included: the one who is lost is not always
                // the one who does not run the group.
                mayRing = phone.phoneId != ownPhoneId,
                mayLookFor = !line.isYou,
                siren = sirens[phone.phoneId],
                sirenNow = sirenNow,
                onSearch = { onSearch(phone) },
                onEndSearch = { onEndSearch(phone.phoneId) },
                onSiren = { onSiren(phone) },
                onStopSiren = { onStopSiren(phone.phoneId) },
            )
        }

        // Under the phones rather than beside them, so that "Remove from the
        // group" fits whole on a phone.
        FlowRow(verticalArrangement = Arrangement.Center) {
            if (line.mayBeMadeAdmin) {
                TextButton(onClick = onMakeAdmin) {
                    Text(text = stringResource(R.string.group_make_admin))
                }
            }

            if (line.mayStopRunning) {
                TextButton(onClick = onStopRunning) {
                    Text(text = stringResource(R.string.group_stop_running))
                }
            }

            if (line.removable) {
                TextButton(onClick = onRemove) {
                    Text(text = stringResource(R.string.group_remove))
                }
            }
        }
    }
}

/**
 * One phone of a member: what it says about itself on the left, and the round
 * buttons that reach into it on the right, in one line — ringing it, or
 * opening a search that puts its place, and only its place, in front of the
 * whole group.
 */
@Composable
private fun AdminPhoneRow(
    phone: PhoneLine,
    now: Instant,
    /** The search standing for this phone, or null while nobody is looking for it. */
    search: Search?,
    mayRing: Boolean,
    mayLookFor: Boolean,
    siren: Siren?,
    sirenNow: Instant,
    onSearch: () -> Unit,
    onEndSearch: () -> Unit,
    onSiren: () -> Unit,
    onStopSiren: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = phoneText(phone, now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (search != null) {
                Text(
                    text =
                    stringResource(
                        R.string.sought_until,
                        SEARCH_ENDS_AT.format(search.endsAt.atZone(ZoneId.systemDefault())),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(SOUGHT_COLOUR),
                )
            }

            val sirenState = siren?.let { sirenState(it, sirenNow) }

            if (siren != null && sirenState != null) {
                sirenText(siren, sirenState, listOf(phone))?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(SOUGHT_COLOUR),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val sirenState = siren?.let { sirenState(it, sirenNow) }
            val sounding = sirenState is SirenState.Waiting || sirenState is SirenState.Sounding

            if (siren != null && sounding) {
                Button(
                    onClick = onStopSiren,
                    colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(SOUGHT_COLOUR),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_siren_off),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(text = stringResource(R.string.siren_stop_short))
                }
            } else if (mayRing) {
                // Round and red, as on the page: the one button in the row
                // that makes this phone sound loud.
                OutlinedIconButton(
                    onClick = onSiren,
                    border = BorderStroke(1.dp, Color(SOUGHT_COLOUR)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_siren),
                        contentDescription = stringResource(R.string.siren_hint),
                        tint = Color(SOUGHT_COLOUR),
                    )
                }
            }

            if (mayLookFor) {
                SearchButton(
                    standing = search != null,
                    onClick = if (search != null) onEndSearch else onSearch,
                )
            }
        }
    }
}

/** When a search ends: the day as well as the time, the same reading a permission's end has. */
private val SEARCH_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/**
 * Every search standing now, under the rows: who is looked for, who opened it
 * and when it ends, with the two things to do about it.
 */
@Composable
private fun AdminSearchSummary(
    search: Search,
    phoneName: String,
    memberName: String,
    openedByYou: Boolean,
    openedByName: String,
    onRenew: () -> Unit,
    onEndSearch: () -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text =
            stringResource(
                R.string.search_summary,
                phoneName,
                memberName,
                SEARCH_ENDS_AT.format(search.endsAt.atZone(ZoneId.systemDefault())),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(SOUGHT_COLOUR),
        )
        Text(
            text =
            stringResource(
                if (openedByYou) R.string.search_opened_by_you else R.string.search_opened_by,
                openedByName,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row {
            TextButton(onClick = onRenew) {
                Text(text = stringResource(R.string.search_renew))
            }

            Button(
                onClick = onEndSearch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(SOUGHT_COLOUR)),
            ) {
                Text(text = stringResource(R.string.search_found))
            }
        }
    }
}

/**
 * A phone, when it was last in touch, and its charge: after its name where the
 * line stands for the member, and without it where the name is the line above.
 */
@Composable
internal fun phoneText(phone: PhoneLine, now: Instant, named: Boolean = true): String {
    val onlineAt =
        phone.onlineAt ?: return if (named) {
            stringResource(R.string.admin_phone_silent, phone.name)
        } else {
            stringResource(R.string.phone_silent)
        }
    val age = ageText(positionAge(onlineAt, now))
    val said =
        mutableListOf(
            if (named) {
                stringResource(R.string.admin_phone_online, phone.name, age)
            } else {
                stringResource(R.string.phone_online, age)
            },
        )

    phone.batteryPercent?.let { said.add(stringResource(R.string.map_battery, it)) }

    if (phone.charging) {
        said.add(stringResource(R.string.admin_phone_charging))
    }

    return said.joinToString(", ")
}

/**
 * The line under the name: who they are to this group, and what this phone may
 * see of them.
 *
 * Said in one line rather than in marks beside the name, because a mark has to
 * be learnt and this is read once a month.
 */
@Composable
private fun aboutThem(line: GroupLine): String {
    val said = mutableListOf<String>()

    if (line.isYou) {
        said.add(stringResource(R.string.admin_you))
    }

    if (line.runsTheGroup) {
        said.add(stringResource(R.string.group_runs_it))
    }

    said.add(
        if (line.phonesShared == 0) {
            stringResource(R.string.admin_not_shared)
        } else {
            pluralStringResource(
                R.plurals.admin_phones_shared,
                line.phonesShared,
                line.phonesShared,
            )
        },
    )

    return said.joinToString(separator = ", ")
}

/**
 * The invitation, made here rather than in the settings: handing somebody a
 * way into the group is running it, and the screen for running it is this one.
 */
@Composable
private fun AdminInvitation(invitation: String?, onMakeInvitation: () -> Unit) {
    val context = LocalContext.current

    if (invitation != null) {
        Text(
            text = invitation,
            style = MaterialTheme.typography.bodyLarge,
            // A face of one width throughout: half of it is an account id,
            // where a letter read for another is a different account.
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Text(
            text = stringResource(R.string.group_code_lasts),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(onClick = onMakeInvitation) {
            Text(
                text =
                stringResource(
                    if (invitation == null) {
                        R.string.group_make_invitation
                    } else {
                        R.string.group_make_another_invitation
                    },
                ),
            )
        }

        if (invitation != null) {
            TextButton(
                onClick = {
                    context
                        .getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText(CLIPBOARD_SAYS, invitation))
                    Toast
                        .makeText(context, R.string.group_copied, Toast.LENGTH_SHORT)
                        .show()
                },
            ) {
                Text(text = stringResource(R.string.group_copy))
            }
        }
    }
}

/** How much of an account id stands in for a name nobody has published. */
private const val NAME_SHOWN_INSTEAD = 6

/** What the clipboard calls what it is holding, where a system shows the name. */
private const val CLIPBOARD_SAYS = "geoshare"
