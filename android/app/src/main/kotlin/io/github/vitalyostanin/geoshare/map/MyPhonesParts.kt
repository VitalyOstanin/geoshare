package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant
import java.time.ZoneId

/** The title of the member's own devices, and the group they are read from. */
@Composable
internal fun MyPhonesHeader(groupCaption: String?, onHelp: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.my_phones_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )

        HelpButton(onClick = onHelp)

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
}

/** What one of the member's devices is doing right now, and what can be done about it. */
internal class MyPhoneState(
    val phone: PhoneLine,
    /** Whether this is the device the screen is drawn on. */
    val own: Boolean,
    val siren: Siren?,
    val sirenState: SirenState?,
    val search: Search?,
    val reset: Reset?,
)

/** What the buttons of a row of [MyPhonesScreen] ask for. */
internal class MyPhoneActions(
    val mayLookFor: Boolean,
    val onReset: (PhoneLine) -> Unit,
    val onCall: (PhoneLine) -> Unit,
    val onStop: (PhoneLine) -> Unit,
    val onSearch: (PhoneLine) -> Unit,
    val onEndSearch: (PhoneLine) -> Unit,
    val onRemove: (PhoneLine) -> Unit,
)

/** One of the member's devices: what it says of itself, and the buttons beside it. */
@Composable
internal fun MyPhoneRow(state: MyPhoneState, now: Instant, actions: MyPhoneActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MyPhoneLines(state, now, Modifier.weight(1f))
        MyPhoneButtons(state, actions)
    }
}

@Composable
private fun MyPhoneLines(state: MyPhoneState, now: Instant, modifier: Modifier) {
    val phone = state.phone

    Column(modifier = modifier) {
        Text(text = phone.name, style = MaterialTheme.typography.bodyLarge)

        Text(
            text =
            if (state.own) {
                stringResource(R.string.my_phones_this)
            } else {
                // The name stands just above.
                phoneText(phone, now, named = false)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.search?.let { search ->
            Text(
                text = stringResource(
                    R.string.sought_until,
                    endsAtText(search.endsAt.atZone(ZoneId.systemDefault())),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color(SOUGHT_COLOUR),
            )
        }

        if (state.siren != null && state.sirenState != null) {
            sirenText(state.siren, state.sirenState, listOf(phone))?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(SOUGHT_COLOUR),
                )
            }
        }

        state.reset?.let { resetText(it, now) }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyPhoneButtons(state: MyPhoneState, actions: MyPhoneActions) {
    val phone = state.phone
    val sounding = state.sirenState is SirenState.Waiting || state.sirenState is SirenState.Sounding

    // Both buttons are outlined in the same red, and a row this narrow does
    // not always fit them side by side.
    FlowRow(verticalArrangement = Arrangement.Center) {
        // Plain and before the siren, as on the administrator's panel. A
        // device does not ring itself: the siren belongs to every row but the
        // one this screen is drawn on.
        if (!state.own) {
            ResetButton(onClick = { actions.onReset(phone) })

            SirenButton(
                sounding = sounding,
                onCall = { actions.onCall(phone) },
                onStop = { actions.onStop(phone) },
            )
        }

        // Every device offers it, this one included: an administrator may
        // open a search on any device of theirs, this one among them.
        if (actions.mayLookFor) {
            SearchButton(
                standing = state.search != null,
                enabled = !LocalLink.current.lost,
                onClick = {
                    if (state.search !=
                        null
                    ) {
                        actions.onEndSearch(phone)
                    } else {
                        actions.onSearch(phone)
                    }
                },
            )
        }

        // Last in the row and as red as the siren: removing a device is as
        // final. This device is taken out by signing out on it, so its row
        // has no bin.
        if (!state.own) {
            OutlinedIconButton(
                border = BorderStroke(1.dp, Color(SOUGHT_COLOUR)),
                onClick = { actions.onRemove(phone) },
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

/** What the buttons of [MyPhonesScreen] do, the search ones only where the member may look. */
@Composable
internal fun MyPhonesHelp(mayLookFor: Boolean, onClose: () -> Unit) {
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
            HelpEntry(
                R.drawable.ic_search_alarm,
                HelpLook.Alarm,
                stringResource(R.string.help_search),
            )
                .takeIf { mayLookFor },
            HelpEntry(
                R.drawable.ic_search_alarm,
                HelpLook.AlarmFilled,
                stringResource(R.string.help_search_standing),
            ).takeIf { mayLookFor },
            HelpEntry(R.drawable.ic_bin, HelpLook.Alarm, stringResource(R.string.help_remove)),
        ),
        onClose = onClose,
    )
}

/**
 * The question asked before a device is taken out of every group, naming the
 * groups when there is more than one, and saying so when a database refused.
 */
@Composable
internal fun RemovePhoneDialog(
    phone: PhoneLine,
    groupNames: List<String>,
    trouble: Boolean,
    busy: Boolean,
    onGo: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.phone_remove_title, phone.name)) },
        text = {
            Column {
                Text(text = stringResource(R.string.phone_remove_text))

                if (groupNames.size > 1) {
                    Text(
                        text = stringResource(
                            R.string.phone_remove_groups,
                            groupNames.joinToString(", "),
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (trouble) {
                    Text(
                        text = stringResource(R.string.phone_remove_refused),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGo, enabled = !busy) {
                Text(
                    text = stringResource(R.string.phone_remove_go),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.search_cancel))
            }
        },
    )
}
