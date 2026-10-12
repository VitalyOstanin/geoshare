package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.Orbits
import io.github.vitalyostanin.geoshare.collection.orbitsOf
import io.github.vitalyostanin.geoshare.download.DownloadSetting
import io.github.vitalyostanin.geoshare.download.RoamingQuestion
import io.github.vitalyostanin.geoshare.download.mayDownloadNow
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.zip.GZIPInputStream

// The published orbits, fetched once a day for the check in OrbitCheck.kt.
//
// The file is the one IGS stations put together for a whole day, of every
// system, about 1.2 MB compressed. Yesterday's is fetched rather than today's:
// today's is still being written, and a day-old orbit is within a kilometre
// and a half of a fresh one while the check allows two hundred.

private const val TAG = "geoshare.orbits"
private const val DIRECTORY = "orbits"
private const val SUFFIX = ".rnx.gz"
private const val TIMEOUT_MS = 30_000
private const val MAX_BYTES = 8 * 1024 * 1024

/** How often the service asks whether a newer file is due. */
private val TRY_EVERY: Duration = Duration.ofHours(1)

/**
 * The names the day's file is published under, the usual one first.
 *
 * The others are the same orbits merged by other centres; any of them does
 * when the first is missing.
 */
private val NAMES =
    listOf("BRDC00WRD_S", "BRDC00IGS_R", "BRDC00WRD_R", "BRDM00DLR_S", "BRD400DLR_S")

/** Where the file of [day] is published under [name]. */
fun orbitsUrl(name: String, day: LocalDate): String = String.format(
    Locale.ROOT,
    "https://igs.bkg.bund.de/root_ftp/IGS/BRDC/%04d/%03d/%s_%04d%03d0000_01D_MN.rnx.gz",
    day.year,
    day.dayOfYear,
    name,
    day.year,
    day.dayOfYear,
)

/** Whether a newer file is due: none kept, or the one kept is older than yesterday's. */
fun orbitsDue(kept: LocalDate?, today: LocalDate): Boolean =
    kept == null || kept < today.minusDays(1)

/**
 * Keeps the orbits fresh for as long as the service runs, handing each file
 * read to [onRead].
 *
 * Asks every hour, and again whenever the member's answer about downloads
 * changes: a "download" tapped in the roaming notification, or a network
 * switched on in the settings. Whether it may download at all is the
 * downloads rule's to say — see [mayDownloadNow].
 */
suspend fun keepTheOrbits(context: Context, onRead: (Orbits) -> Unit) {
    val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    keptFile(directory)?.let { read(it) }?.let(onRead)

    val setting = DownloadSetting(context)
    val hourly = flow {
        while (true) {
            emit(Unit)
            delay(TRY_EVERY.toMillis())
        }
    }
    val granted = setting.changes().filter { it.granted }.map { }
    val networks = setting.changes().map { it.allowed }.distinctUntilChanged().map { }

    merge(hourly, granted, networks).collect { fetchIfDue(context, directory)?.let(onRead) }
}

private suspend fun fetchIfDue(context: Context, directory: File): Orbits? {
    val today = LocalDate.now(ZoneOffset.UTC)

    if (!orbitsDue(keptFile(directory)?.let(::dayOf), today)) {
        return null
    }

    val words = context.inAppLanguage()
    val question = RoamingQuestion(
        title = words.getString(R.string.orbits_roaming_title),
        text = words.getString(R.string.orbits_roaming_text),
    )

    if (!mayDownloadNow(words, question)) {
        return null
    }

    val day = today.minusDays(1)
    val target = File(directory, "$day$SUFFIX")

    for (name in NAMES) {
        val url = orbitsUrl(name, day)

        try {
            withContext(Dispatchers.IO) { download(url, target) }
        } catch (failed: IOException) {
            AppLog.w(TAG, "orbits: $name for $day not fetched: ${failed.message}")
            continue
        }

        val orbits = read(target)

        if (orbits == null || orbits.size == 0) {
            AppLog.w(TAG, "orbits: $name for $day holds no orbits")
            target.delete()
            continue
        }

        directory.listFiles()?.filter { it != target }?.forEach(File::delete)
        AppLog.i(TAG, "orbits: $name for $day, ${target.length()} bytes, ${orbits.size} orbits")

        return orbits
    }

    return null
}

private fun download(url: String, target: File) {
    val connection = URL(url).openConnection() as HttpURLConnection

    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS

    try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP ${connection.responseCode}")
        }

        val partial = File(target.parentFile, target.name + ".part")

        connection.inputStream.use { input ->
            partial.outputStream().use { output ->
                val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0

                while (true) {
                    val count = input.read(chunk)

                    if (count < 0) break
                    total += count
                    if (total > MAX_BYTES) throw IOException("larger than $MAX_BYTES bytes")
                    output.write(chunk, 0, count)
                }
            }
        }

        if (!partial.renameTo(target)) {
            throw IOException("could not keep the file")
        }
    } finally {
        connection.disconnect()
    }
}

private suspend fun read(file: File): Orbits? = withContext(Dispatchers.Default) {
    try {
        GZIPInputStream(file.inputStream()).bufferedReader().useLines { orbitsOf(it) }
    } catch (broken: IOException) {
        AppLog.w(TAG, "orbits: ${file.name} unreadable: ${broken.message}")
        null
    }
}

private fun keptFile(directory: File): File? =
    directory.listFiles { file -> file.name.endsWith(SUFFIX) }?.maxByOrNull { it.name }

private fun dayOf(file: File): LocalDate? = try {
    LocalDate.parse(file.name.removeSuffix(SUFFIX))
} catch (unnamed: DateTimeParseException) {
    null
}
