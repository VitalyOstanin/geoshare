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
     * no-op. This says nothing about whether the device itself already has a
     * project — a link can still arrive here to join another.
     */
    data object NotConnected : ConnectState

    /** Pasted text held no [ConnectLink]; reported rather than left unanswered. */
    data object NoLinkPasted : ConnectState

    /**
     * A device already in a group asked to join another: the same paste
     * screen a device with nothing connected shows, offered on request
     * rather than because there is nothing else to show.
     */
    data object AskingForLink : ConnectState

    /** [link]'s settings are being downloaded from [ConnectLink.settingsUrl]. */
    data class Fetching(val link: ConnectLink) : ConnectState

    /** What the site answered for [link], shown before it is accepted. */
    data class Confirm(val link: ConnectLink, val settings: ProjectSettings) : ConnectState

    /** The download for [link] failed; [kind] says how. */
    data class Failed(val link: ConnectLink, val kind: FailureKind) : ConnectState

    /**
     * [settings] and [invitation] were accepted, and there is nothing
     * further for [ConnectFlow] to do about them: keeping the settings,
     * keeping [invitation] until the sign-in they lead to, and spending it
     * once signed in are all done by whoever collects
     * [state] rather than by this class, since every one of them touches
     * Firebase or the device's own storage, neither of which [ConnectFlow] is
     * given a way to reach.
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
 * [fetch] and [connected] are taken as parameters rather than reached for
 * directly ([fetchSettings], [Connections]), so a test can run the whole
 * flow without a network or a saved project.
 */
class ConnectFlow(
    private val scope: CoroutineScope,
    private val fetch: suspend (ConnectLink) -> SettingsFetch,
    private val connected: () -> List<ProjectSettings>,
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
        // The launcher's intent carries no data at all; only an intent that brought an address is worth a warning.
        if (dataString == null) {
            return
        }

        val link = ConnectLink.parse(dataString)

        if (link == null) {
            AppLog.w(TAG, "an intent matched the connect filter but did not parse")

            return
        }

        beginFetch(link)
    }

    /** Shows the paste screen to a device that wants to join one more group. */
    fun askForLink() {
        mutableState.value = ConnectState.AskingForLink
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

    /** Accepts [ConnectState.Confirm]. */
    fun connect() {
        val current = mutableState.value as? ConnectState.Confirm ?: return

        mutableState.value = ConnectState.Done(current.settings, current.link.invitation)
    }

    /**
     * Starts a fetch for [link], unless one of the projects the device is
     * [connected] to already answers to [link]'s site — a settings file pasted back at the
     * device that already has it (an invitation re-pasted on the owner's own
     * device, or the owner's link pasted a second time) needs no download,
     * and skipping it is what keeps that paste working offline or against
     * an emulator with no site to reach.
     */
    private fun beginFetch(link: ConnectLink) {
        val settings = connected().firstOrNull { it.site == link.site }

        if (settings != null) {
            mutableState.value = settle(link, settings)

            return
        }

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
     * What [settings] — fetched, or already the device's own — turn into
     * once the project they name is known: [ConnectState.Confirm] for a
     * project the device is not in yet — joined beside the groups it is
     * already in, none of which it leaves — and for a link back to one it is
     * in, either [ConnectState.Confirm] when it still carries an invitation
     * or a silent [ConnectState.NotConnected] when it is only the owner's
     * own link — accepting that again would change nothing.
     */
    private fun settle(link: ConnectLink, settings: ProjectSettings): ConnectState {
        val listed = connected().any { it.projectId == settings.projectId }

        return when {
            !listed -> ConnectState.Confirm(link, settings)

            link.invitation != null -> ConnectState.Confirm(link, settings)

            else -> {
                AppLog.i(TAG, "the owner's own link to the connected project, nothing to do")
                ConnectState.NotConnected
            }
        }
    }
}
