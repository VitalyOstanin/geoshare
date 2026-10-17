package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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
    val ringable =
        remember(phones, ownPhoneId) {
            phones.map { it.phoneId }.filter { it != ownPhoneId }.toSet()
        }
    // The siren of every device this screen offers to ring, followed while it
    // is open: a call rung from anywhere else shows on its row as well.
    val sirens by remember(ringable) { sirensOf(ringable, sirenOf) }
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    // The requests to forget a place on the same devices: this one forgets
    // its own from the settings, where the button has always been.
    val resets by remember(ringable) { resetsOf(ringable, resetOf) }
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    val sirenNow by sirenClock(sirens)
    val state = rememberMyPhonesState()
    val rowActions = state.actions(mayLookFor, onStopSiren, onEndSearch)

    Surface(modifier = modifier.fillMaxSize()) {
        MyPhonesList(
            phones = phones,
            ownPhoneId = ownPhoneId,
            now = now,
            groupCaption = groupCaption,
            rows = MyPhoneRows(sirens, sirenNow, searches, resets, rowActions),
            onHelp = { state.helpOpen = true },
            onClose = onClose,
        )
    }

    MyPhonesDialogs(
        state = state,
        mayLookFor = mayLookFor,
        groupNames = groupNames,
        onAskReset = onAskReset,
        onForgetPhone = onForgetPhone,
    )
    MyPhonesAskDialogs(state, now, onCallSiren, onSearch)
}

/** What each row of [MyPhonesList] is drawn from, apart from the device itself. */
private class MyPhoneRows(
    val sirens: Map<String, Siren>,
    val sirenNow: Instant,
    val searches: Map<String, Search>,
    val resets: Map<String, Reset>,
    val actions: MyPhoneActions,
)

@Composable
private fun MyPhonesList(
    phones: List<PhoneLine>,
    ownPhoneId: String?,
    now: Instant,
    groupCaption: String?,
    rows: MyPhoneRows,
    onHelp: () -> Unit,
    onClose: () -> Unit,
) {
    // Scrolled and inset the way the administrator's panel is, and for the
    // same reasons: one row per device runs off a device, and the title
    // has to clear the clock.
    Column(
        modifier =
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        MyPhonesHeader(groupCaption, onHelp = onHelp, onClose = onClose)

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
            val siren = rows.sirens[phone.phoneId]

            MyPhoneRow(
                state =
                MyPhoneState(
                    phone = phone,
                    own = phone.phoneId == ownPhoneId,
                    siren = siren,
                    sirenState = siren?.let { sirenState(it, rows.sirenNow) },
                    search = rows.searches[phone.phoneId]?.takeIf { it.stands(now) },
                    reset = rows.resets[phone.phoneId],
                ),
                now = now,
                actions = rows.actions,
            )
        }
    }
}

/** The help, the removal and the forget-a-place dialogs, each up while its ask is held. */
@Composable
private fun MyPhonesDialogs(
    state: MyPhonesState,
    mayLookFor: Boolean,
    groupNames: List<String>,
    onAskReset: suspend (phoneId: String) -> WriteOutcome,
    onForgetPhone: suspend (phoneId: String) -> Boolean,
) {
    if (state.helpOpen) {
        MyPhonesHelp(mayLookFor, onClose = { state.helpOpen = false })
    }

    state.removeAsk?.let { phone ->
        RemovePhoneDialog(
            phone = phone,
            groupNames = groupNames,
            trouble = state.removeTrouble,
            busy = state.removeBusy,
            onGo = { state.writeRemoval(onForgetPhone) },
            onDismiss = { state.removeAsk = null },
        )
    }

    state.resetting.asked?.let { phone ->
        ResetDialog(
            phoneName = phone.name,
            trouble = state.resetting.trouble,
            busy = state.resetting.busy,
            onGo = { state.resetting.write { onAskReset(it.phoneId) } },
            onCancel = state.resetting::cancel,
        )
    }
}

/** The siren and search dialogs: the two asks that carry more than a yes or no. */
@Composable
private fun MyPhonesAskDialogs(
    state: MyPhonesState,
    now: Instant,
    onCallSiren: suspend (phoneId: String) -> WriteOutcome,
    onSearch: suspend (
        phoneId: String,
        phoneName: String?,
        renew: Boolean,
        hours: Int,
    ) -> WriteOutcome,
) {
    val ringing = state.ringing
    val searching = state.searching

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
            hours = state.searchHours,
            onHoursChange = { state.searchHours = it },
            now = now,
            trouble = searching.trouble,
            busy = searching.busy,
            onGo = { searching.write { onSearch(it.phoneId, it.name, false, state.searchHours) } },
            onCancel = searching::cancel,
        )
    }
}
