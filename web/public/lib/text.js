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
