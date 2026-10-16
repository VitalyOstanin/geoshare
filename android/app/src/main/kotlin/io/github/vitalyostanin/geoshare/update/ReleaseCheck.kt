package io.github.vitalyostanin.geoshare.update

import io.github.vitalyostanin.geoshare.project.FetchTransport
import io.github.vitalyostanin.geoshare.project.RawResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/VitalyOstanin/geoshare/releases/latest"

private const val TIMEOUT_MS = 10_000

/**
 * GitHub's answer for a release is a few tens of kilobytes at most (its
 * notes, a list of assets); it is read up to this cap and no further.
 */
private const val MAX_BODY_BYTES = 1024 * 1024

private const val NOT_FOUND = 404
private const val SUCCESS_RANGE_START = 200
private const val SUCCESS_RANGE_END_EXCLUSIVE = 300

/** What asking GitHub for the latest release produced. */
sealed interface ReleaseCheck {
    data class Found(val release: LatestRelease) : ReleaseCheck

    /**
     * GitHub answered, but there is nothing to install: no release yet, or
     * one [LatestRelease.parse] refuses (no APK, no digest, a tag that is not
     * a version).
     */
    data object NothingToInstall : ReleaseCheck

    /**
     * The check could not be made: no connection, a timeout, a rate limit or
     * any other status. Worth asking again later, and not worth telling a
     * member who did not ask.
     */
    data object Unavailable : ReleaseCheck
}

/**
 * Asks GitHub for the latest release over an [HttpURLConnection] GET run on
 * [Dispatchers.IO]. Network trouble is a result, not an exception.
 */
suspend fun fetchLatestRelease(): ReleaseCheck = fetchLatestRelease(::httpGet)

/** [fetchLatestRelease] with [transport] exposed so a test can fake GitHub's answer. */
internal suspend fun fetchLatestRelease(transport: FetchTransport): ReleaseCheck =
    withContext(Dispatchers.IO) {
        val response = try {
            transport(LATEST_RELEASE_URL)
        } catch (offline: IOException) {
            return@withContext ReleaseCheck.Unavailable
        }

        if (response.status == NOT_FOUND) return@withContext ReleaseCheck.NothingToInstall

        if (response.status !in SUCCESS_RANGE_START until SUCCESS_RANGE_END_EXCLUSIVE) {
            return@withContext ReleaseCheck.Unavailable
        }

        LatestRelease.parse(response.body)
            ?.let { ReleaseCheck.Found(it) }
            ?: ReleaseCheck.NothingToInstall
    }

private fun httpGet(url: String): RawResponse {
    val connection = URL(url).openConnection() as HttpURLConnection

    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.setRequestProperty("Accept", "application/vnd.github+json")

    return try {
        val status = connection.responseCode
        val stream = if (status in SUCCESS_RANGE_START until SUCCESS_RANGE_END_EXCLUSIVE) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val body = stream?.use { it.readUpTo(MAX_BODY_BYTES) } ?: ByteArray(0)

        RawResponse(status, body.toString(Charsets.UTF_8))
    } finally {
        connection.disconnect()
    }
}

/** [InputStream.readBytes] has no cap of its own; GitHub's answer is not read past [limit]. */
private fun InputStream.readUpTo(limit: Int): ByteArray {
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    val read = ByteArrayOutputStream()
    var total = 0

    while (total < limit) {
        val count = read(chunk, 0, minOf(chunk.size, limit - total))

        if (count < 0) break
        read.write(chunk, 0, count)
        total += count
    }

    return read.toByteArray()
}
