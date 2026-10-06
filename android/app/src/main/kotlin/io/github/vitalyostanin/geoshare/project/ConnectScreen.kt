package io.github.vitalyostanin.geoshare.project

import android.content.ClipboardManager
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/**
 * The screens ADR-0018 describes for [ConnectState]: A phone offering a link
 * to paste, what the site answered before it is accepted, and a failed
 * download. What [ConnectState.Done] leads to — keeping the settings and the
 * invitation, and signing in to the project they name — is done by
 * `MainActivity` rather than here, and drawn as nothing.
 *
 * The caller decides where this goes: the whole of the window for a phone
 * without a project yet (MainActivity's own placeholder), or an overlay
 * above the map for one that already has a project and is offered another
 * link.
 */
@Composable
fun ConnectScreen(
    state: ConnectState,
    onPasteLink: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            is ConnectState.NotConnected, is ConnectState.NoLinkPasted ->
                NotConnected(
                    showError = state is ConnectState.NoLinkPasted,
                    onPasteLink = onPasteLink,
                )

            is ConnectState.AskingForLink ->
                NotConnected(
                    showError = false,
                    onPasteLink = onPasteLink,
                    onCancel = onCancel,
                )

            is ConnectState.Fetching -> {
                AppMark()
                CircularProgressIndicator()
            }

            is ConnectState.Confirm ->
                Confirm(
                    settings = state.settings,
                    invitation = state.link.invitation,
                    // An invitation pasted on a phone already in this
                    // project: nobody is signed out, so there is no
                    // browser to send them to.
                    alreadyConnected = Connections.of(state.settings.projectId) != null,
                    onCancel = onCancel,
                    onConnect = onConnect,
                )

            is ConnectState.Failed ->
                Failed(site = state.link.site, onCancel = onCancel, onRetry = onRetry)

            // MainActivity reacts to Done on its own, outside this
            // composable, by keeping the settings and closing the flow;
            // nothing is left to draw here.
            is ConnectState.Done -> Unit
        }
    }
}

/** The mark from the launcher icon, the same two vectors it is built from. */
@Composable
private fun AppMark() {
    Box(modifier = Modifier.size(72.dp).clip(CircleShape)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Screen А: nothing offered yet, with [showError] for a paste that held no
 * link this project recognises ([ConnectState.NoLinkPasted]). A link the
 * scanner read goes the way a pasted one does.
 *
 * Given [onCancel] it is a device already in a group asking to join one more
 * ([ConnectState.AskingForLink]): the title says that rather than that it is
 * not connected, and there is a way back to the map.
 */
@Composable
private fun NotConnected(
    showError: Boolean,
    onPasteLink: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var creating by rememberSaveable { mutableStateOf(false) }

    // Someone with no invitation reads how to set up a group of their own in
    // place of this screen; back brings the buttons again.
    if (creating) {
        OwnGroupScreen(onBack = { creating = false })
        return
    }

    AppMark()

    Text(
        text =
        stringResource(
            if (onCancel ==
                null
            ) {
                R.string.connect_not_connected_title
            } else {
                R.string.connect_join_title
            },
        ),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )

    Text(
        text = stringResource(R.string.connect_not_connected_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    if (showError) {
        Text(
            text = stringResource(R.string.connect_no_link_in_clipboard),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }

    var scanning by rememberSaveable { mutableStateOf(false) }

    // The scanner first: an invitation is most often shown on the
    // administrator's screen right next to this one. The camera comes on
    // only once it is pressed.
    Button(onClick = { scanning = true }) {
        Text(text = stringResource(R.string.connect_scan_qr))
    }

    FilledTonalButton(
        onClick = {
            val pasted =
                context
                    .getSystemService(ClipboardManager::class.java)
                    ?.primaryClip
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.coerceToText(context)
                    ?.toString()
                    .orEmpty()

            onPasteLink(pasted)
        },
    ) {
        Text(text = stringResource(R.string.connect_paste_link))
    }

    if (onCancel != null) {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.connect_cancel))
        }
    }

    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

    Text(
        text = stringResource(R.string.connect_own_group_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    OutlinedButton(onClick = { creating = true }) {
        Text(text = stringResource(R.string.connect_own_group_button))
    }

    if (scanning) {
        QrScanDialog(
            onFound = { link ->
                scanning = false
                onPasteLink(link)
            },
            onDismiss = { scanning = false },
        )
    }
}

/** Screen Б: [settings] as [ConnectState.Confirm] holds them, before they are accepted. */
@Composable
private fun Confirm(
    settings: ProjectSettings,
    invitation: Invitation?,
    alreadyConnected: Boolean,
    onCancel: () -> Unit,
    onConnect: () -> Unit,
) {
    Text(
        text = stringResource(R.string.connect_confirm_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    // The owner's own link to a phone with nothing connected yet carries no
    // invitation, so only the project row is left; see ConnectFlow.settle.
    InvitationFields(invitation)
    ConnectField(R.string.connect_field_project, settings.projectId)

    // Whoever set the project up reads it all from the Firebase console, rules
    // or no rules; somebody joining is told so before they press the button.
    Text(
        text = stringResource(R.string.connect_confirm_storage),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Text(
        text =
        stringResource(
            if (alreadyConnected) {
                R.string.connect_confirm_hint_joining
            } else {
                R.string.connect_confirm_hint
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    ConnectButtons(
        cancelLabel = R.string.connect_cancel,
        actionLabel = R.string.connect_action,
        onCancel = onCancel,
        onAction = onConnect,
    )
}

/** The rows a link's invitation adds, each only when the link carries it. */
@Composable
private fun InvitationFields(invitation: Invitation?) {
    invitation?.groupName?.let {
        val quoted = stringResource(R.string.connect_quoted_value, it)

        ConnectField(R.string.connect_field_group, quoted)
    }
    invitation?.inviter?.let { ConnectField(R.string.connect_field_invited_by, it) }
    invitation?.invitedAddress?.let { ConnectField(R.string.connect_field_for_account, it) }
}

/** Screen В: [site]'s `geoshare.json` could not be fetched, for either [FailureKind]. */
@Composable
private fun Failed(site: String, onCancel: () -> Unit, onRetry: () -> Unit) {
    Text(
        text = stringResource(R.string.connect_failed_title, site),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )

    Text(
        text = stringResource(R.string.connect_failed_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    ConnectButtons(
        cancelLabel = R.string.connect_cancel,
        actionLabel = R.string.connect_retry,
        onCancel = onCancel,
        onAction = onRetry,
    )
}

/** One row of the key/value block Б shows: [label] on the left, [value] on the right. */
@Composable
private fun ConnectField(@StringRes label: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The cancel/action pair every screen but А and Fetching ends on. */
@Composable
private fun ConnectButtons(
    @StringRes cancelLabel: Int,
    @StringRes actionLabel: Int,
    onCancel: () -> Unit,
    onAction: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(cancelLabel))
        }
        Button(onClick = onAction) {
            Text(text = stringResource(actionLabel))
        }
    }
}
