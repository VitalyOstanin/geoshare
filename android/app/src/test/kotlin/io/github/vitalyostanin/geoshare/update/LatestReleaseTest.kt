package io.github.vitalyostanin.geoshare.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

private const val SHA = "c50b9dff3f28ce8de578f89d4e317d2c5e0196f407fb28e71bab9f14481969eb"
private const val APK_URL =
    "https://github.com/VitalyOstanin/geoshare/releases/download/v0.1.1/geoshare-0.1.1.apk"

private fun release(tag: String = "\"v0.1.1\"", assets: String = apk()): String = """
    {"tag_name":$tag,
     "html_url":"https://github.com/VitalyOstanin/geoshare/releases/tag/v0.1.1",
     "published_at":"2026-10-16T18:29:16Z",
     "assets":[$assets]}
""".trimIndent()

private fun apk(
    name: String = "geoshare-0.1.1.apk",
    size: String = "32128522",
    digest: String = "\"sha256:$SHA\"",
    url: String = APK_URL,
) = """{"name":"$name","size":$size,"digest":$digest,"browser_download_url":"$url"}"""

private val SUMS = """
    {"name":"SHA256SUMS","size":87,"digest":"sha256:8ac1","browser_download_url":"$APK_URL.sums"}
""".trimIndent()

class LatestReleaseTest {
    @Test
    fun `a release is read with its apk asset`() {
        val parsed = LatestRelease.parse(release(assets = "$SUMS,${apk()}"))!!

        assertEquals("0.1.1", parsed.version)
        assertEquals(
            "https://github.com/VitalyOstanin/geoshare/releases/tag/v0.1.1",
            parsed.pageUrl,
        )
        assertEquals(Instant.parse("2026-10-16T18:29:16Z"), parsed.publishedAt)
        assertEquals(APK_URL, parsed.apk.url)
        assertEquals(32128522L, parsed.apk.size)
        assertEquals(SHA, parsed.apk.sha256)
    }

    @Test
    fun `the body is kept as the release notes`() {
        val body = "{\"body\":\"### Added\\n\\n- One.\","
        val json = release().replaceFirst("{", body)

        assertEquals("### Added\n\n- One.", LatestRelease.parse(json)!!.notes)
        assertEquals("", LatestRelease.parse(release())!!.notes)
    }

    @Test
    fun `a tag without the leading v is taken as it is`() {
        assertEquals("0.2.0", LatestRelease.parse(release(tag = "\"0.2.0\""))!!.version)
    }

    @Test
    fun `a release without an apk asset is no update`() {
        assertNull(LatestRelease.parse(release(assets = SUMS)))
        assertNull(LatestRelease.parse(release(assets = "")))
    }

    @Test
    fun `an apk without a sha256 digest is no update`() {
        assertNull(LatestRelease.parse(release(assets = apk(digest = "null"))))
        assertNull(LatestRelease.parse(release(assets = apk(digest = "\"md5:abcd\""))))
        assertNull(LatestRelease.parse(release(assets = apk(digest = "\"sha256:xyz\""))))
    }

    @Test
    fun `an apk of no usable size is no update`() {
        assertNull(LatestRelease.parse(release(assets = apk(size = "0"))))
    }

    @Test
    fun `a download link is only taken from github over https`() {
        val cdn = "https://objects.githubusercontent.com/x/geoshare-0.1.1.apk"

        assertEquals(cdn, LatestRelease.parse(release(assets = apk(url = cdn)))!!.apk.url)
        assertNull(LatestRelease.parse(release(assets = apk(url = "http://github.com/a.apk"))))
        assertNull(LatestRelease.parse(release(assets = apk(url = "https://evil.example/a.apk"))))
        assertNull(
            LatestRelease.parse(
                release(assets = apk(url = "https://github.com.evil.example/a.apk")),
            ),
        )
    }

    @Test
    fun `a tag that is not a version is no update`() {
        assertNull(LatestRelease.parse(release(tag = "\"nightly\"")))
        assertNull(LatestRelease.parse(release(tag = "null")))
    }

    @Test
    fun `malformed json is no update, not an exception`() {
        assertNull(LatestRelease.parse("not json"))
        assertNull(LatestRelease.parse(""))
        assertNull(LatestRelease.parse("[]"))
        assertNull(LatestRelease.parse("""{"message":"Not Found"}"""))
    }
}
