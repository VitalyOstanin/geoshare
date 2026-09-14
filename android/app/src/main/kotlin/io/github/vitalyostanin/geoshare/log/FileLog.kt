package io.github.vitalyostanin.geoshare.log

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What one file may reach before the log rolls over; two files are kept. */
const val LOG_FILE_LIMIT_BYTES: Long = 256L * 1024

private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

/**
 * The application's own log, written to a file it owns.
 *
 * The system log is not readable on every phone: MagicOS keeps the buffers to
 * itself, and `adb logcat` there returns nothing at all. A file inside the
 * application's private directory is readable with `run-as` regardless, which
 * is what makes a check on such a phone possible.
 *
 * The file itself — two of them, bounded, written off the main thread — is
 * [RollingFiles]; this is the part that decides what a line looks like.
 */
class FileLog(
    directory: File,
    limitBytes: Long = LOG_FILE_LIMIT_BYTES,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val files =
        RollingFiles(
            directory = directory,
            currentName = "current.log",
            previousName = "previous.log",
            limitBytes = limitBytes,
            threadName = "geoshare-log",
        )

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

        files.append(line.toByteArray())
    }

    /** Both files, oldest line first, which is the order a reader expects. */
    fun read(): String = files.read()

    private fun stackTrace(error: Throwable): String {
        val text = StringWriter()

        PrintWriter(text).use { error.printStackTrace(it) }

        return text.toString()
    }
}
