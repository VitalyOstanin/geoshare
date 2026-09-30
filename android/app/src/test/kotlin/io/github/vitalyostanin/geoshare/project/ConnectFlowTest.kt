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

class ConnectFlowTest {
    /**
     * Unconfined rather than a test dispatcher this project has no
     * dependency on: a [ConnectFlow.beginFetch] that never really suspends
     * runs to its settled state inline, and one built on
     * [CompletableDeferred] pauses at the `await` and hands control straight
     * back here, letting a test read [ConnectState.Fetching] in between.
     */
    private fun flow(
        connectedProjectId: () -> String? = { null },
        fetch: suspend (ConnectLink) -> SettingsFetch,
    ) = ConnectFlow(CoroutineScope(Dispatchers.Unconfined), fetch, connectedProjectId)

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
    fun `a link naming a different project than the one connected shows SwitchProject`() {
        val flow = flow(
            connectedProjectId = { "geoshare-other" },
            fetch = { SettingsFetch.Fetched(SETTINGS) },
        )

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(
            ConnectState.SwitchProject("geoshare-other", OWNER_LINK, SETTINGS),
            flow.state.value,
        )
    }

    @Test
    fun `the owner's own link to the already-connected project is a no-op`() {
        val flow = flow(
            connectedProjectId = { SETTINGS.projectId },
            fetch = { SettingsFetch.Fetched(SETTINGS) },
        )

        flow.linkArrived("https://$SITE.web.app/join")

        assertEquals(ConnectState.NotConnected, flow.state.value)
    }

    @Test
    fun `an invitation link to the already-connected project still shows Confirm`() {
        val flow = flow(
            connectedProjectId = { SETTINGS.projectId },
            fetch = { SettingsFetch.Fetched(SETTINGS) },
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
