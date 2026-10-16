package io.github.vitalyostanin.geoshare.update

import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.time.format.DateTimeParseException

/** The release APK: where to get it and what to check it against once it is on disk. */
data class ApkAsset(val url: String, val size: Long, val sha256: String)

/**
 * What the app needs of GitHub's "latest release" answer: the [version]
 * (the tag without its leading `v`), the release [pageUrl] ("What's new"),
 * when it was [publishedAt] (null if GitHub gave no readable date) and the
 * [apk] to install.
 */
data class LatestRelease(
    val version: String,
    val pageUrl: String,
    val publishedAt: Instant?,
    val apk: ApkAsset,
) {
    companion object {
        private const val APK_SUFFIX = ".apk"
        private const val DIGEST_PREFIX = "sha256:"
        private val SHA256 = Regex("[0-9a-f]{64}")
        private val DOWNLOAD_HOSTS = setOf("github.com", "objects.githubusercontent.com")

        /**
         * Reads the body of `GET /repos/…/releases/latest`. Null for anything
         * that cannot be installed safely: not JSON, a tag that is not a
         * version, no `.apk` asset, or one without a size or a SHA-256 digest
         * to check the download against. The answer is not trusted, so a
         * download link is accepted only for https on GitHub's own hosts.
         */
        fun parse(json: String): LatestRelease? {
            val document = try {
                JSONObject(json)
            } catch (malformed: JSONException) {
                return null
            }

            val version = document.optString("tag_name").removePrefix("v")

            if (parseVersion(version) == null) return null

            val assets = document.optJSONArray("assets") ?: return null
            val asset = (0 until assets.length())
                .mapNotNull { assets.optJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(APK_SUFFIX) }
                ?: return null

            val digest = asset.optString("digest").removePrefix(DIGEST_PREFIX)
            val size = asset.optLong("size")
            val url = asset.optString("browser_download_url")

            if (!asset.optString("digest").startsWith(DIGEST_PREFIX)) return null
            if (!SHA256.matches(digest) || size <= 0 || !isDownloadUrl(url)) return null

            return LatestRelease(
                version = version,
                pageUrl = document.optString("html_url"),
                publishedAt = parseInstant(document.optString("published_at")),
                apk = ApkAsset(url, size, digest),
            )
        }

        private fun isDownloadUrl(url: String): Boolean {
            val uri = try {
                URI(url)
            } catch (malformed: URISyntaxException) {
                return false
            }

            return uri.scheme == "https" && uri.host in DOWNLOAD_HOSTS
        }

        private fun parseInstant(text: String): Instant? = try {
            Instant.parse(text)
        } catch (unreadable: DateTimeParseException) {
            null
        }
    }
}
