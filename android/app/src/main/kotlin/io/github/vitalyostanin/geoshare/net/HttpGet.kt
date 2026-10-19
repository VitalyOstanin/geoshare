package io.github.vitalyostanin.geoshare.net

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val SUCCESS_RANGE_START = 200
private const val SUCCESS_RANGE_END_EXCLUSIVE = 300

/** A GET's raw answer, before the caller reads it as whatever document it asked for. */
internal data class RawResponse(val status: Int, val body: String)

/** A GET of [url], throwing [IOException] for anything that keeps the body from being read. */
internal typealias FetchTransport = (url: String) -> RawResponse

/** Any 2xx: the one place the app decides which statuses carry the body it asked for. */
internal fun isSuccess(status: Int): Boolean =
    status in SUCCESS_RANGE_START until SUCCESS_RANGE_END_EXCLUSIVE

/**
 * A small GET over [HttpURLConnection] for a document the app reads whole into
 * memory. The error body is read too, so a caller can log what a host said
 * alongside a non-2xx status; either way no more than [maxBytes] is read,
 * since the host is not trusted to keep its answer small. [headers] carry the
 * one or two request properties a particular host wants.
 */
internal fun httpGet(
    url: String,
    headers: Map<String, String>,
    maxBytes: Int,
    timeoutMs: Int,
): RawResponse {
    val connection = URL(url).openConnection() as HttpURLConnection

    connection.connectTimeout = timeoutMs
    connection.readTimeout = timeoutMs
    headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

    return try {
        val status = connection.responseCode
        val stream = if (isSuccess(status)) connection.inputStream else connection.errorStream
        val body = stream?.use { it.readUpTo(maxBytes) } ?: ByteArray(0)

        RawResponse(status, body.toString(Charsets.UTF_8))
    } finally {
        connection.disconnect()
    }
}

/** [InputStream.readBytes] has no cap of its own; an untrusted host is not read past [limit]. */
internal fun InputStream.readUpTo(limit: Int): ByteArray {
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
