package io.github.vitalyostanin.geoshare.log

import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Satellite
import io.github.vitalyostanin.geoshare.collection.word
import java.io.File
import java.time.Instant
import java.util.Locale

/** What one sky file may reach before it rolls over; two files are kept. */
const val SKY_FILE_LIMIT_BYTES: Long = 2L * 1024 * 1024

/**
 * The sky as the receiver reported it, beside the position it worked out.
 *
 * Apart from the prose log because it is not prose: thirty rows an epoch, of
 * numbers a reader will want to sort and average rather than read. Apart also
 * because it can be thrown away — the prose log is what says what the
 * application did, and this is the evidence behind one line of it.
 *
 * A block per fix rather than per epoch. The receiver reports its satellites
 * about once a second, which is more than any question here is asked at; what
 * matters is what the sky looked like at the moment a position was worked out
 * from it, and that is what a block is.
 */
object SkyLog {
    @Volatile
    private var files: RollingFiles? = null

    /** Called once, from the application object. */
    fun start(directory: File) {
        files =
            RollingFiles(
                directory = directory,
                currentName = "sky.csv",
                previousName = "sky-previous.csv",
                limitBytes = SKY_FILE_LIMIT_BYTES,
                threadName = "geoshare-sky",
            )
    }

    /**
     * One block: the position, the gains the receiver is running its bands at,
     * and every satellite it can see.
     *
     * The three kinds of row are told apart by their first field, and every
     * row carries the same moment, so a block stays a block however the file
     * is later cut about.
     */
    fun note(at: Instant, place: String, gains: List<Gain>, sky: List<Satellite>) {
        val writing = files ?: return
        val stamp = at.toEpochMilli()

        val block =
            buildString {
                append("Fix,").append(stamp).append(',').append(place).append('\n')

                for (gain in gains) {
                    append("Agc,").append(stamp).append(',')
                    append(gain.constellation.word).append(',')
                    append(number(gain.carrierHz)).append(',')
                    append(number(gain.levelDb)).append('\n')
                }

                for (satellite in sky) {
                    append("Sat,").append(stamp).append(',')
                    append(satellite.constellation.word).append(',')
                    append(satellite.id).append(',')
                    append(number(satellite.loudness)).append(',')
                    append(number(satellite.basebandLoudness)).append(',')
                    append(number(satellite.elevationDegrees)).append(',')
                    append(number(satellite.azimuthDegrees)).append(',')
                    append(number(satellite.carrierHz)).append(',')
                    append(if (satellite.usedInFix) '1' else '0').append(',')
                    append(if (satellite.hasEphemeris) '1' else '0').append(',')
                    append(if (satellite.hasAlmanac) '1' else '0').append('\n')
                }
            }

        writing.append(block.toByteArray())
    }

    /** Both files, oldest row first. */
    fun read(): String = files?.read().orEmpty()

    /** A number the way a spreadsheet wants it, or nothing where there is none. */
    private fun number(value: Double?): String =
        value?.let { String.format(Locale.ROOT, "%.2f", it) }.orEmpty()
}
