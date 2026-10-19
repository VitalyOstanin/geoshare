package io.github.vitalyostanin.geoshare.update

import io.github.vitalyostanin.geoshare.BuildConfig
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.net.FetchTransport
import io.github.vitalyostanin.geoshare.net.httpGet
import io.github.vitalyostanin.geoshare.net.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection

private val LATEST_RELEASE_URL: String = BuildConfig.LATEST_RELEASE_URL

private const val TAG = "geoshare.release-check"

private const val TIMEOUT_MS = 10_000

/**
 * GitHub's answer for a release is a few tens of kilobytes at most (its
 * notes, a list of assets); it is read up to this cap and no further.
 */
private const val MAX_BODY_BYTES = 1024 * 1024

private const val NOT_FOUND = 404

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
suspend fun fetchLatestRelease(): ReleaseCheck = fetchLatestRelease(::getLatestRelease)

/** [fetchLatestRelease] with [transport] exposed so a test can fake GitHub's answer. */
internal suspend fun fetchLatestRelease(transport: FetchTransport): ReleaseCheck =
    withContext(Dispatchers.IO) {
        val response = try {
            transport(LATEST_RELEASE_URL)
        } catch (offline: IOException) {
            AppLog.w(
                TAG,
                "the latest release could not be requested: " +
                    "${offline.javaClass.simpleName}: ${offline.message}",
            )

            return@withContext ReleaseCheck.Unavailable
        }

        if (response.status == NOT_FOUND) return@withContext ReleaseCheck.NothingToInstall

        if (!isSuccess(response.status)) {
            AppLog.w(TAG, "GitHub answered ${response.status}, not the latest release")

            return@withContext ReleaseCheck.Unavailable
        }

        LatestRelease.parse(response.body)
            ?.let { ReleaseCheck.Found(it) }
            ?: ReleaseCheck.NothingToInstall
    }

private fun getLatestRelease(url: String) =
    httpGet(url, mapOf("Accept" to "application/vnd.github+json"), MAX_BODY_BYTES, TIMEOUT_MS)
