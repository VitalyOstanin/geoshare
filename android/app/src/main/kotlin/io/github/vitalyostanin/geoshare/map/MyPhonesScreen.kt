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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A member's own phones, seen by that member rather than by whoever runs the
 * group.
 *
 * [AdminPanel] answers "what does the household look like", which only the
 * group's administrator needs; this screen answers the narrower question any
 * member asks about themselves — "which of my phones are in touch, and can I
 * make one of them sound" — so it opens straight from the map rather than
 * from the group's own settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyPhonesScreen(
    /** All of this member's phones, this one included. */
    phones: List<PhoneLine>,
    /** This phone's own `{uid}.{deviceId}`: listed, but never rung from itself. */
    ownPhoneId: String?,
    now: Instant,
    sirenOf: (phoneId: String) -> Flow<Siren?>,
    onCallSiren: suspend (phoneId: String) -> Boolean,
    onStopSiren: suspend (phoneId: String) -> Unit,
    /** Takes a phone out of the group; answers whether the database allowed it. */
    onForgetPhone: suspend (phoneId: String) -> Boolean,
    /** The group's searches, by the phone each one is looking for. */
    searches: Map<String, Search> = emptyMap(),
    /** Whether this member runs the group: only they may open a search, on their own phones. */
    mayLookFor: Boolean,
    /** Opens a search on one of these phones; answers whether it stuck. */
    onSearch: suspend (phoneId: String, phoneName: String?, renew: Boolean, hours: Int) -> Boolean,
    /** Ends the search on one phone: it is found. */
    onEndSearch: suspend (phoneId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // The phone this screen last rang, followed until the panel is closed so
    // that its row can say whether it answered.
    var sirenPhoneId by remember { mutableStateOf<String?>(null) }
    val siren by remember(sirenPhoneId) { sirenPhoneId?.let(sirenOf) ?: flowOf(null) }
        .collectAsState(initial = null)
    var sirenAsk by remember { mutableStateOf<PhoneLine?>(null) }
    var sirenTrouble by remember { mutableStateOf(false) }
    var sirenBusy by remember { mutableStateOf(false) }
    // Which phone the search dialog is about, held apart from whether the
    // write is refused, the same way the siren state above is.
    var searchAsk by remember { mutableStateOf<PhoneLine?>(null) }
    var searchHours by remember { mutableStateOf(DEFAULT_SEARCH_HOURS) }
    var searchTrouble by remember { mutableStateOf(false) }
    var searchBusy by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    // The phone the member asked to remove, held while the dialog is up.
    var removeAsk by remember { mutableStateOf<PhoneLine?>(null) }
    var removeTrouble by remember { mutableStateOf(false) }
    var removeBusy by remember { mutableStateOf(false) }
    // The countdown reads seconds, and the screen's own clock ticks far slower.
    val sirenNow by produceState(now, siren) {
        while (siren != null) {
            value = Instant.now()
            delay(MY_PHONES_TICK_MS)
        }
    }

    fun writeSiren() {
        val phone = sirenAsk ?: return

        sirenBusy = true
        scope.launch {
            val allowed = onCallSiren(phone.phoneId)

            sirenBusy = false

            if (allowed) {
                sirenPhoneId = phone.phoneId
                sirenAsk = null
                sirenTrouble = false
            } else {
                sirenTrouble = true
            }
        }
    }

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
        searchAsk = phone
        searchHours = DEFAULT_SEARCH_HOURS
        searchTrouble = false
    }

    fun writeSearch() {
        val phone = searchAsk ?: return

        searchBusy = true
        scope.launch {
            val allowed = onSearch(phone.phoneId, phone.name, false, searchHours)

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
        // Scrolled and inset the way the admin panel is, and for the same
        // reasons: one row per phone runs off a phone, and the title has to
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
                    text = stringResource(R.string.my_phones_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                HelpButton(onClick = { helpOpen = true })

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
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
                // Only the phone this screen itself called has a siren to
                // show: every other row is silent until it is rung from here.
                val phoneSiren = siren?.takeIf { phone.phoneId == sirenPhoneId }
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
                            val endsAt = SEARCH_ENDS_AT.format(zoned)

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
                    }

                    // Both buttons are outlined in the same red, and a row
                    // this narrow does not always fit them side by side.
                    FlowRow(verticalArrangement = Arrangement.Center) {
                        // A phone does not ring itself: the button belongs to
                        // every row but the one this screen is drawn on.
                        if (phone.phoneId != ownPhoneId) {
                            if (sounding) {
                                Button(
                                    onClick = { scope.launch { onStopSiren(phone.phoneId) } },
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
                            } else {
                                OutlinedIconButton(
                                    border = BorderStroke(1.dp, Color(SOUGHT_COLOUR)),
                                    onClick = {
                                        sirenTrouble = false
                                        sirenAsk = phone
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_siren),
                                        contentDescription = stringResource(R.string.siren_hint),
                                        tint = Color(SOUGHT_COLOUR),
                                    )
                                }
                            }
                        }

                        // Every phone offers it, this one included: an
                        // administrator may open a search on any phone of
                        // theirs, this one among them.
                        if (mayLookFor) {
                            SearchButton(
                                standing = search != null,
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
                        // phone is as final. This phone is taken out by
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

    sirenAsk?.let { phone ->
        SirenDialog(
            memberName = phone.name,
            phones = listOf(phone),
            selected = phone.phoneId,
            onSelect = {},
            now = now,
            trouble = sirenTrouble,
            busy = sirenBusy,
            onGo = ::writeSiren,
            onCancel = { sirenAsk = null },
        )
    }

    searchAsk?.let { phone ->
        SearchDialog(
            memberName = stringResource(R.string.map_member_you),
            phoneName = phone.name,
            renew = false,
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

/** How often the sounding countdown redraws, same cadence as [AdminPanel]'s own. */
private const val MY_PHONES_TICK_MS = 1_000L

/** Same pattern as [AdminPanel]'s own: private to each file, so each spells it out itself. */
private val SEARCH_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")
