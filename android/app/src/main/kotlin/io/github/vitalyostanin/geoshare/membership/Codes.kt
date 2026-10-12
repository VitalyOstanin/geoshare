package io.github.vitalyostanin.geoshare.membership

import java.security.SecureRandom
import java.time.Duration

// The code that lets one more person in.
//
// It is read off one screen and typed into another, often through somebody
// reading it aloud, so what it is made of matters as much as how random it is.

/**
 * What a code is made of.
 *
 * The letters and digits that are read for each other are left out: I and 1,
 * O and 0. The rules allow the whole of A to Z and 2 to 9, so a code from
 * anywhere else in that range is still accepted — this is what is handed out,
 * not what is let in.
 */
private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/**
 * How long a code is.
 *
 * Thirty-two characters to choose from and eight of them is about forty bits:
 * guessing one before it runs out would take more attempts than a device can
 * make, and typing one takes a moment.
 */
const val CODE_LENGTH: Int = 8

/**
 * How long an invitation lasts.
 *
 * A day, because the code cannot be spent: adding oneself to the roster and
 * marking the invitation used are two writes, and nothing makes the second
 * follow the first, so what limits a code is the time it stands rather than
 * the number of people it lets in. A day is long enough for somebody to read
 * the message in the evening and type the code in the morning, and short
 * enough that a code left in a chat is no longer a way in by the week after.
 */
val INVITATION_LASTS: Duration = Duration.ofHours(24)

/** A code nobody has had before, drawn from the alphabet above. */
fun newInvitationCode(random: SecureRandom = SecureRandom()): String = (1..CODE_LENGTH)
    .map { ALPHABET[random.nextInt(ALPHABET.length)] }
    .joinToString("")

/**
 * A typed code as the rules will see it.
 *
 * People type what they see: lower case, the spaces and dashes a long string
 * is broken up with on the screen it was read from. None of that is part of
 * the code, and rejecting it would be rejecting somebody who typed it right.
 */
fun tidyCode(typed: String): String =
    typed.filterNot { it == ' ' || it == '-' || it == '–' }.uppercase()

/**
 * Whether a tidied code is one the rules would accept at all.
 *
 * Asked before the write rather than after it, so that a mistyped code is a
 * sentence on the screen instead of a refusal from the database — and so that
 * nothing is written into the claim document, whose shape the rules hold to
 * exactly this.
 */
fun looksLikeACode(tidied: String): Boolean =
    tidied.length == CODE_LENGTH && tidied.all { it in 'A'..'Z' || it in '2'..'9' }

/** An invitation as it is handed over: which group, and the code that opens it. */
data class Invitation(val group: String, val code: String)

/**
 * An invitation as somebody pasted or typed it, read back into its two halves:
 * the group and the code joined by a dot. Nothing hands this form out any
 * more — an invitation is sent as a link — but one sent before still reads.
 *
 * The dot taken from the end rather than the start: the code has none, and a
 * group named before this rule existed might. What the halves are made of is
 * held to what the database will accept — the group becomes a document name,
 * and a name with a slash in it is a path of somebody else's choosing.
 *
 * Only the code is put into upper case. The group is an account id, where case
 * is the difference between two accounts, so it is passed on as it came with
 * nothing but the spaces a long string picks up on its way through a chat
 * taken out of it.
 */
fun invitationFrom(typed: String): Invitation? {
    val whole = typed.filterNot { it.isWhitespace() }
    val dot = whole.lastIndexOf('.')

    if (dot <= 0) {
        return null
    }

    val group = whole.substring(0, dot)
    val code = tidyCode(whole.substring(dot + 1))

    return if (looksLikeAGroupName(group) && looksLikeACode(code)) {
        Invitation(group, code)
    } else {
        null
    }
}

/**
 * Whether a name is one a group could have.
 *
 * A group is named after the account that made it, and an account id is
 * letters and digits; the household that started before that rule has a name
 * of the same shape. Held to it because the name becomes part of a document
 * path: what is refused here is a name that would reach somewhere else in the
 * database, and the refusal is a sentence on the screen rather than an error
 * from the server.
 */
fun looksLikeAGroupName(name: String): Boolean = name.isNotEmpty() &&
    name.length <= GROUP_NAME_LIMIT &&
    name.all { it.isLetterOrDigit() || it == '-' || it == '_' }

/**
 * How long a name of a group may be.
 *
 * The limit an account id is held to by the service that issues one, which is
 * longer than any id actually seen: what the number is for is stopping a name
 * of any length from being typed into a path, not describing the ids.
 */
private const val GROUP_NAME_LIMIT = 128

/**
 * How long the address on an invitation may be, the rules' own limit
 * (`invitationShape`).
 */
const val ADDRESS_LIMIT = 254

/**
 * The shape `invitationShape` holds an invitation's address to, read back here
 * so the screen that asks for one can say so before the write is tried rather
 * than after it is refused.
 */
private val ADDRESS_PATTERN = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")

/**
 * Whether a typed address is one the rules would accept for an invitation.
 *
 * Trimmed first, the way the address itself is trimmed before it is written
 * (`makeInvitation`), so a screen weighing the two agrees with what is about
 * to be saved rather than with the spaces a keyboard leaves around it.
 */
fun looksLikeAnAddress(typed: String): Boolean {
    val trimmed = typed.trim()

    return trimmed.length <= ADDRESS_LIMIT && ADDRESS_PATTERN.matches(trimmed)
}
