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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
 * to paste, what the site answered before it is accepted, a failed
 * download, and the choice a second project's link forces on a phone that
 * already has one. What [ConnectState.Done] leads to — saving the settings,
 * carrying an invitation across the restart and restarting — is done by
 * `MainActivity` rather than here, and drawn as nothing since the process
 * ends before another frame of this would be shown.
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

            is ConnectState.Fetching -> {
                AppMark()
                CircularProgressIndicator()
            }

            is ConnectState.Confirm ->
                Confirm(
                    settings = state.settings,
                    invitation = state.link.invitation,
                    onCancel = onCancel,
                    onConnect = onConnect,
                )

            is ConnectState.Failed ->
                Failed(site = state.link.site, onCancel = onCancel, onRetry = onRetry)

            is ConnectState.SwitchProject ->
                SwitchProject(state = state, onCancel = onCancel, onConnect = onConnect)

            // MainActivity reacts to Done on its own, outside this
            // composable, by saving the settings and restarting; nothing is
            // drawn here since the process ends before another frame would
            // be.
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
 * link this project recognises ([ConnectState.NoLinkPasted]).
 */
@Composable
private fun NotConnected(showError: Boolean, onPasteLink: (String) -> Unit) {
    val context = LocalContext.current

    AppMark()

    Text(
        text = stringResource(R.string.connect_not_connected_title),
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
}

/** Screen Б: [settings] as [ConnectState.Confirm] holds them, before they are accepted. */
@Composable
private fun Confirm(
    settings: ProjectSettings,
    invitation: Invitation?,
    onCancel: () -> Unit,
    onConnect: () -> Unit,
) {
    Text(
        text = stringResource(R.string.connect_confirm_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    // The owner's own link to a phone with nothing connected yet carries no
    // invitation, so only the project row is left; see ConnectFlow.settle.
    invitation?.groupName?.let {
        val quoted = stringResource(R.string.connect_quoted_value, it)

        ConnectField(R.string.connect_field_group, quoted)
    }
    invitation?.inviter?.let { ConnectField(R.string.connect_field_invited_by, it) }
    invitation?.invitedAddress?.let { ConnectField(R.string.connect_field_for_account, it) }
    ConnectField(R.string.connect_field_project, settings.projectId)

    Text(
        text = stringResource(R.string.connect_confirm_hint),
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

/** Screen Г: [state]'s link names a project other than the one this phone already has. */
@Composable
private fun SwitchProject(
    state: ConnectState.SwitchProject,
    onCancel: () -> Unit,
    onConnect: () -> Unit,
) {
    Text(
        text = stringResource(R.string.connect_switch_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    Text(
        text = stringResource(R.string.connect_switch_hint, state.currentProjectId),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    ConnectField(R.string.connect_field_new_project, state.settings.projectId)

    ConnectButtons(
        cancelLabel = R.string.connect_keep,
        actionLabel = R.string.connect_switch,
        onCancel = onCancel,
        onAction = onConnect,
    )
}

/** One row of the key/value block Б and Г show: [label] on the left, [value] on the right. */
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
