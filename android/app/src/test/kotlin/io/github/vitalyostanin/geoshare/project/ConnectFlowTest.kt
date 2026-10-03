package io.github.vitalyostanin.geoshare.project

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

private const val SITE = "geoshare-example-abc123"
private val OWNER_LINK = ConnectLink(site = SITE, invitation = null)
private val INVITATION = Invitation(
    groupId = "group-1",
    code = "ABCDEFGH",
    inviter = "Anya",
    groupName = "Sunday hikers",
    invitedAddress = "ivan…@example.com",
)
private val INVITED_LINK = ConnectLink(site = SITE, invitation = INVITATION)
private val SETTINGS = ProjectSettings(
    version = 1,
    projectId = "geoshare-example",
    applicationId = "1:123456789012:android:0123456789abcdef",
    apiKey = "AIzaExampleKeyNotReal",
    gcmSenderId = "123456789012",
    site = SITE,
)
private val OTHER_SETTINGS = ProjectSettings(
    version = 1,
    projectId = "geoshare-other",
    applicationId = "1:210987654321:android:fedcba9876543210",
    apiKey = "AIzaOtherExampleKeyNotReal",
    gcmSenderId = "210987654321",
    site = "geoshare-other-abc123",
)

class ConnectFlowTest {
    /**
     * Unconfined rather than a test dispatcher this project has no
     * dependency on: a [ConnectFlow.beginFetch] that never really suspends
     * runs to its settled state inline, and one built on
     * [CompletableDeferred] pauses at the `await` and hands control straight
     * back here, letting a test read [ConnectState.Fetching] in between.
     */
    private fun flow(
        connected: () -> List<ProjectSettings> = { emptyList() },
        fetch: suspend (ConnectLink) -> SettingsFetch,
    ) = ConnectFlow(CoroutineScope(Dispatchers.Unconfined), fetch, connected)

    @Test
    fun `an owner link goes through Fetching before settling on Confirm with no invitation`() {
        val result = CompletableDeferred<SettingsFetch>()
        val flow = flow(fetch = { result.await() })

        flow.linkArrived("https://$SITE.web.app/join")
        assertEquals(ConnectState.Fetching(OWNER_LINK), flow.state.value)

        result.complete(SettingsFetch.Fetched(SETTINGS))
        assertEquals(ConnectState.Confirm(OWNER_LINK, SETTINGS), flow.state.value)
    }

    @Test
    fun `an invitation link settles on Confirm carrying its captions`() {
        val flow = flow(fetch = { SettingsFetch.Fetched(SETTINGS) })

        flow.linkArrived(
            "https://$SITE.web.app/join#g=group-1&c=ABCDEFGH&by=Anya&n=Sunday+hikers" +
                "&to=ivan%E2%80%A6%40example.com",
        )

        assertEquals(ConnectState.Confirm(INVITED_LINK, SETTINGS), flow.state.value)
    }

    @Test
    fun `offline reads as Failed with the offline kind`() {
        val flow = flow(fetch = { SettingsFetch.Offline })

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(ConnectState.Failed(OWNER_LINK, FailureKind.Offline), flow.state.value)
    }

    @Test
    fun `a site that is not this project's reads as Failed with the not-a-project kind`() {
        val flow = flow(fetch = { SettingsFetch.NotAProject })

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(ConnectState.Failed(OWNER_LINK, FailureKind.NotAProject), flow.state.value)
    }

    @Test
    fun `retry starts fetching the failed link again`() {
        var result = CompletableDeferred<SettingsFetch>()
        val flow = flow(fetch = { result.await() })

        flow.linkArrived("https://$SITE.web.app/join")
        result.complete(SettingsFetch.Offline)
        assertEquals(ConnectState.Failed(OWNER_LINK, FailureKind.Offline), flow.state.value)

        result = CompletableDeferred()
        flow.retry()

        assertEquals(ConnectState.Fetching(OWNER_LINK), flow.state.value)
    }

    @Test
    fun `a link to a project the device is not in yet joins it beside the others`() {
        val flow = flow(
            connected = { listOf(OTHER_SETTINGS) },
            fetch = { SettingsFetch.Fetched(SETTINGS) },
        )

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(ConnectState.Confirm(OWNER_LINK, SETTINGS), flow.state.value)
    }

    @Test
    fun `the owner's own link to a group that is not the one shown is a no-op too`() {
        val flow = flow(
            connected = { listOf(OTHER_SETTINGS, SETTINGS) },
            fetch = { error("fetch should not run for a site already connected") },
        )

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(ConnectState.NotConnected, flow.state.value)
    }

    @Test
    fun `asking to join another group shows the paste screen without an error`() {
        val flow = flow(connected = { listOf(SETTINGS) }, fetch = { SettingsFetch.Offline })

        flow.askForLink()

        assertEquals(ConnectState.AskingForLink, flow.state.value)
    }

    @Test
    fun `cancelling the request for a link leaves the flow idle`() {
        val flow = flow(connected = { listOf(SETTINGS) }, fetch = { SettingsFetch.Offline })

        flow.askForLink()
        flow.cancel()

        assertEquals(ConnectState.NotConnected, flow.state.value)
    }

    @Test
    fun `the owner's own link to the already-connected site is a no-op without fetching`() {
        val flow = flow(
            connected = { listOf(SETTINGS) },
            fetch = { error("fetch should not run for a site already connected") },
        )

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(ConnectState.NotConnected, flow.state.value)
    }

    @Test
    fun `an invitation link to the already-connected site settles on Confirm without fetching`() {
        val flow = flow(
            connected = { listOf(SETTINGS) },
            fetch = { error("fetch should not run for a site already connected") },
        )

        flow.linkArrived("https://$SITE.web.app/join#g=group-1&c=ABCDEFGH")

        assertEquals(SETTINGS, (flow.state.value as ConnectState.Confirm).settings)
        assertEquals("group-1", (flow.state.value as ConnectState.Confirm).link.invitation?.groupId)
    }

    @Test
    fun `pasting text with no link reports it rather than doing nothing quietly`() {
        val flow = flow(fetch = { SettingsFetch.Fetched(SETTINGS) })

        flow.pasteLink("thanks for the recipe, see you Sunday")

        assertEquals(ConnectState.NoLinkPasted, flow.state.value)
    }

    @Test
    fun `cancel from Confirm returns to the state the link arrived from`() {
        val flow = flow(fetch = { SettingsFetch.Fetched(SETTINGS) })

        flow.linkArrived("https://$SITE.web.app/join")
        assertEquals(ConnectState.Confirm(OWNER_LINK, SETTINGS), flow.state.value)

        flow.cancel()

        assertEquals(ConnectState.NotConnected, flow.state.value)
    }
}
