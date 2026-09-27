package io.github.vitalyostanin.geoshare.map

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Roster
import kotlinx.coroutines.launch
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
    /** The group's searches, by the member each is looking for. */
    searches: Map<String, Search>,
    onMakeInvitation: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetRunning: (String, Boolean) -> Unit,
    /** Opens a search for a member, or renews one already standing; answers whether it stuck. */
    onSearch: suspend (memberId: String, renew: Boolean, hours: Int) -> Boolean,
    /** Ends a search: the member has been found. */
    onEndSearch: suspend (memberId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = groupOverview(roster, profiles, grants, ownMemberId, labels)
    val standing = remember(searches, now) { standingSearches(searches, now) }
    var givingUp by remember { mutableStateOf(false) }
    // Who is being asked about, in which of the two ways, and the length
    // picked so far -- held apart from whether the write is refused, so a
    // refusal on one member's search does not linger once a different one is
    // opened.
    var searchAsk by remember { mutableStateOf<SearchAsk?>(null) }
    var searchHours by remember { mutableStateOf(DEFAULT_SEARCH_HOURS) }
    var searchTrouble by remember { mutableStateOf(false) }
    var searchBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun askSearch(memberId: String, name: String, renew: Boolean) {
        searchAsk = SearchAsk(memberId, name, renew)
        searchHours = DEFAULT_SEARCH_HOURS
        searchTrouble = false
    }

    fun writeSearch() {
        val ask = searchAsk ?: return

        searchBusy = true
        scope.launch {
            val allowed = onSearch(ask.memberId, ask.renew, searchHours)

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

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
            }

            HorizontalDivider()

            for (line in lines) {
                AdminMemberRow(
                    line = line,
                    now = now,
                    search = standing[line.memberId],
                    onMakeAdmin = { onSetRunning(line.memberId, true) },
                    onStopRunning = {
                        if (line.isYou) givingUp = true else onSetRunning(line.memberId, false)
                    },
                    onRemove = { onRemoveMember(line.memberId) },
                    onSearch = {
                        askSearch(
                            line.memberId,
                            line.displayName ?: line.memberId.take(NAME_SHOWN_INSTEAD),
                            renew = false,
                        )
                    },
                    onEndSearch = { scope.launch { onEndSearch(line.memberId) } },
                )
            }

            // Every search standing now, under the rows rather than in them: a
            // search is the group's own state, not a detail of one member's
            // line.
            for (search in standing.values) {
                AdminSearchSummary(
                    search = search,
                    name =
                    lines.firstOrNull { it.memberId == search.memberId }?.displayName
                        ?: search.memberId.take(NAME_SHOWN_INSTEAD),
                    openedByYou = search.openedBy == ownMemberId,
                    openedByName =
                    profiles[search.openedBy]?.displayName
                        ?: search.openedBy.take(NAME_SHOWN_INSTEAD),
                    onRenew = {
                        askSearch(
                            search.memberId,
                            lines.firstOrNull { it.memberId == search.memberId }?.displayName
                                ?: search.memberId.take(NAME_SHOWN_INSTEAD),
                            renew = true,
                        )
                    },
                    onEndSearch = { scope.launch { onEndSearch(search.memberId) } },
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

    searchAsk?.let { ask ->
        SearchDialog(
            memberName = ask.name,
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

/** Who is being asked about, in which of the two ways a search is opened. */
private data class SearchAsk(val memberId: String, val name: String, val renew: Boolean)

/**
 * One member, with what the household knows about them from here.
 *
 * The phones are counted rather than named: their identities mean nothing to
 * anybody reading, and what the person running the group is asking is whether
 * the member has a phone in this at all.
 */
@Composable
private fun AdminMemberRow(
    line: GroupLine,
    now: Instant,
    /** The search standing for this member, or null while nobody is looking for them. */
    search: Search?,
    onMakeAdmin: () -> Unit,
    onStopRunning: () -> Unit,
    onRemove: () -> Unit,
    onSearch: () -> Unit,
    onEndSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.displayName ?: line.memberId.take(NAME_SHOWN_INSTEAD),
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = aboutThem(line),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // One line per phone: whether it is in touch is the question the
            // panel is opened with, and a marker stuck in one place does not
            // answer it.
            for (phone in line.phones) {
                Text(
                    text = phoneText(phone, now),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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
        }

        // One action under another rather than side by side: the names take
        // the width.
        Column(horizontalAlignment = Alignment.End) {
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

            // Anybody but oneself may be looked for, administrators included:
            // the one who is lost is not always the one who does not run the
            // group.
            if (!line.isYou) {
                val alarmText =
                    ButtonDefaults.textButtonColors(contentColor = Color(SOUGHT_COLOUR))

                if (search != null) {
                    TextButton(onClick = onEndSearch, colors = alarmText) {
                        Text(text = stringResource(R.string.search_found))
                    }
                } else {
                    TextButton(onClick = onSearch, colors = alarmText) {
                        Text(text = stringResource(R.string.search_start))
                    }
                }
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
    name: String,
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
                name,
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

/** A phone, when it was last in touch, and its charge. */
@Composable
private fun phoneText(phone: PhoneLine, now: Instant): String {
    val onlineAt = phone.onlineAt ?: return stringResource(R.string.admin_phone_silent, phone.name)
    val age = ageText(positionAge(onlineAt, now))
    val said = mutableListOf(stringResource(R.string.admin_phone_online, phone.name, age))

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
