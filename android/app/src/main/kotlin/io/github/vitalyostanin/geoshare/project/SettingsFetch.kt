package io.github.vitalyostanin.geoshare.project

import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.net.FetchTransport
import io.github.vitalyostanin.geoshare.net.httpGet
import io.github.vitalyostanin.geoshare.net.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection

/** How long a GET for `geoshare.json` may take before it counts as [SettingsFetch.Offline]. */
private const val TIMEOUT_MS = 10_000

/**
 * The site is not trusted, so its answer is read up to this many bytes and no
 * further; a well-formed document is a few hundred bytes, and anything past
 * this cap fails [ProjectSettings.parse] anyway.
 */
private const val MAX_BODY_BYTES = 64 * 1024

private const val TOO_MANY_REQUESTS = 429
private const val SERVER_ERROR_START = 500
private const val SERVER_ERROR_END_EXCLUSIVE = 600

private const val TAG = "geoshare.settings-fetch"

/** What downloading a group's settings from the site a [ConnectLink] names produced. */
sealed interface SettingsFetch {
    data class Fetched(val settings: ProjectSettings) : SettingsFetch

    /**
     * No connection, DNS failure, a timeout, or a host that is overloaded
     * (429, 5xx) — worth trying again once the network or the site is back.
     */
    data object Offline : SettingsFetch

    /**
     * The site answered, but not with this project's settings: a non-2xx
     * status other than 429 and 5xx, a body that is not JSON, a document
     * [ProjectSettings.parse] refuses, or one naming a site other than
     * [ConnectLink.site] — a site must never hand out another project's
     * settings, whatever put them there. Kept apart from [Offline] even
     * though today's screen shows both the same way, since a log line and
     * later wording both want to know which one happened.
     */
    data object NotAProject : SettingsFetch
}

/**
 * Downloads a group's `geoshare.json` from the site [link] names and reads it
 * into the settings [Connections] starts Firebase with, over an
 * [HttpURLConnection] GET run on [Dispatchers.IO].
 */
suspend fun fetchSettings(link: ConnectLink): SettingsFetch = fetchSettings(link, ::getSettingsFile)

/**
 * [fetchSettings] with [transport] exposed so a test can fake the site's
 * answer without a server; kept internal since [FetchTransport] names an
 * internal type the public overload above cannot expose as a parameter.
 */
internal suspend fun fetchSettings(link: ConnectLink, transport: FetchTransport): SettingsFetch =
    withContext(Dispatchers.IO) {
        val response = try {
            transport(link.settingsUrl)
        } catch (offline: IOException) {
            AppLog.w(
                TAG,
                "the settings could not be downloaded: " +
                    "${offline.javaClass.simpleName}: ${offline.message}",
            )

            return@withContext SettingsFetch.Offline
        }

        if (!isSuccess(response.status)) {
            AppLog.w(TAG, "the site answered ${response.status}, not the settings")

            // An overloaded host says nothing about whether the project exists.
            return@withContext if (response.status.isTransient()) {
                SettingsFetch.Offline
            } else {
                SettingsFetch.NotAProject
            }
        }

        val settings = ProjectSettings.parse(response.body)
            ?: return@withContext SettingsFetch.NotAProject

        if (settings.site != link.site) {
            return@withContext SettingsFetch.NotAProject
        }

        SettingsFetch.Fetched(settings)
    }

/** 429 and 5xx are the host's state, not a verdict on the project; asking again may succeed. */
private fun Int.isTransient(): Boolean =
    this == TOO_MANY_REQUESTS || this in SERVER_ERROR_START until SERVER_ERROR_END_EXCLUSIVE

/**
 * [FetchTransport]'s default: [url] is always `https://…/geoshare.json`
 * ([ConnectLink.settingsUrl] builds nothing else), and the file is served
 * no-cache since a group's site can be asked to change it at any time.
 */
private fun getSettingsFile(url: String) =
    httpGet(url, mapOf("Cache-Control" to "no-cache"), MAX_BODY_BYTES, TIMEOUT_MS)
