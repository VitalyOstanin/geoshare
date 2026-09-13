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
 * guessing one before it runs out would take more attempts than a phone can
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
