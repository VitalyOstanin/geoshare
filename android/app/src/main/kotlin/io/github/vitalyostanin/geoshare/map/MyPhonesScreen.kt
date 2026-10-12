package io.github.vitalyostanin.geoshare.map

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * A member's own devices, seen by that member rather than by whoever runs the
 * group.
 *
 * [AdminPanel] answers "what does the household look like", which only the
 * group's administrator needs; this screen answers the narrower question any
 * member asks about themselves — "which of my devices are in touch, and can I
 * make one of them sound" — so it opens straight from the map rather than
 * from the group's own settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyPhonesScreen(
    /** All of this member's devices, this one included. */
    phones: List<PhoneLine>,
    /** This device's own `{uid}.{deviceId}`: listed, but never rung from itself. */
    ownPhoneId: String?,
    now: Instant,
    sirenOf: (phoneId: String) -> Flow<Siren?>,
    onCallSiren: suspend (phoneId: String) -> WriteOutcome,
    onStopSiren: suspend (phoneId: String) -> Unit,
    /** A device's request to forget its place as the database has it. */
    resetOf: (phoneId: String) -> Flow<Reset?>,
    /** Asks one of these devices to forget its place; answers how the write came out. */
    onAskReset: suspend (phoneId: String) -> WriteOutcome,
    /** Takes a device out of every group; answers whether each database allowed it. */
    onForgetPhone: suspend (phoneId: String) -> Boolean,
    /** The groups a removed device leaves, named in the dialog when there is more than one. */
    groupNames: List<String> = emptyList(),
    /**
     * The group the list is of, said under the title while the device is in
     * more than one: the devices are read from that group alone, and a device
     * the member has only in another one shows after switching to it.
     */
    groupCaption: String? = null,
    /** The group's searches, by the device each one is looking for. */
    searches: Map<String, Search> = emptyMap(),
    /** Whether this member runs the group: only they may open a search, on their own devices. */
    mayLookFor: Boolean,
    /** Opens a search on one of these devices; answers how the write came out. */
    onSearch: suspend (
        phoneId: String,
        phoneName: String?,
        renew: Boolean,
        hours: Int,
    ) -> WriteOutcome,
    /** Ends the search on one device: it is found. */
    onEndSearch: suspend (phoneId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // The siren of every device this screen offers to ring, followed while it
    // is open: a call rung from anywhere else shows on its row as well.
    val ringable =
        remember(phones, ownPhoneId) {
            phones.map { it.phoneId }.filter { it != ownPhoneId }.toSet()
        }
    val sirens by remember(ringable) { sirensOf(ringable, sirenOf) }
        .collectAsState(initial = emptyMap())
    val ringing = rememberAskedWrite<PhoneLine>()
    val searching = rememberAskedWrite<PhoneLine>()
    var searchHours by remember { mutableStateOf(DEFAULT_SEARCH_HOURS) }
    var helpOpen by remember { mutableStateOf(false) }
    // The device the member asked to remove, held while the dialog is up.
    var removeAsk by remember { mutableStateOf<PhoneLine?>(null) }
    var removeTrouble by remember { mutableStateOf(false) }
    var removeBusy by remember { mutableStateOf(false) }
    val sirenNow by sirenClock(sirens)

    // The requests to forget a place on the same devices: this one forgets
    // its own from the settings, where the button has always been.
    val resets by remember(ringable) { resetsOf(ringable, resetOf) }
        .collectAsState(initial = emptyMap())
    val resetting = rememberAskedWrite<PhoneLine>()

    fun writeRemoval() {
        val phone = removeAsk ?: return

        removeBusy = true
        scope.launch {
            val removed = onForgetPhone(phone.phoneId)

            removeBusy = false

            if (removed) {
                removeAsk = null
                removeTrouble = false
            } else {
                removeTrouble = true
            }
        }
    }

    fun askSearch(phone: PhoneLine) {
        searching.ask(phone)
        searchHours = DEFAULT_SEARCH_HOURS
    }

    Surface(modifier = modifier.fillMaxSize()) {
        // Scrolled and inset the way the admin panel is, and for the same
        // reasons: one row per device runs off a device, and the title has to
        // clear the clock.
        Column(
            modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.my_phones_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                HelpButton(onClick = { helpOpen = true })

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
            }

            if (groupCaption != null) {
                Text(
                    text = stringResource(R.string.my_phones_group, groupCaption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }

            HorizontalDivider()

            if (phones.isEmpty()) {
                Text(
                    text = stringResource(R.string.my_phones_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            for (phone in phones) {
                val phoneSiren = sirens[phone.phoneId]
                val phoneSirenState = phoneSiren?.let { sirenState(it, sirenNow) }
                val sounding =
                    phoneSirenState is SirenState.Waiting || phoneSirenState is SirenState.Sounding
                val search = searches[phone.phoneId]?.takeIf { it.stands(now) }

                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = phone.name, style = MaterialTheme.typography.bodyLarge)

                        Text(
                            text =
                            if (phone.phoneId == ownPhoneId) {
                                stringResource(R.string.my_phones_this)
                            } else {
                                // The name stands just above.
                                phoneText(phone, now, named = false)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (search != null) {
                            val zoned = search.endsAt.atZone(ZoneId.systemDefault())
                            val endsAt = endsAtText(zoned)

                            Text(
                                text = stringResource(R.string.sought_until, endsAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(SOUGHT_COLOUR),
                            )
                        }

                        if (phoneSiren != null && phoneSirenState != null) {
                            sirenText(phoneSiren, phoneSirenState, listOf(phone))?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(SOUGHT_COLOUR),
                                )
                            }
                        }

                        resets[phone.phoneId]?.let { resetText(it, now) }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Both buttons are outlined in the same red, and a row
                    // this narrow does not always fit them side by side.
                    FlowRow(verticalArrangement = Arrangement.Center) {
                        // Plain and before the siren, as on the admin panel.
                        if (phone.phoneId != ownPhoneId) {
                            ResetButton(onClick = { resetting.ask(phone) })
                        }

                        // A device does not ring itself: the button belongs to
                        // every row but the one this screen is drawn on.
                        if (phone.phoneId != ownPhoneId) {
                            SirenButton(
                                sounding = sounding,
                                onCall = { ringing.ask(phone) },
                                onStop = { scope.launch { onStopSiren(phone.phoneId) } },
                            )
                        }

                        // Every device offers it, this one included: an
                        // administrator may open a search on any device of
                        // theirs, this one among them.
                        if (mayLookFor) {
                            SearchButton(
                                standing = search != null,
                                enabled = !LocalLink.current.lost,
                                onClick = {
                                    if (search != null) {
                                        scope.launch { onEndSearch(phone.phoneId) }
                                    } else {
                                        askSearch(phone)
                                    }
                                },
                            )
                        }

                        // Last in the row and as red as the siren: removing a
                        // device is as final. This device is taken out by
                        // signing out on it, so its row has no bin.
                        if (phone.phoneId != ownPhoneId) {
                            OutlinedIconButton(
                                border = BorderStroke(1.dp, Color(SOUGHT_COLOUR)),
                                onClick = {
                                    removeTrouble = false
                                    removeAsk = phone
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_bin),
                                    contentDescription = stringResource(R.string.phone_remove),
                                    tint = Color(SOUGHT_COLOUR),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (helpOpen) {
        HelpSheet(
            title = stringResource(R.string.help_title_screen),
            entries =
            listOfNotNull(
                HelpEntry(
                    R.drawable.ic_place_reset,
                    HelpLook.Plain,
                    stringResource(R.string.help_reset),
                ),
                HelpEntry(R.drawable.ic_siren, HelpLook.Alarm, stringResource(R.string.help_siren)),
                if (mayLookFor) {
                    HelpEntry(
                        R.drawable.ic_search_alarm,
                        HelpLook.Alarm,
                        stringResource(R.string.help_search),
                    )
                } else {
                    null
                },
                if (mayLookFor) {
                    HelpEntry(
                        R.drawable.ic_search_alarm,
                        HelpLook.AlarmFilled,
                        stringResource(R.string.help_search_standing),
                    )
                } else {
                    null
                },
                HelpEntry(R.drawable.ic_bin, HelpLook.Alarm, stringResource(R.string.help_remove)),
            ),
            onClose = { helpOpen = false },
        )
    }

    removeAsk?.let { phone ->
        AlertDialog(
            onDismissRequest = { removeAsk = null },
            title = { Text(text = stringResource(R.string.phone_remove_title, phone.name)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.phone_remove_text))

                    if (groupNames.size > 1) {
                        Text(
                            text =
                            stringResource(
                                R.string.phone_remove_groups,
                                groupNames.joinToString(", "),
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (removeTrouble) {
                        Text(
                            text = stringResource(R.string.phone_remove_refused),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = ::writeRemoval, enabled = !removeBusy) {
                    Text(
                        text = stringResource(R.string.phone_remove_go),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { removeAsk = null }) {
                    Text(text = stringResource(R.string.search_cancel))
                }
            },
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

    ringing.asked?.let { phone ->
        SirenDialog(
            memberName = stringResource(R.string.map_member_you),
            phones = listOf(phone),
            selected = phone.phoneId,
            onSelect = {},
            now = now,
            trouble = ringing.trouble,
            busy = ringing.busy,
            onGo = { ringing.write { onCallSiren(it.phoneId) } },
            onCancel = ringing::cancel,
        )
    }

    searching.asked?.let { phone ->
        SearchDialog(
            memberName = stringResource(R.string.map_member_you),
            phoneName = phone.name,
            renew = false,
            hours = searchHours,
            onHoursChange = { searchHours = it },
            now = now,
            trouble = searching.trouble,
            busy = searching.busy,
            onGo = { searching.write { onSearch(it.phoneId, it.name, false, searchHours) } },
            onCancel = searching::cancel,
        )
    }
}
