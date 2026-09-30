package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.runsTheGroup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * One member, with what the household knows about them from here, each of
 * their phones its own line and its own pair of round buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminMemberRow(
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
                    enabled = !LocalLink.current.lost,
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
                    enabled = !LocalLink.current.lost,
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
internal fun AdminSearchSummary(
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
                enabled = !LocalLink.current.lost,
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

    knownNote()?.let(said::add)

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
