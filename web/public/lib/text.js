// Small things done to text that more than one part of the page needs.

/**
 * [value] cut to at most [limit] UTF-16 code units, without a lone surrogate
 * left at the end. The limit stays in code units so this page and the
 * application keep the same amount of a caption or a name; but when the cut
 * falls between the halves of a pair (an emoji in a group's name), the first
 * half is dropped too, since on its own it is not text and makes
 * encodeURIComponent throw.
 */
export function cutToLimit(value, limit) {
    const cut = value.slice(0, limit);
    const last = cut.charCodeAt(cut.length - 1);
    const splitsPair = last >= 0xd800 && last <= 0xdbff && value.length > cut.length;

    return splitsPair ? cut.slice(0, -1) : cut;
}

/**
 * [value] without leading and trailing white space, by an explicit set rather
 * than String.prototype.trim(). The set is the one trim() strips, written out
 * because the application's trimText must strip exactly the same characters;
 * Kotlin's own trim() differs (it strips U+001C..U+001F and keeps U+FEFF).
 */
export function trimText(value) {
    return value.replace(TRIMMED_EDGES, '');
}

const SPACE = '[\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029'
    + '\\u202F\\u205F\\u3000\\uFEFF]+';
const TRIMMED_EDGES = new RegExp(`^${SPACE}|${SPACE}$`, 'g');
