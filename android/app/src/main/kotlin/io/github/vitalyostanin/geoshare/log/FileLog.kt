package io.github.vitalyostanin.geoshare.log

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** What one file may reach before the log rolls over; two files are kept. */
const val LOG_FILE_LIMIT_BYTES: Long = 256L * 1024

/** How long a reader waits for the lines already said to be written down. */
private const val SETTLE_SECONDS = 2L

private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

/**
 * The application's own log, written to a file it owns.
 *
 * The system log is not readable on every phone: MagicOS keeps the buffers to
 * itself, and `adb logcat` there returns nothing at all. A file inside the
 * application's private directory is readable with `run-as` regardless, which
 * is what makes a check on such a phone possible.
 *
 * Two files are kept, so the log survives a rollover and still has an upper
 * bound on the space it takes.
 */
class FileLog(
    private val directory: File,
    private val limitBytes: Long = LOG_FILE_LIMIT_BYTES,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val current = File(directory, "current.log")
    private val previous = File(directory, "previous.log")

    /** Writers come from the service, the screen and the subscription alike. */
    private val lock = Any()

    /**
     * The one thread the file is written on.
     *
     * The callers are the ones a line is worth writing about: a position that
     * arrived, a subscription that answered, a permission that was granted —
     * and every one of them runs on the main thread, where opening a file,
     * measuring it and closing it again is the kind of work the platform
     * complains about. The line is built where it is said and put in the file
     * a moment later.
     */
    private val writer =
        Executors.newSingleThreadExecutor { work ->
            Thread(work, "geoshare-log").apply {
                isDaemon =
                    true
            }
        }

    fun append(
        level: Char,
        tag: String,
        message: String,
        error: Throwable? = null,
        at: Instant = Instant.now(),
    ) {
        val line =
            buildString {
                append(STAMP.format(at.atZone(zone)))
                append(' ')
                append(level)
                append('/')
                append(tag)
                append(": ")
                append(message)
                append('\n')

                if (error != null) {
                    append(stackTrace(error))
                }
            }

        val bytes = line.toByteArray()

        writer.execute {
            synchronized(lock) {
                directory.mkdirs()

                if (current.length() + bytes.size > limitBytes) {
                    roll()
                }

                current.appendBytes(bytes)
            }
        }
    }

    /**
     * Both files, oldest line first, which is the order a reader expects.
     *
     * Everything said before this call is in them: a reader asking what just
     * happened would otherwise be answered without the last few lines, which
     * are exactly the ones being asked about.
     */
    fun read(): String {
        settle()

        return synchronized(lock) {
            listOf(previous, current)
                .filter { it.exists() }
                .joinToString(separator = "") { it.readText() }
        }
    }

    /** Waits for the lines already said to reach the file, and no longer. */
    private fun settle() {
        try {
            writer.submit { }.get(SETTLE_SECONDS, TimeUnit.SECONDS)
        } catch (@Suppress("SwallowedException") refused: RejectedExecutionException) {
            // Nothing is being written any more, so there is nothing to wait
            // for; what is in the files is what there is. Saying so is not
            // this class's to do — it is the thing that writes what is said.
        } catch (@Suppress("SwallowedException") slow: TimeoutException) {
            // Likewise: a reader gets the lines that reached the file, and a
            // writer stuck for two seconds is not worth holding them for.
        }
    }

    private fun roll() {
        previous.delete()
        current.renameTo(previous)
    }

    private fun stackTrace(error: Throwable): String {
        val text = StringWriter()

        PrintWriter(text).use { error.printStackTrace(it) }

        return text.toString()
    }
}
