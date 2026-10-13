package io.github.vitalyostanin.geoshare.log

/**
 * What the application knows about itself that a log must not carry out.
 *
 * Handed in by the caller, because only the caller knows which project the
 * device is connected to and what the groups and the people are called; the
 * scrubbing does not guess at names. Each is cut out word for word, wherever it
 * stands.
 */
data class Known(
    val projectIds: List<String> = emptyList(),
    val groupNames: List<String> = emptyList(),
    val sites: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val names: List<String> = emptyList(),
)

// A connection link, whole: the site, the group and the code all stand in its
// fragment (ConnectLink). Taken before anything else, so that the site name
// inside it is not replaced on its own and leave the rest of the link behind.
// A message's own punctuation after the link is left standing.
private val LINK = Regex(
    """https://[a-z][a-z0-9-]*\.(?:web\.app|firebaseapp\.com)/join\S*?""" +
        """(?=[).,;:!?\]}'"<>]*(?:\s|$))""",
)

// A code written next to the word for it; the code alone has nothing to tell
// it from any other eight capitals (membership/Codes.kt).
private val CODE = Regex("""((?i:\bcode\b)\W{0,3})[A-Z2-9-]{8,12}\b""")

// "%.5f,%.5f" as the fix lines and the false places write a position, and any
// other pair of decimals with a comma between that is plainly a position.
private val PLACE = Regex("""-?\d{1,3}\.\d{4,}\s*,\s*-?\d{1,3}\.\d{4,}""")

private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

// A member id is a Firebase uid, twenty-eight letters and digits; a device id
// is the platform's sixteen hex digits, or a UUID when the platform gave none
// (DeviceIdentity). The uid is looked for first so that a hex-looking stretch
// of one is not cut out of it.
private val ID = Regex(
    """(?<![A-Za-z0-9])(?:[0-9A-Za-z]{28}|""" +
        """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|""" +
        """[0-9a-f]{16})(?![A-Za-z0-9])""",
)

/**
 * The text of a log with what identifies people and places taken out.
 *
 * Replacements, in this order so that one does not break another: invitations
 * and places first (they contain pieces the later ones would cut), then the
 * known strings (the site before the project id it contains, and within each
 * kind the longest first, so that a short name inside a longer one does not
 * win), then addresses and ids by shape. Ids are numbered `‹member 1›`,
 * `‹member 2›`… by first appearance, the same id getting the same number
 * within one call, so that a reader can still tell who did what.
 */
fun scrubLog(text: String, known: Known): String {
    var out = LINK.replace(text, "‹invitation›")
    out = CODE.replace(out) { "${it.groupValues[1]}‹invitation›" }
    out = PLACE.replace(out, "‹place›")
    out = cutOut(out, known.emails, "‹email›")
    out = cutOut(out, known.sites, "‹site›")
    out = cutOut(out, known.projectIds, "‹project›")
    out = cutOut(out, known.groupNames, "‹group›")
    out = cutOut(out, known.names, "‹name›")
    out = EMAIL.replace(out, "‹email›")
    val numbers = LinkedHashMap<String, Int>()
    return ID.replace(out) { found ->
        val number = numbers.getOrPut(found.value) { numbers.size + 1 }
        "‹member $number›"
    }
}

private fun cutOut(text: String, strings: List<String>, replacement: String): String =
    strings.filter { it.isNotBlank() }
        .sortedByDescending { it.length }
        .fold(text) { rest, string -> rest.replace(string, replacement, ignoreCase = true) }
