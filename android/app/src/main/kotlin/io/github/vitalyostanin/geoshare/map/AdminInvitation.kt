package io.github.vitalyostanin.geoshare.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** When an invitation expires: the day, spelled out, and the time (ADR-0019). */
private val INVITATION_EXPIRES_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM, HH:mm")

/**
 * The invitation link [AdminInvitation] shows and hands over, built from what
 * making one returned and what the screen already knows: this phone's own
 * site, the name it publishes for this member, and the group's own name.
 *
 * A function of its own so what goes into the link can be checked without a
 * screen to draw it on. Null before the site is known — [AdminPanel] is not
 * reached before it is, since a phone gets there only through a group, and a
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
internal fun AdminInvitation(invitation: MadeInvitation?, link: String?, onOpenInvite: () -> Unit) {
    val context = LocalContext.current

    if (invitation != null && link != null) {
        Text(
            text =
            stringResource(
                R.string.group_invite_screen_title,
                ConnectLink.shortenedAddress(invitation.email),
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

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
                R.string.group_invite_expires,
                INVITATION_EXPIRES_AT.format(invitation.expiresAt.atZone(ZoneId.systemDefault())),
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

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
                Text(text = stringResource(R.string.group_invite_send))
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

        TextButton(
            onClick = onOpenInvite,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(text = stringResource(R.string.group_make_another_invitation))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            TextButton(onClick = onOpenInvite) {
                Text(text = stringResource(R.string.group_make_invitation))
            }
        }
    }
}
