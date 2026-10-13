package io.github.vitalyostanin.geoshare.log

import android.util.Log
import io.github.vitalyostanin.geoshare.BuildConfig
import java.io.File

/**
 * Where the application says what it is doing.
 *
 * Every message goes to a file of our own, and in a debug build to the system
 * log as well: the system log is the one that also carries what the platform
 * says back, and the file is the one that can be read on a device whose system
 * log is closed.
 *
 * A release build keeps out of the system log, because the messages carry
 * positions to the metre and the system log is readable by whatever holds the
 * permission to read it; the file stays private to the application.
 */
object AppLog {
    /** Whether messages also go to the system log; a test sets it either way. */
    @Volatile
    internal var toSystemLog: Boolean = BuildConfig.DEBUG

    @Volatile
    private var file: FileLog? = null

    @Volatile
    private var kept: FileLog? = null

    /** Called once, from the application object. */
    fun start(directory: File) {
        file = FileLog(directory)
        kept =
            FileLog(
                directory,
                limitBytes = KEPT_FILE_LIMIT_BYTES,
                currentName = "kept.log",
                previousName = "kept-previous.log",
            )
    }

    /**
     * A warning that is also kept apart from the ordinary log, where it would
     * be rolled away within the hour.
     *
     * For the few observations a threshold is waiting to be measured on: the
     * ordinary log holds an hour of a device under a false signal, and the
     * question is asked of a day.
     */
    fun keep(tag: String, message: String) {
        w(tag, message)
        kept?.append('W', tag, message)
    }

    fun i(tag: String, message: String) {
        if (toSystemLog) {
            Log.i(tag, message)
        }
        file?.append('I', tag, message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        if (toSystemLog) {
            Log.w(tag, message, error)
        }
        file?.append('W', tag, message, error)
    }

    /** Lets the file catch up with what was said; see [FileLog.flush]. */
    fun flush() {
        file?.flush()
        kept?.flush()
    }

    /** The ordinary log as it stands, oldest line first; empty before [start]. */
    fun read(): String {
        file?.flush()

        return file?.read().orEmpty()
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (toSystemLog) {
            Log.e(tag, message, error)
        }
        file?.append('E', tag, message, error)
    }
}
