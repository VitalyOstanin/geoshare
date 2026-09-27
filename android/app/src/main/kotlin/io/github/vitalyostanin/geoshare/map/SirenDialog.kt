package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant

/**
 * Choosing a target phone and confirming a siren, the way [SearchDialog] chooses
 * a search length: one dialog for both, since a member with a single phone
 * needs no chip row at all and one already selected before the dialog opens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SirenDialog(
    memberName: String,
    phones: List<PhoneLine>,
    selected: String,
    onSelect: (String) -> Unit,
    now: Instant,
    /** Whether the last attempt to sound this siren was refused. */
    trouble: Boolean,
    busy: Boolean,
    onGo: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.siren_title, memberName)) },
        text = {
            Column {
                if (phones.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (phone in phones) {
                            FilterChip(
                                selected = phone.phoneId == selected,
                                onClick = { onSelect(phone.phoneId) },
                                label = { Text(text = sirenPhoneChipLabel(phone, now)) },
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.siren_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (trouble) {
                    Text(
                        text = stringResource(R.string.group_refused),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGo, enabled = !busy) {
                Text(
                    text = stringResource(R.string.siren_go),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = stringResource(R.string.search_cancel)) }
        },
    )
}

/** A chip's label: the phone's name, plus how long ago it was last in touch. */
@Composable
private fun sirenPhoneChipLabel(phone: PhoneLine, now: Instant): String {
    val onlineAt = phone.onlineAt ?: return phone.name
    val age = ageText(positionAge(onlineAt, now))

    return stringResource(R.string.siren_phone_chip, phone.name, age)
}
