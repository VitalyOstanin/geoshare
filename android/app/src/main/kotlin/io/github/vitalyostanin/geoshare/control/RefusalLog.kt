package io.github.vitalyostanin.geoshare.control

/**
 * Whether a refusal is worth a line in the log: the first of its kind, and again once
 * [intervalMillis] have passed. A caller retrying in a loop would otherwise fill the file with
 * the same line, and the one line is what tells a wrong digest in the configuration apart from a
 * caller that is not on the list at all.
 */
internal class RefusalLog(
    private val intervalMillis: Long = 60_000,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val lastWritten = mutableMapOf<String, Long>()

    fun shouldWrite(key: String): Boolean {
        val at = now()
        val last = lastWritten[key]
        if (last != null && at - last < intervalMillis) return false
        lastWritten[key] = at
        return true
    }
}
