package io.github.vitalyostanin.geoshare.log

import android.util.Log
import java.io.File

/**
 * Where the application says what it is doing.
 *
 * Every message goes to both the system log and a file of our own: the system
 * log is the one that also carries what the platform says back, and the file is
 * the one that can be read on a phone whose system log is closed.
 */
object AppLog {
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
     * ordinary log holds an hour of a phone under a false signal, and the
     * question is asked of a day.
     */
    fun keep(tag: String, message: String) {
        w(tag, message)
        kept?.append('W', tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        file?.append('I', tag, message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        Log.w(tag, message, error)
        file?.append('W', tag, message, error)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        Log.e(tag, message, error)
        file?.append('E', tag, message, error)
    }
}
