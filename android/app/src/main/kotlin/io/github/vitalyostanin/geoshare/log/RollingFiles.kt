package io.github.vitalyostanin.geoshare.log

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** How long a reader waits for the bytes already handed over to be written down. */
private const val SETTLE_SECONDS = 2L

/**
 * A file that is appended to, bounded in size, and readable while it is being
 * written.
 *
 * Two files rather than one, so that the record survives a rollover and still
 * has an upper bound on the space it takes: the current one is renamed over
 * the previous one when it fills, and a reader asks for both.
 *
 * Written on a thread of its own. The callers are the ones worth recording — a
 * position that arrived, an epoch of satellites — and every one of them runs on
 * the main thread, where opening a file, measuring it and closing it again is
 * the kind of work the platform complains about.
 */
class RollingFiles(
    private val directory: File,
    currentName: String,
    previousName: String,
    private val limitBytes: Long,
    threadName: String,
    /**
     * What every file starts with, the first one and each one after a
     * rollover, so that either file read alone still says what it holds.
     */
    private val head: ByteArray = ByteArray(0),
) {
    private val current = File(directory, currentName)
    private val previous = File(directory, previousName)

    /** Writers come from the service, the screen and the subscription alike. */
    private val lock = Any()

    private val writer =
        Executors.newSingleThreadExecutor { work ->
            Thread(work, threadName).apply { isDaemon = true }
        }

    init {
        if (head.isNotEmpty()) {
            writer.execute { synchronized(lock) { rollAnOlderLayout() } }
        }
    }

    fun append(bytes: ByteArray) {
        writer.execute {
            synchronized(lock) {
                directory.mkdirs()

                if (current.length() + bytes.size > limitBytes) {
                    roll()
                }

                if (current.length() == 0L) {
                    current.appendBytes(head)
                }

                current.appendBytes(bytes)
            }
        }
    }

    /**
     * Both files, oldest byte first, which is the order a reader expects.
     *
     * Everything handed over before this call is in them: a reader asking what
     * just happened would otherwise be answered without the last few lines,
     * which are exactly the ones being asked about.
     */
    fun read(): String {
        settle()

        return synchronized(lock) {
            listOf(previous, current)
                .filter { it.exists() }
                .joinToString(separator = "") { it.readText() }
        }
    }

    /** Waits for the bytes already handed over to reach the file, and no longer. */
    private fun settle() {
        try {
            writer.submit { }.get(SETTLE_SECONDS, TimeUnit.SECONDS)
        } catch (@Suppress("SwallowedException") refused: RejectedExecutionException) {
            // Nothing is being written any more, so there is nothing to wait
            // for; what is in the files is what there is. Saying so is not
            // this class's to do — it is the thing that writes what is said.
        } catch (@Suppress("SwallowedException") slow: TimeoutException) {
            // Likewise: a reader gets the bytes that reached the file, and a
            // writer stuck for two seconds is not worth holding them for.
        }
    }

    /**
     * Puts a file written under another head out of the way.
     *
     * A file the application kept appending to across a change of layout would
     * have rows under a head that no longer describes them. Only the head's
     * first line is compared: the rest names the version and the receiver,
     * which may read differently from one start to the next without the
     * columns having changed.
     */
    private fun rollAnOlderLayout() {
        if (current.length() == 0L) {
            return
        }

        val opens = head.decodeToString().substringBefore('\n')
        val opened = current.bufferedReader().use { it.readLine() }

        if (opened != opens) {
            roll()
        }
    }

    private fun roll() {
        previous.delete()
        current.renameTo(previous)
    }
}
