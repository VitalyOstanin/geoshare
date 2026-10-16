package io.github.vitalyostanin.geoshare

/**
 * [text] cut to at most [limit] UTF-16 code units, without a lone surrogate
 * left at the end. The limit stays in code units so the application and the
 * page keep the same amount of a caption or a name; but when the cut falls
 * between the halves of a pair (an emoji in a group's name), the first half is
 * dropped too, since on its own it is not text and URLEncoder turns it into a
 * `?`.
 */
internal fun cutToLimit(text: String, limit: Int): String {
    val cut = text.take(limit)

    return if (cut.isNotEmpty() && cut.length < text.length && cut.last().isHighSurrogate()) {
        cut.dropLast(1)
    } else {
        cut
    }
}

/**
 * Characters [trimText] strips: the set the page's `String.trim()` strips
 * (ECMAScript white space and line terminators), written out so the two clients
 * cut the same edges. Kotlin's own `trim()` differs from it: it also strips the
 * four information separators U+001C..U+001F and keeps U+FEFF.
 */
private const val TRIMMED_CHARS =
    "\u0009\u000A\u000B\u000C\u000D\u0020\u00A0\u1680\u2000\u2001\u2002\u2003" +
        "\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u2028\u2029\u202F\u205F\u3000\uFEFF"

/**
 * [text] without leading and trailing characters of [TRIMMED_CHARS]. Used where
 * a value is signed or compared across clients, so that a link written by one
 * is read by the other with the same text.
 */
internal fun trimText(text: String): String = text.trim { it in TRIMMED_CHARS }
