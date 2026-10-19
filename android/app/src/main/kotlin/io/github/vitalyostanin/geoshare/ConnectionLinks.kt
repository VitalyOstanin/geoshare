package io.github.vitalyostanin.geoshare

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vitalyostanin.geoshare.project.ConnectFlow
import io.github.vitalyostanin.geoshare.project.ConnectScreen
import io.github.vitalyostanin.geoshare.project.ConnectState
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupNames
import io.github.vitalyostanin.geoshare.project.INHERITED
import io.github.vitalyostanin.geoshare.project.PendingInvitationStore
import io.github.vitalyostanin.geoshare.project.fetchSettings
import io.github.vitalyostanin.geoshare.project.pendingInvitationFor
import io.github.vitalyostanin.geoshare.update.EXTRA_OPEN_UPDATE
import io.github.vitalyostanin.geoshare.update.UpdateBoard
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// The connection links the activity is handed, and the screens that follow one
// through. A link is worth acting on whether or not the device is already in a
// group, so none of this waits for one.

/** Follows the links this activity is handed, the first one included. */
internal fun MainActivity.listenForLinks(savedInstanceState: Bundle?) {
    // Independent of the project check in onCreate: a link is worth
    // acting on whether or not this device already has one, and singleTask
    // means this is the only place onCreate ever sees one — a later
    // arrival comes through onNewIntent instead.
    connectFlow = ConnectFlow(
        scope = lifecycleScope,
        fetch = ::fetchSettings,
        connected = { Connections.all.value.map { it.settings } },
    )
    // A recreated activity (a rotation) still carries the link it was
    // opened with, which has already been acted on.
    if (savedInstanceState == null) {
        connectFlow.linkArrived(intent.dataString)
    }

    // Reacts to Done wherever it is reached — the placeholder screen for
    // a device with no project yet, or the dialog offered over the map
    // for one switching to another — since accepting a link always ends
    // the same way. Only while started: accepting ends in recreate(), which
    // an activity already stopped would run against a window nobody sees,
    // and a Done reached meanwhile is handled on return, the state being
    // held by the flow.
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            connectFlow.state.collect { state ->
                if (state is ConnectState.Done) {
                    acceptConnection(state)
                }
            }
        }
    }
}

/**
 * A later intent, routed here by singleTask (AndroidManifest.xml) instead of
 * into a second instance of the activity, whether the first one is in the
 * foreground or was merely still on the back stack.
 */
internal fun MainActivity.followIntent(intent: Intent) {
    if (intent.getBooleanExtra(EXTRA_OPEN_UPDATE, false)) {
        UpdateBoard.openRequested = true
    }
    connectFlow.linkArrived(intent.dataString)
}

/**
 * What [ConnectState.Done] leads to: the group's project joins the list
 * and is shown, and a group invitation carried alongside it is kept for
 * the sign-in that follows to spend.
 *
 * Nothing is restarted and nothing signed out: every group has a Firebase
 * instance of its own (ADR-0026), so the groups this device was already
 * in go on as they were. A link back to a project already listed shows
 * that group rather than starting it again, and its session stays — the
 * account is the same, and the invitation is spent straight away.
 *
 * The first group a device connects to is the one exception to staying on
 * the screen: the activity was built without a group to show, so it is
 * built again.
 */
private fun MainActivity.acceptConnection(done: ConnectState.Done) {
    val first = !Connections.connected
    val projectId = done.settings.projectId

    PendingInvitationStore.save(this, projectId, pendingInvitationFor(done))
    GroupNames.note(this, projectId, done.invitation?.groupName)

    if (Connections.of(projectId) == null) {
        // Whoever comes in is the one chosen on the sign-in page next,
        // not an account left over from the group's last time here.
        Connections.dropLingeringSession(this, projectId)

        // The invitation's group from the start: the inherited one is not
        // there in a project of its own, and the service would write into
        // it, refused, until the invitation is spent.
        Connections.connect(this, done.settings, done.invitation?.groupId ?: INHERITED)
    } else {
        Connections.select(this, projectId)
    }

    connectFlow.cancel()

    if (first) {
        recreate()
    }
}

/**
 * The link that arrived on a device already in a group, offering an
 * invitation or naming another project, over the map. NotConnected and
 * Done have nothing of their own to show there.
 */
@Composable
internal fun ConnectFlowDialog(flow: ConnectFlow, connectState: ConnectState) {
    if (connectState !is ConnectState.NotConnected &&
        connectState !is ConnectState.Done
    ) {
        Dialog(
            onDismissRequest = flow::cancel,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ConnectFlowScreen(flow, connectState)
            }
        }
    }
}

/** The connecting screen, driven by [flow]: offered on its own and over the map alike. */
@Composable
internal fun ConnectFlowScreen(flow: ConnectFlow, state: ConnectState) {
    ConnectScreen(
        state = state,
        onPasteLink = flow::pasteLink,
        onCancel = flow::cancel,
        onRetry = flow::retry,
        onConnect = flow::connect,
    )
}
