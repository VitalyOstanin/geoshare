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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        .collectAsStateWithLifecycle(initialValue = emptyMap())
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
        .collectAsStateWithLifecycle(initialValue = emptyMap())
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

    val rowActions =
        MyPhoneActions(
            mayLookFor = mayLookFor,
            onReset = { resetting.ask(it) },
            onCall = { ringing.ask(it) },
            onStop = { scope.launch { onStopSiren(it.phoneId) } },
            onSearch = ::askSearch,
            onEndSearch = { scope.launch { onEndSearch(it.phoneId) } },
            onRemove = {
                removeTrouble = false
                removeAsk = it
            },
        )

    Surface(modifier = modifier.fillMaxSize()) {
        // Scrolled and inset the way the administrator's panel is, and for the
        // same reasons: one row per device runs off a device, and the title
        // has to clear the clock.
        Column(
            modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            MyPhonesHeader(groupCaption, onHelp = { helpOpen = true }, onClose = onClose)

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
                val siren = sirens[phone.phoneId]

                MyPhoneRow(
                    state =
                    MyPhoneState(
                        phone = phone,
                        own = phone.phoneId == ownPhoneId,
                        siren = siren,
                        sirenState = siren?.let { sirenState(it, sirenNow) },
                        search = searches[phone.phoneId]?.takeIf { it.stands(now) },
                        reset = resets[phone.phoneId],
                    ),
                    now = now,
                    actions = rowActions,
                )
            }
        }
    }

    if (helpOpen) {
        MyPhonesHelp(mayLookFor, onClose = { helpOpen = false })
    }

    removeAsk?.let { phone ->
        RemovePhoneDialog(
            phone = phone,
            groupNames = groupNames,
            trouble = removeTrouble,
            busy = removeBusy,
            onGo = ::writeRemoval,
            onDismiss = { removeAsk = null },
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
