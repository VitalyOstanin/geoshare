package io.github.vitalyostanin.geoshare.collection

import java.util.Locale

// What the satellites are saying, as against how loud they are.
//
// The status says a satellite was heard and the raw measurements say what was
// measured off it. This is the content of the signal itself: the orbit a
// satellite broadcasts for itself, the rough orbits of its whole system, its
// health flags and the corrections to its own clock. A transmitter drawing a
// false road has to fill those in with something, and what it fills them in
// with is the one part of a false sky that can be read rather than measured.
//
// Whether a receiver hands them over at all is a property of the chipset, and
// not one every chipset has. That is the first question here, and the answer
// is worth writing down whichever way it goes: a phone that never had them is
// a phone none of this can be asked of.

/** How the top byte of a message type carries the system it belongs to. */
private const val SYSTEM_BYTE = 8

/** How many satellite-and-page combinations are remembered at once. */
private const val PAGES_REMEMBERED = 512

/** One navigation message, as the receiver handed it over. */
data class NavMessage(
    /** The system, which the receiver states as the top byte of [type]. */
    val constellation: Constellation,
    /** The receiver's own number for the satellite, unique within its system. */
    val id: Int,
    /** Which signal the words came on, and in which of that signal's formats. */
    val type: Int,
    /** Whether the words passed their parity checks, were rebuilt, or neither. */
    val status: Int,
    val messageId: Int,
    val submessageId: Int,
    /**
     * The words themselves, hexadecimal, two characters to the byte.
     *
     * Google's own logger writes them as decimal numbers separated by commas,
     * which leaves a row's field count depending on the length of the message.
     * The same bytes are here, in a form one field can hold.
     */
    val data: String,
)

/** How many messages a system has produced, and from how many satellites. */
data class NavSeen(val messages: Int, val satellites: Int)

/**
 * The system a message belongs to, read off the top byte of its type.
 *
 * The receiver numbers the systems the same way it numbers them in the status,
 * so the two files name the same system by the same word. Read as arithmetic
 * rather than against the platform's constants, because the list of those
 * grows with the Android version while the numbering does not change.
 */
fun navConstellation(type: Int): Constellation = when (type shr SYSTEM_BYTE) {
    1 -> Constellation.Gps
    2 -> Constellation.Sbas
    3 -> Constellation.Glonass
    4 -> Constellation.Qzss
    5 -> Constellation.Beidou
    6 -> Constellation.Galileo
    7 -> Constellation.Irnss
    else -> Constellation.Other
}

/**
 * The words for the signal a message came on.
 *
 * The type is kept as the number as well, in the file: a phone newer than this
 * code will name signals this does not know, and the number is what lets them
 * be looked up later rather than lost as "other".
 */
@Suppress("MagicNumber")
fun navWord(type: Int): String = when (type) {
    0x0101 -> "GPS L1 C/A"
    0x0102 -> "GPS L2 CNAV"
    0x0103 -> "GPS L5 CNAV"
    0x0104 -> "GPS CNAV2"
    0x0201 -> "SBAS"
    0x0301 -> "GLONASS L1 C/A"
    0x0401 -> "QZSS L1 C/A"
    0x0501 -> "BeiDou D1"
    0x0502 -> "BeiDou D2"
    0x0503 -> "BeiDou CNAV1"
    0x0504 -> "BeiDou CNAV2"
    0x0601 -> "Galileo I"
    0x0602 -> "Galileo F"
    0x0701 -> "IRNSS L5 C/A"
    0x0702 -> "IRNSS L5"
    0x0703 -> "IRNSS L1"
    else -> String.format(Locale.ROOT, "type 0x%04X", type)
}

/**
 * Whether the message arrived intact.
 *
 * "Rebuilt" is the interesting one: the receiver found the parity wrong and
 * worked out what the words must have been. A signal being talked over by a
 * stronger one produces exactly that, and so does a satellite low on the
 * horizon, so it counts for nothing on its own and for something beside the
 * loudness of the satellite it came from.
 */
fun navStatusWord(status: Int): String = when (status) {
    1 -> "passed"
    2 -> "rebuilt"
    else -> "unknown"
}

/** Bytes as a spreadsheet can hold them: two characters each, no separators. */
fun hexOf(bytes: ByteArray): String =
    bytes.joinToString("") { String.format(Locale.ROOT, "%02X", it) }

/**
 * What has already been written down, and what is worth writing next.
 *
 * A satellite repeats itself until it has something else to say. The GPS
 * almanac takes twelve and a half minutes to come round in full, and the same
 * pages arrive again and again in between; writing every repeat would fill the
 * sky file within hours and push out everything else in it. A message is
 * written when its words differ from the last written for that satellite, that
 * signal and that page — which is precisely when the satellite has said
 * something new.
 *
 * The repeats are not lost, only not written twice: how many arrived is what
 * says how much the receiver is hearing, and that is counted here.
 */
class NavSoFar {
    private val written = LinkedHashMap<String, String>()
    private val satellites = mutableMapOf<Constellation, MutableSet<Int>>()
    private val messages = mutableMapOf<Constellation, Int>()

    private var heard = 0
    private var kept = 0

    /** Counts the message, and says whether it carries anything new. */
    fun worthWriting(message: NavMessage): Boolean {
        heard += 1
        messages[message.constellation] = (messages[message.constellation] ?: 0) + 1
        satellites.getOrPut(message.constellation) { mutableSetOf() }.add(message.id)

        val said = written.put(keyOf(message), message.data)

        trim()

        if (said == message.data) {
            return false
        }

        kept += 1

        return true
    }

    /** How much each system has said, for the summary and for the tests. */
    fun counted(): Map<Constellation, NavSeen> = messages.mapValues { (system, count) ->
        NavSeen(messages = count, satellites = satellites[system]?.size ?: 0)
    }

    /**
     * The same in the words of the log.
     *
     * Said even when there is nothing, because nothing is the answer this was
     * written to find out: a chipset that hands over no navigation messages at
     * all is indistinguishable, in a file, from one nobody subscribed to.
     */
    fun saying(): String {
        if (heard == 0) {
            return "the satellites have said nothing: no navigation messages at all"
        }

        val said =
            inOrder(counted().keys).joinToString(", ") { system ->
                val seen = counted()[system] ?: NavSeen(messages = 0, satellites = 0)

                "${system.word} ${seen.messages} from ${seen.satellites}"
            }

        return "the satellites are saying $said; $kept of $heard messages carried something new"
    }

    /**
     * The satellite, the signal and the page, which together name the thing
     * that repeats.
     */
    private fun keyOf(message: NavMessage): String =
        "${message.type}/${message.id}/${message.messageId}/${message.submessageId}"

    private fun trim() {
        while (written.size > PAGES_REMEMBERED) {
            val oldest = written.keys.firstOrNull() ?: return

            written.remove(oldest)
        }
    }
}
