package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Whether the card of the device [positionId] offers the two round buttons that
 * reach into it, the same rule as the panels: any device of one's own but the
 * one the application is open on, and another member's only to an
 * administrator of the group.
 */
fun offersReach(
    positionId: String,
    memberId: String,
    ownPhoneId: String?,
    ownMemberId: String,
    runsTheGroup: Boolean,
): Boolean = positionId != ownPhoneId && (memberId == ownMemberId || runsTheGroup)

/**
 * What the card of a tapped device needs to reach into it: the call standing on
 * it and the request to forget its place, followed while the card is open, and
 * the two confirmations the buttons open. The buttons, the status lines and the
 * dialogs are separate composables because they stand in different places of
 * the card.
 */
@Stable
class DeviceReach(
    val phone: PhoneLine,
    val memberName: String,
    val siren: Siren?,
    val sirenNow: Instant,
    val reset: Reset?,
    val ringing: AskedWrite<PhoneLine>,
    val resetting: AskedWrite<PhoneLine>,
    val onCallSiren: suspend (phoneId: String) -> WriteOutcome,
    val onStopSiren: () -> Unit,
    val onAskReset: suspend (phoneId: String) -> WriteOutcome,
)

/** Follows the siren and the reset request of [phone] for as long as its card is open. */
@Composable
fun rememberDeviceReach(
    phone: PhoneLine,
    memberName: String,
    actions: SettingsActions,
): DeviceReach {
    val scope = rememberCoroutineScope()
    val sirenFlow = remember(phone.phoneId) { actions.sirenOf(phone.phoneId) }
    val resetFlow = remember(phone.phoneId) { actions.resetOf(phone.phoneId) }
    val siren by sirenFlow.collectAsStateWithLifecycle(initialValue = null)
    val reset by resetFlow.collectAsStateWithLifecycle(initialValue = null)
    val sirenNow by sirenTicks(siren) { siren?.let { s -> anyLive(listOf(s), it) } ?: false }

    return DeviceReach(
        phone = phone,
        memberName = memberName,
        siren = siren,
        sirenNow = sirenNow,
        reset = reset,
        ringing = rememberAskedWrite(),
        resetting = rememberAskedWrite(),
        onCallSiren = actions.onCallSiren,
        onStopSiren = { scope.launch { actions.onStopSiren(phone.phoneId) } },
        onAskReset = actions.onAskReset,
    )
}

/** The reset and the siren, round and without a caption, as on the panels. */
@Composable
fun DeviceReachButtons(reach: DeviceReach) {
    val state = reach.siren?.let { sirenState(it, reach.sirenNow) }
    val sounding = state is SirenState.Waiting || state is SirenState.Sounding

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ResetButton(onClick = { reach.resetting.ask(reach.phone) })
        SirenButton(
            sounding = sounding,
            onCall = { reach.ringing.ask(reach.phone) },
            onStop = reach.onStopSiren,
        )
    }
}

/** What the card says of the call and of the request, under the row with the buttons. */
@Composable
fun DeviceReachStatus(reach: DeviceReach, now: Instant) {
    val state = reach.siren?.let { sirenState(it, reach.sirenNow) }

    if (reach.siren != null && state != null) {
        sirenText(reach.siren, state, listOf(reach.phone))?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(SOUGHT_COLOUR),
            )
        }
    }

    reach.reset?.let { resetText(it, now) }?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The two confirmations the buttons of [reach] open. */
@Composable
fun DeviceReachDialogs(reach: DeviceReach, now: Instant) {
    reach.resetting.asked?.let { phone ->
        ResetDialog(
            phoneName = phone.name,
            trouble = reach.resetting.trouble,
            busy = reach.resetting.busy,
            onGo = { reach.resetting.write { reach.onAskReset(it.phoneId) } },
            onCancel = reach.resetting::cancel,
        )
    }

    reach.ringing.asked?.let { phone ->
        SirenDialog(
            memberName = reach.memberName,
            phones = listOf(phone),
            selected = phone.phoneId,
            onSelect = {},
            now = now,
            trouble = reach.ringing.trouble,
            busy = reach.ringing.busy,
            onGo = { reach.ringing.write { reach.onCallSiren(it.phoneId) } },
            onCancel = reach.ringing::cancel,
        )
    }
}
