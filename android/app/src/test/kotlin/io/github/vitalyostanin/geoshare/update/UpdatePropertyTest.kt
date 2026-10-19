package io.github.vitalyostanin.geoshare.update

import io.github.vitalyostanin.geoshare.project.randomAwkward
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

private const val SHA = "c50b9dff3f28ce8de578f89d4e317d2c5e0196f407fb28e71bab9f14481969eb"
private const val APK_URL =
    "https://github.com/VitalyOstanin/geoshare/releases/download/v0.1.1/geoshare-0.1.1.apk"

/**
 * What the update check makes of answers far from the handful the other tests
 * use. The release answer and its changelog come from GitHub and are written
 * by whoever publishes the release, so each reader must give a value or null
 * whatever arrives. A fixed seed keeps a failure the same on every run.
 */
class UpdatePropertyTest {
    @Test
    fun `release parse answers any text with a release or null`() {
        val random = Random(701L)
        var accepted = 0

        repeat(600) { case ->
            val valid = validRelease(random).toString()
            val text = when (case % 6) {
                0 -> valid

                1 -> valid.substring(0, random.nextInt(valid.length + 1))

                2 -> mutated(valid, random)

                3 -> wrongTyped(random).toString()

                4 -> randomAwkward(random)

                else -> validRelease(random)
                    .put("assets", JSONArray().put(wrongTypedAsset(random)))
                    .toString()
            }
            val parsed = try {
                LatestRelease.parse(text)
            } catch (thrown: Exception) {
                throw AssertionError("case $case threw on: $text", thrown)
            }

            if (parsed != null) {
                accepted++
                assertTrue("case $case: $text", parseVersion(parsed.version) != null)
                assertTrue("case $case: $text", parsed.apk.size > 0)
                assertTrue("case $case: $text", Regex("[0-9a-f]{64}").matches(parsed.apk.sha256))
                assertTrue("case $case: $text", parsed.apk.url.startsWith("https://"))
            }
        }

        // Without accepted cases the checks above would hold vacuously.
        assertTrue(accepted > 0)
    }

    @Test
    fun `release notes parse answers any body with notes or null`() {
        val random = Random(702L)

        repeat(500) { case ->
            val body = randomNotes(random)
            val language = if (random.nextBoolean()) "ru" else randomAwkward(random)
            val parsed = try {
                ReleaseNotes.parse(body, language)
            } catch (thrown: Exception) {
                throw AssertionError("case $case threw on: $body", thrown)
            }

            if (parsed != null) {
                assertTrue("case $case: $body", parsed.sections.isNotEmpty())
                parsed.sections.forEach { section ->
                    assertTrue("case $case: $body", section.items.isNotEmpty())
                    assertFalse("case $case: $body", section.title == "Project owners")
                }
            }
        }
    }

    @Test
    fun `version parse answers any text with three numbers or null`() {
        val random = Random(703L)

        repeat(500) { case ->
            val text = if (case % 2 == 0) randomVersionLike(random) else randomAwkward(random)
            val parsed = try {
                parseVersion(text)
            } catch (thrown: Exception) {
                throw AssertionError("case $case threw on: $text", thrown)
            }

            if (parsed != null) {
                assertEquals("case $case: $text", 3, parsed.size)
                assertTrue("case $case: $text", parsed.all { it >= 0 })
            }
        }
    }

    // The order the update check relies on: no version is newer than itself,
    // two versions are never newer than each other, newer is transitive, and
    // of two different versions one is always the newer.
    @Test
    fun `newer version is a strict total order on versions`() {
        val random = Random(704L)

        repeat(500) { case ->
            val a = randomVersion(random)
            val b = if (random.nextInt(4) == 0) a else randomVersion(random)
            val c = randomVersion(random)
            val label = "case $case: $a $b $c"

            assertFalse(label, isNewerVersion(a, a))
            assertFalse(label, isNewerVersion(a, b) && isNewerVersion(b, a))
            if (isNewerVersion(a, b) && isNewerVersion(b, c)) {
                assertTrue(label, isNewerVersion(a, c))
            }
            if (parseVersion(a) != parseVersion(b)) {
                assertTrue(label, isNewerVersion(a, b) || isNewerVersion(b, a))
            }
        }
    }

