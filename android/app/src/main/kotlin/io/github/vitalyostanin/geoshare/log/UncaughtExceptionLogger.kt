package io.github.vitalyostanin.geoshare.log

/**
 * Puts an exception nobody caught into the application's own log before the
 * process goes.
 *
 * The system log would carry it too, but on a phone that keeps its buffers to
 * itself that is nowhere anyone can read, and the crash would leave no trace at
 * all. The previous handler is still called, once, so the process dies the way
 * it would have and the platform's own report is not lost.
 *
 * The log and the flush are handed in, so that the wrapper can be tried
 * without an application around it.
 */
class UncaughtExceptionLogger(
    private val record: (message: String, error: Throwable) -> Unit,
    private val flush: () -> Unit,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            record("uncaught on ${thread.name}", error)
            // The file is written on a thread of its own, and the process is
            // about to be killed: without waiting, the last line is the one
            // that is lost.
            flush()
        } catch (failed: Throwable) {
            // Whatever goes wrong here must not stand between the exception
            // and the handler that ends the process.
        }

        previous?.uncaughtException(thread, error)
    }

    companion object {
        /** Wraps whatever handler is in place now. */
        fun install() {
            Thread.setDefaultUncaughtExceptionHandler(
                UncaughtExceptionLogger(
                    record = { message, error -> AppLog.e("geoshare.crash", message, error) },
                    flush = AppLog::flush,
                    previous = Thread.getDefaultUncaughtExceptionHandler(),
                ),
            )
        }
    }
}
