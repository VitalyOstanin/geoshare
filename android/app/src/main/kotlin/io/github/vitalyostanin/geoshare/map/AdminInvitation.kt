package io.github.vitalyostanin.geoshare.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import java.time.Instant
import java.time.ZoneId

/**
 * The invitation link [AdminInvitation] shows and hands over, built from what
 * making one returned and what the screen already knows: this device's own
 * site, the name it publishes for this member, and the group's own name.
 *
 * A function of its own so what goes into the link can be checked without a
 * screen to draw it on. Null before the site is known — [AdminPanel] is not
 * reached before it is, since a device gets there only through a group, and a
 * group only once a project's settings, site included, have been read.
 */
internal fun invitationLinkText(
    made: MadeInvitation,
    site: String?,
    inviter: String?,
    groupName: String?,
): String? {
    val host = site ?: return null

    return ConnectLink.invitationLink(
        site = host,
        groupId = made.groupId,
        code = made.code,
        inviter = inviter,
        groupName = groupName,
        email = made.email,
    )
}

/**
 * The invitation, made here rather than in the settings: handing somebody a
 * way into the group is running it, and the screen for running it is this one.
 *
 * What is shown once one is made is the link itself (ADR-0019) rather than
 * the group and the code apart: a link opens the app straight at the group it
 * names, where the two typed by hand would only get somebody as far as the
 * screen that asks for them.
 */
@Composable
internal fun AdminInvitation(
    invitation: MadeInvitation?,
    /** Whether the invitation still stands, as the database has it; null while unknown. */
    standing: Boolean?,
    /** The clock the invitation is held against: one that has run out admits nobody. */
    now: Instant,
    link: String?,
    onOpenInvitation: () -> Unit,
) {
    val context = LocalContext.current
    var qrOpen by remember { mutableStateOf(false) }

    // Used or run out, the link is taken away: one that admits nobody would
    // only be handed on to somebody who then cannot get in.
    if (invitation != null && (standing == false || !now.isBefore(invitation.expiresAt))) {
        InvitationOver(invitation, used = standing == false, onOpenInvitation = onOpenInvitation)
    } else if (invitation != null && link != null) {
        Text(
            text =
            stringResource(
                R.string.group_invitation_screen_title,
                ConnectLink.shortenedAddress(invitation.email),
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        InvitationQr(
            link = link,
            onOpen = { qrOpen = true },
            modifier = Modifier.fillMaxWidth().wrapContentWidth().padding(vertical = 4.dp),
        )

        if (qrOpen) {
            FullScreenQr(link = link, onClose = { qrOpen = false })
        }

        SelectionContainer {
            Text(
                text = link,
                style = MaterialTheme.typography.bodyLarge,
                // A face of one width throughout: a letter misread for
                // another turns the link into a different one.
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Text(
            text =
            stringResource(
                R.string.group_invitation_expires,
                invitationEndsText(invitation.expiresAt.atZone(ZoneId.systemDefault())),
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SendOrCopyLink(link = link)

        TextButton(
            onClick = onOpenInvitation,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(text = stringResource(R.string.group_make_another_invitation))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            TextButton(onClick = onOpenInvitation) {
                Text(text = stringResource(R.string.group_make_invitation))
            }
        }
    }
}

/** The two ways the link leaves this device: handed to another application, or copied. */
@Composable
private fun SendOrCopyLink(link: String) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, link)
                }

                context.startActivity(Intent.createChooser(send, null))
            },
        ) {
            Text(text = stringResource(R.string.group_invitation_send))
        }

        TextButton(
            onClick = {
                context
                    .getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText(CLIPBOARD_SAYS, link))
                Toast
                    .makeText(context, R.string.group_copied, Toast.LENGTH_SHORT)
                    .show()
            },
        ) {
            Text(text = stringResource(R.string.group_copy))
        }
    }
}

/**
 * What is left of an invitation once it admits nobody: that it was used, or
 * when it ran out, and the way to make the next one.
 */
@Composable
private fun InvitationOver(
    invitation: MadeInvitation,
    used: Boolean,
    onOpenInvitation: () -> Unit,
) {
    Text(
        text =
        stringResource(
            R.string.group_invitation_screen_title,
            ConnectLink.shortenedAddress(invitation.email),
        ),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    Text(
        text =
        if (used) {
            stringResource(R.string.group_invitation_used)
        } else {
            stringResource(
                R.string.group_invitation_expired,
                invitationEndsText(invitation.expiresAt.atZone(ZoneId.systemDefault())),
            )
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    TextButton(
        onClick = onOpenInvitation,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Text(
            text =
            stringResource(
                if (used) R.string.group_make_invitation else R.string.group_invitation_again,
            ),
        )
    }
}
