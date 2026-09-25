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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Roster

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
    /** The code just made, held until the screen is left. */
    invitation: String?,
    /** Whether the last write to the group was refused. */
    trouble: Boolean,
    onMakeInvitation: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = groupOverview(roster, profiles, grants, ownMemberId)

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
                AdminMemberRow(line = line, onRemove = { onRemoveMember(line.memberId) })
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
}

/**
 * One member, with what the household knows about them from here.
 *
 * The phones are counted rather than named: their identities mean nothing to
 * anybody reading, and what the person running the group is asking is whether
 * the member has a phone in this at all.
 */
@Composable
private fun AdminMemberRow(line: GroupLine, onRemove: () -> Unit) {
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
        }

        if (line.removable) {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(R.string.group_remove))
            }
        }
    }
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