    // A build suffix says nothing about the release, so it never changes the
    // answer.
    @Test
    fun `a build suffix never changes the order`() {
        val random = Random(705L)

        repeat(500) { case ->
            val a = randomVersion(random)
            val b = randomVersion(random)
            val suffix = listOf("-debug", "+build.7", "-rc.1", "-")[random.nextInt(4)]

            assertEquals(
                "case $case: $a $b$suffix",
                isNewerVersion(a, b),
                isNewerVersion(a, b + suffix),
            )
        }
    }

    private fun randomVersion(random: Random): String =
        List(3) { random.nextInt(if (random.nextBoolean()) 3 else Int.MAX_VALUE) }
            .joinToString(".")

    // Mostly near a version: digits, dots, suffix marks, numbers past Int.
    private fun randomVersionLike(random: Random): String = buildString {
        val pieces = listOf(
            "0", "1", "9", "10", "2147483647", "2147483648", "99999999999999999999",
            ".", ".", "-", "+", "v", " ", "٣", "a",
        )

        repeat(random.nextInt(12)) { append(pieces[random.nextInt(pieces.size)]) }
    }

    private fun validRelease(random: Random): JSONObject {
        val version = randomVersion(random)

        return JSONObject()
            .put("tag_name", "v$version")
            .put("html_url", "https://github.com/VitalyOstanin/geoshare/releases/tag/v$version")
            .put("published_at", "2026-10-16T18:29:16Z")
            .put("body", randomAwkward(random))
            .put(
                "assets",
                JSONArray()
                    .put(JSONObject().put("name", "SHA256SUMS").put("size", 87))
                    .put(
                        JSONObject()
                            .put("name", "geoshare-$version.apk")
                            .put("size", 1L + random.nextInt(Int.MAX_VALUE))
                            .put("digest", "sha256:$SHA")
                            .put("browser_download_url", APK_URL),
                    ),
            )
    }

    private fun mutated(valid: String, random: Random): String {
        val chars = StringBuilder(valid)

        repeat(1 + random.nextInt(4)) {
            val at = random.nextInt(chars.length)

            when (random.nextInt(3)) {
                0 -> chars.deleteCharAt(at)
                1 -> chars.insert(at, "{}[]\",:\\\u0000"[random.nextInt(9)])
                else -> chars.setCharAt(at, "{}[]\",:\\\u0000"[random.nextInt(9)])
            }
        }

        return chars.toString()
    }

    private fun wrongValue(random: Random): Any = when (random.nextInt(11)) {
        0 -> JSONObject.NULL
        1 -> random.nextBoolean()
        2 -> random.nextInt()
        3 -> Long.MAX_VALUE
        4 -> Double.MIN_VALUE
        5 -> Double.MAX_VALUE
        6 -> JSONArray().put(randomAwkward(random))
        7 -> JSONObject().put("a", randomAwkward(random))
        8 -> "-1"
        9 -> "1e400"
        else -> randomAwkward(random)
    }

    // Every field of the release present or not, each holding a value of
    // whatever type.
    private fun wrongTyped(random: Random): JSONObject {
        val document = JSONObject()

        listOf("tag_name", "html_url", "published_at", "body", "assets").forEach { field ->
            if (random.nextInt(6) != 0) document.put(field, wrongValue(random))
        }

        return document
    }

    // An `.apk` asset whose other fields hold whatever type.
    private fun wrongTypedAsset(random: Random): JSONObject {
        val asset = JSONObject().put("name", "geoshare.apk")

        listOf("size", "digest", "browser_download_url").forEach { field ->
            if (random.nextInt(6) != 0) asset.put(field, wrongValue(random))
        }

        return asset
    }

    // A body assembled from the markup the reader looks for, in random order,
    // so that headings, bullets, continuations and the Russian block meet in
    // every arrangement.
    private fun randomNotes(random: Random): String = buildString {
        val pieces = listOf(
            "### ", "### Added", "### Project owners", "- ", "- item", "  ", "\t", "\n", "\n",
            "\r\n", "<details lang=\"ru\">", "</details>", "<summary>", "[a](b)", "[", "](",
            ")", "`", "$1", "\\", "Владельцам проектов",
        )

        repeat(random.nextInt(60)) {
            append(
                if (random.nextInt(5) == 0) {
                    randomAwkward(random)
                } else {
                    pieces[random.nextInt(pieces.size)]
                },
            )
        }
    }
}
