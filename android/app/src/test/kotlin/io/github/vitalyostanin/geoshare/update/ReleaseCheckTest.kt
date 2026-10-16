package io.github.vitalyostanin.geoshare.update

import io.github.vitalyostanin.geoshare.project.RawResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private const val DIGEST = "0000000000000000000000000000000000000000000000000000000000000000"

private val RELEASE_JSON = """
    {"tag_name":"v0.2.0","html_url":"https://github.com/x/geoshare/releases/tag/v0.2.0",
     "published_at":"2026-10-17T10:00:00Z",
     "assets":[{"name":"geoshare.apk","size":10,
       "digest":"sha256:$DIGEST",
       "browser_download_url":"https://github.com/x/geoshare/releases/download/v0.2.0/geoshare.apk"}]}
""".trimIndent()

class ReleaseCheckTest {
    private fun check(transport: (String) -> RawResponse): ReleaseCheck =
        runBlocking { fetchLatestRelease(transport) }

    @Test
    fun `a readable release is found`() {
        val result = check { RawResponse(200, RELEASE_JSON) }

        assertEquals("0.2.0", (result as ReleaseCheck.Found).release.version)
    }

    @Test
    fun `the request goes to the latest release of the project`() {
        var asked = ""

        check { url ->
            asked = url
            RawResponse(200, RELEASE_JSON)
        }

        assertEquals("https://api.github.com/repos/VitalyOstanin/geoshare/releases/latest", asked)
    }

    @Test
    fun `a project with no release yet is nothing to install`() {
        assertEquals(ReleaseCheck.NothingToInstall, check { RawResponse(404, "{}") })
    }

    @Test
    fun `an answer that cannot be installed safely is nothing to install`() {
        assertEquals(ReleaseCheck.NothingToInstall, check { RawResponse(200, "{}") })
    }

    @Test
    fun `any other status means the check could not be made`() {
        assertEquals(ReleaseCheck.Unavailable, check { RawResponse(403, "rate limited") })
        assertEquals(ReleaseCheck.Unavailable, check { RawResponse(503, "") })
    }

    @Test
    fun `a network failure means the check could not be made`() {
        assertTrue(check { throw IOException("no route") } === ReleaseCheck.Unavailable)
    }
}
