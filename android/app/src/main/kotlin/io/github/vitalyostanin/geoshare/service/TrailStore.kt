package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Trail
import io.github.vitalyostanin.geoshare.log.AppLog
import java.io.File
import java.time.Instant

/**
 * The trail as it survives the process being killed.
 *
 * On disk rather than in the database: what the trail is for is the phone's
 * own judgement about its own answers, nobody else reads it, and the free
 * quota is counted in writes. A plain file of one line per place, because the
 * whole of it is four numbers and a file needs no schema, no migration and no
 * library.
 *
 * Losing it costs a few hours of the phone treating familiar places as new,
 * which is exactly what the phone does on a fresh install anyway — so every
 * failure here is logged and swallowed.
 */
class TrailStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun read(): Trail = try {
        if (!file.exists()) Trail() else Trail(file.readLines().mapNotNull(::placeOf))
    } catch (error: Exception) {
        AppLog.w(TAG, "the trail could not be read", error)

        Trail()
    }

    fun write(trail: Trail) {
        try {
            file.writeText(trail.fixes.joinToString("\n", transform = ::lineOf))
        } catch (error: Exception) {
            AppLog.w(TAG, "the trail could not be written", error)
        }
    }

    private fun lineOf(fix: Fix): String =
        "${fix.latitude},${fix.longitude},${fix.takenAt.toEpochMilli()},${fix.accuracyMetres ?: ""}"

    private fun placeOf(line: String): Fix? {
        val parts = line.split(",")

        if (parts.size < PARTS_IN_A_LINE) {
            return null
        }

        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        val millis = parts[2].toLongOrNull() ?: return null

        return Fix(
            latitude = latitude,
            longitude = longitude,
            takenAt = Instant.ofEpochMilli(millis),
            accuracyMetres = parts[3].toDoubleOrNull(),
        )
    }

    private companion object {
        const val FILE_NAME = "trail.csv"
        const val PARTS_IN_A_LINE = 4
        const val TAG = "geoshare.trail"
    }
}
