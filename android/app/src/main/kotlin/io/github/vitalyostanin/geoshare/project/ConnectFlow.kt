package io.github.vitalyostanin.geoshare.project

import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "geoshare.project"

/**
 * What a connection attempt looks like, one state at a time. [ConnectFlow] is
 * the only thing that moves between these; the screens that paint them are a
 * later change, so a name here describes the mockup screen it stands in for
 * rather than anything drawn yet.
 */
sealed interface ConnectState {
    /**
     * Nothing in hand: no link has been offered since the flow was created,
     * or the previous attempt was cancelled, finished or dismissed as a
     * no-op. This says nothing about whether the phone itself already has a
     * project — a link can still arrive here to switch it.
     */
    data object NotConnected : ConnectState

    /** Pasted text held no [ConnectLink]; reported rather than left unanswered. */
    data object NoLinkPasted : ConnectState

    /** [link]'s settings are being downloaded from [ConnectLink.settingsUrl]. */
    data class Fetching(val link: ConnectLink) : ConnectState

    /** What the site answered for [link], shown before it is accepted. */
    data class Confirm(val link: ConnectLink, val settings: ProjectSettings) : ConnectState

    /** The download for [link] failed; [kind] says how. */
    data class Failed(val link: ConnectLink, val kind: FailureKind) : ConnectState

    /**
     * [link] names a project other than [currentProjectId], the one this
     * phone already talks to — shown instead of [Confirm] because accepting
     * it means leaving that project rather than joining a first one.
     */
    data class SwitchProject(
        val currentProjectId: String,
        val link: ConnectLink,
        val settings: ProjectSettings,
    ) : ConnectState

    /**
     * [settings] and [invitation] were accepted. Saving them, restarting the
     * process and carrying the invitation across that restart are a later
     * change; this state is only the seam it will hang off — nothing is
     * written to the phone by reaching it.
     */
    data class Done(val settings: ProjectSettings, val invitation: Invitation?) : ConnectState
}

/** Why a [ConnectState.Failed] happened, mirroring [SettingsFetch]'s two failure cases. */
enum class FailureKind {
    Offline,
    NotAProject,
}

/**
 * Drives one connection attempt from a link to [ConnectState.Done]. [state]
 * is what a screen collects, and the functions below are what its actions
 * call; none of them draws anything, which is what keeps this testable on
 * the JVM.
 *
 * [fetch] and [connectedProjectId] are taken as parameters rather than
 * reached for directly ([fetchSettings], [FamilyProject]), so a test can
 * run the whole flow without a network or a saved project.
 */
class ConnectFlow(
    private val scope: CoroutineScope,
    private val fetch: suspend (ConnectLink) -> SettingsFetch,
    private val connectedProjectId: () -> String?,
) {
    private val mutableState = MutableStateFlow<ConnectState>(ConnectState.NotConnected)
    val state: StateFlow<ConnectState> = mutableState.asStateFlow()

    /**
     * Reads a link out of pasted text. Text with no [ConnectLink] in it is
     * reported as [ConnectState.NoLinkPasted] rather than left silent —
     * whoever pasted it needs to know the paste did nothing.
     */
    fun pasteLink(text: String) {
        val link = ConnectLink.parse(text)

        if (link == null) {
            mutableState.value = ConnectState.NoLinkPasted

            return
        }

        beginFetch(link)
    }

    /**
     * A link the system itself handed over, as an intent's data rather than
     * the clipboard. A string that fails to parse is logged and otherwise
     * ignored rather than turned into [ConnectState.NoLinkPasted]: the
     * manifest's intent filter already narrows what reaches here, so a
     * failure means the filter and [ConnectLink.parse] disagree, not that
     * whoever sent it made a mistake worth reporting on screen.
     */
    fun linkArrived(dataString: String?) {
        val link = dataString?.let(ConnectLink::parse)

        if (link == null) {
            AppLog.w(TAG, "an intent matched the connect filter but did not parse")

            return
        }

        beginFetch(link)
    }

    /** Tries [ConnectState.Failed.link]'s site again. */
    fun retry() {
        val failed = mutableState.value as? ConnectState.Failed ?: return

        beginFetch(failed.link)
    }

    /** Leaves whatever the current attempt offered without accepting it. */
    fun cancel() {
        mutableState.value = ConnectState.NotConnected
    }

    /** Accepts [ConnectState.Confirm] or [ConnectState.SwitchProject]. */
    fun connect() {
        val (settings, invitation) = when (val current = mutableState.value) {
            is ConnectState.Confirm -> current.settings to current.link.invitation
            is ConnectState.SwitchProject -> current.settings to current.link.invitation
            else -> return
        }

        mutableState.value = ConnectState.Done(settings, invitation)
    }

    private fun beginFetch(link: ConnectLink) {
        mutableState.value = ConnectState.Fetching(link)

        scope.launch {
            mutableState.value = when (val result = fetch(link)) {
                is SettingsFetch.Fetched -> settle(link, result.settings)
                SettingsFetch.Offline -> ConnectState.Failed(link, FailureKind.Offline)
                SettingsFetch.NotAProject -> ConnectState.Failed(link, FailureKind.NotAProject)
            }
        }
    }

    /**
     * What fetched [settings] turn into once the project they name is
     * known: [ConnectState.Confirm] for a phone with nothing to leave,
     * [ConnectState.SwitchProject] for one already talking to a different
     * project, and for a link back to the one it already talks to, either
     * [ConnectState.Confirm] when it still carries an invitation or a
     * silent [ConnectState.NotConnected] when it is only the owner's own
     * link — accepting that again would change nothing.
     */
    private fun settle(link: ConnectLink, settings: ProjectSettings): ConnectState {
        val current = connectedProjectId()

        return when {
            current == null -> ConnectState.Confirm(link, settings)

            current != settings.projectId -> ConnectState.SwitchProject(current, link, settings)

            link.invitation != null -> ConnectState.Confirm(link, settings)

            else -> {
                AppLog.i(TAG, "the owner's own link to the connected project, nothing to do")
                ConnectState.NotConnected
            }
        }
    }
}
