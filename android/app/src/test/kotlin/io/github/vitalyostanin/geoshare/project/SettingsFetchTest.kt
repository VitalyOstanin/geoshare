package io.github.vitalyostanin.geoshare.project

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

private const val SITE = "geoshare-example-abc123"
private val LINK = ConnectLink(site = SITE, invitation = null)

private val DOCUMENT = """
    {
        "version": 1,
        "projectId": "geoshare-example",
        "applicationId": "1:123456789012:android:0123456789abcdef",
        "apiKey": "AIzaExampleKeyNotReal",
        "gcmSenderId": "123456789012",
        "site": "$SITE"
    }
""".trimIndent()

class SettingsFetchTest {
    private fun fetch(transport: FetchTransport) = runBlocking { fetchSettings(LINK, transport) }

    @Test
    fun `a well-formed document naming the link's own site is fetched`() {
        val result = fetch { url ->
            assertEquals(LINK.settingsUrl, url)
            RawResponse(status = 200, body = DOCUMENT)
        }

        assertEquals(SettingsFetch.Fetched(ProjectSettings.parse(DOCUMENT)!!), result)
    }

    @Test
    fun `a 404 is not this project's site, not a dead connection`() {
        val result = fetch { RawResponse(status = 404, body = "") }

        assertEquals(SettingsFetch.NotAProject, result)
    }

    @Test
    fun `a 503 is an overloaded host, offline rather than the wrong site`() {
        val result = fetch { RawResponse(status = 503, body = "") }

        assertEquals(SettingsFetch.Offline, result)
    }

    @Test
    fun `a 429 is offline too, being worth asking again`() {
        val result = fetch { RawResponse(status = 429, body = "") }

        assertEquals(SettingsFetch.Offline, result)
    }

    @Test
    fun `a body that is not json is refused the same way`() {
        val result = fetch { RawResponse(status = 200, body = "<html>not geoshare</html>") }

        assertEquals(SettingsFetch.NotAProject, result)
    }

    @Test
    fun `settings naming a different site than the link are refused`() {
        val otherSite = DOCUMENT.replace(SITE, "geoshare-other-site")
        val result = fetch { RawResponse(status = 200, body = otherSite) }

        assertEquals(SettingsFetch.NotAProject, result)
    }

    @Test
    fun `a thrown IOException is read as offline, not as the wrong site`() {
        val result = fetch { throw IOException("connection reset") }

        assertEquals(SettingsFetch.Offline, result)
    }

    @Test
    fun `a timed-out connection is offline too, being an IOException`() {
        val result = fetch { throw SocketTimeoutException("timeout") }

        assertEquals(SettingsFetch.Offline, result)
    }
}
