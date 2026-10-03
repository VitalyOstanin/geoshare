package io.github.vitalyostanin.geoshare.project

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** How long a GET for `geoshare.json` may take before it counts as [SettingsFetch.Offline]. */
private const val TIMEOUT_MS = 10_000

/**
 * The site is not trusted, so its answer is read up to this many bytes and no
 * further; a well-formed document is a few hundred bytes, and anything past
 * this cap fails [ProjectSettings.parse] anyway.
 */
private const val MAX_BODY_BYTES = 64 * 1024

private const val SUCCESS_RANGE_START = 200
private const val SUCCESS_RANGE_END_EXCLUSIVE = 300

/** What downloading a group's settings from the site a [ConnectLink] names produced. */
sealed interface SettingsFetch {
    data class Fetched(val settings: ProjectSettings) : SettingsFetch

    /** No connection, DNS failure, a timeout — worth trying again once the network is back. */
    data object Offline : SettingsFetch

    /**
     * The site answered, but not with this project's settings: a non-2xx
     * status, a body that is not JSON, a document [ProjectSettings.parse]
     * refuses, or one naming a site other than [ConnectLink.site] — a site
     * must never hand out another project's settings, whatever put them
     * there. Kept apart from [Offline] even though today's screen shows both
     * the same way, since a log line and later wording both want to know
     * which one happened.
     */
    data object NotAProject : SettingsFetch
}

/** A GET's raw answer, before it is read as [ProjectSettings]. */
internal data class RawResponse(val status: Int, val body: String)

/** A GET of [url], throwing [IOException] for anything that keeps the body from being read. */
internal typealias FetchTransport = (url: String) -> RawResponse

/**
 * Downloads a group's `geoshare.json` from the site [link] names and reads it
 * into the settings [Connections] starts Firebase with, over an
 * [HttpURLConnection] GET run on [Dispatchers.IO].
 */
suspend fun fetchSettings(link: ConnectLink): SettingsFetch = fetchSettings(link, ::httpGet)

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
            return@withContext SettingsFetch.Offline
        }

        if (response.status !in SUCCESS_RANGE_START until SUCCESS_RANGE_END_EXCLUSIVE) {
            return@withContext SettingsFetch.NotAProject
        }

        val settings = ProjectSettings.parse(response.body)
            ?: return@withContext SettingsFetch.NotAProject

        if (settings.site != link.site) {
            return@withContext SettingsFetch.NotAProject
        }

        SettingsFetch.Fetched(settings)
    }

/**
 * [FetchTransport]'s default: [url] is always `https://…/geoshare.json`
 * ([ConnectLink.settingsUrl] builds nothing else), and the file is served
 * no-cache since a group's site can be asked to change it at any time.
 */
private fun httpGet(url: String): RawResponse {
    val connection = URL(url).openConnection() as HttpURLConnection

    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.setRequestProperty("Cache-Control", "no-cache")

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

/** [InputStream.readBytes] has no cap of its own; an untrusted site is not read past [limit]. */
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
