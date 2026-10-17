package io.github.vitalyostanin.geoshare.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** The subdirectory of the cache directory the downloaded APK is kept in. */
const val UPDATE_DIRECTORY = "update"

private const val CONNECT_TIMEOUT_MS = 15_000

/** Per read, not for the whole download: the APK is tens of megabytes and the network may be slow. */
private const val READ_TIMEOUT_MS = 30_000

private const val BUFFER_BYTES = 64 * 1024
private const val SUCCESS_RANGE_START = 200
private const val SUCCESS_RANGE_END_EXCLUSIVE = 300

/** How a download of the release APK ended. */
sealed interface ApkDownload {
    /** [file] holds exactly the bytes GitHub announced, checked as they were written. */
    data class Done(val file: File) : ApkDownload

    /** The bytes arrived but are not the announced file; nothing was kept. */
    data object Corrupt : ApkDownload

    /** No connection, a drop halfway or a non-2xx status; nothing was kept. */
    data object Failed : ApkDownload
}

/** Where downloaded updates live under [cacheDir]; the FileProvider hands out this directory. */
fun updateDirectory(cacheDir: File): File = File(cacheDir, UPDATE_DIRECTORY)

/**
 * Downloads [release]'s APK into [directory] and holds it against the size
 * and SHA-256 GitHub announced, hashing while it writes. [onProgress] gets
 * the bytes received so far and the announced size after every chunk. Files
 * of earlier updates in [directory] are removed first, so it never holds
 * more than one APK. If [directory] already holds this release's file with
 * the announced size and SHA-256, it is returned without touching the
 * network, so a return from the install permission screen costs no second
 * download. Cancelling the coroutine deletes the part already
 * written.
 *
 * Not gated here: it starts only when a member presses "Update", and the
 * settings ask first over a network switched off under Downloads
 * (asksBeforeUpdate, ADR-0039).
 */
suspend fun downloadApk(
    release: LatestRelease,
    directory: File,
    onProgress: (received: Long, total: Long) -> Unit,
): ApkDownload = downloadApk(release, directory, onProgress, ::openApk)

/** [downloadApk] with [open] exposed so a test can supply the body without a server. */
internal suspend fun downloadApk(
    release: LatestRelease,
    directory: File,
    onProgress: (received: Long, total: Long) -> Unit,
    open: (url: String) -> InputStream,
): ApkDownload = withContext(Dispatchers.IO) {
    val target = File(directory, updateFileName(release.version))

    if (isIntactCopy(target, release.apk)) {
        // Left by an earlier press of "Update" that ended at the install
        // permission screen; only this release's file is spared.
        directory.listFiles()?.forEach { if (it != target) it.delete() }
        return@withContext ApkDownload.Done(target)
    }

    clearDirectory(directory)

    var kept = false

    try {
        val verdict = open(release.apk.url).use { stream ->
            writeVerified(stream, target, release.apk) { received ->
                ensureActive()
                onProgress(received, release.apk.size)
            }
        }

        if (verdict == ApkVerdict.Intact) {
            kept = true
            ApkDownload.Done(target)
        } else {
            ApkDownload.Corrupt
        }
    } catch (dropped: IOException) {
        ApkDownload.Failed
    } finally {
        // Also reached by cancellation, which is how a half-written file is
        // kept from lingering in the cache.
        if (!kept) target.delete()
    }
}

/**
 * Copies [stream] to [target] while [ApkVerifier] counts and hashes it, and
 * stops as soon as more than the announced size has arrived rather than
 * filling the cache with whatever a bad answer keeps sending.
 */
private inline fun writeVerified(
    stream: InputStream,
    target: File,
    asset: ApkAsset,
    onChunk: (received: Long) -> Unit,
): ApkVerdict {
    val verifier = ApkVerifier(asset)
    val buffer = ByteArray(BUFFER_BYTES)

    target.parentFile?.mkdirs()
    target.outputStream().use { out ->
        while (!verifier.isOverSize) {
            val count = stream.read(buffer)

            if (count < 0) break
            out.write(buffer, 0, count)
            verifier.update(buffer, count)
            onChunk(verifier.received)
        }
    }

    return verifier.verdict()
}

/**
 * Whether [file] is already the whole of [asset]: right size and SHA-256,
 * read once from disk. A part-downloaded or foreign file fails this and is
 * replaced by a fresh download, never reused.
 */
private fun isIntactCopy(file: File, asset: ApkAsset): Boolean {
    if (!file.isFile || file.length() != asset.size) return false

    return try {
        val verifier = ApkVerifier(asset)
        val buffer = ByteArray(BUFFER_BYTES)

        file.inputStream().use { stream ->
            while (true) {
                val count = stream.read(buffer)

                if (count < 0) break
                verifier.update(buffer, count)
            }
        }

        verifier.verdict() == ApkVerdict.Intact
    } catch (unreadable: IOException) {
        false
    }
}

private fun clearDirectory(directory: File) {
    directory.listFiles()?.forEach { it.delete() }
}

/**
 * The default for [downloadApk]'s `open`. The release link redirects from
 * github.com to GitHub's asset host, which [HttpURLConnection] follows
 * (https to https); the digest, not the host, is what vouches for the file.
 */
private fun openApk(url: String): InputStream {
    val connection = URL(url).openConnection() as HttpURLConnection

    connection.connectTimeout = CONNECT_TIMEOUT_MS
    connection.readTimeout = READ_TIMEOUT_MS

    val status = connection.responseCode

    if (status !in SUCCESS_RANGE_START until SUCCESS_RANGE_END_EXCLUSIVE) {
        connection.disconnect()
        throw IOException("HTTP $status")
    }

    return connection.inputStream
}
