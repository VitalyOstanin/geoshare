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
