package io.github.vitalyostanin.geoshare.log

import io.github.vitalyostanin.geoshare.BuildConfig
import io.github.vitalyostanin.geoshare.cutToLimit
import io.github.vitalyostanin.geoshare.project.percentEncoded

/** The longest address the issue page is handed; browsers and servers refuse much past this. */
const val ISSUE_URL_LIMIT: Int = 6000

private val ISSUE_PAGE: String = BuildConfig.ISSUE_PAGE_URL
private const val DEFAULT_TITLE = "Problem report"
private const val TITLE_LIMIT = 100
private const val DESCRIPTION_LIMIT = 600
private const val LEFT_OUT = "… earlier lines left out; the full log can be attached as a file"

/**
 * The issue a member is about to open: the address to open and the part of
 * the log that address carries, so that the screen can show exactly what goes.
 */
data class IssueDraft(val url: String, val tail: String, val leftOut: Boolean)

/**
 * The first line of what the member wrote, or a stock title when they wrote
 * nothing, cut to a length that reads as a title.
 */
fun issueTitle(description: String): String =
    description.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        ?.let { cutToLimit(it, TITLE_LIMIT) }
        ?: DEFAULT_TITLE

/**
 * The address of a new issue carrying [description], the version and the tail
 * of [scrubbedLog].
 *
 * The address has a length limit, and the log is what does not fit: lines are
 * dropped from the start, whole ones, until the address is no longer than
 * [limit], and a line at the head of the block says some were. The shortest
 * answer is found by bisection, because a log of a quarter of a megabyte
 * encoded line by line on every keystroke would be felt. The model of the
 * device is not carried; the version of the system is enough to place a fault.
 */
fun buildIssue(
    description: String,
    appVersion: String,
    sdk: Int,
    scrubbedLog: String,
    limit: Int = ISSUE_URL_LIMIT,
): IssueDraft {
    val written = cutToLimit(description.trim(), DESCRIPTION_LIMIT)
    val all = scrubbedLog.trimEnd().lines().filter { it.isNotEmpty() }

    // Encoding never makes a character shorter, so lines that alone are
    // longer than the limit cannot be part of the answer; keeping them out of
    // the bisection keeps it short.
    var kept = 0
    var skipped = 0

    for (line in all.asReversed()) {
        kept += line.length + 1
        if (kept > limit) {
            break
        }
        skipped++
    }

    val cutBefore = skipped < all.size
    val lines = all.takeLast(skipped)

    fun draft(drop: Int): IssueDraft {
        val shown = lines.drop(drop)
        val dropped = cutBefore || drop > 0
        val tail = (if (dropped) listOf(LEFT_OUT) else emptyList()).plus(shown).joinToString("\n")

        return IssueDraft(
            url = issueUrl(issueTitle(written), body(written, appVersion, sdk, tail)),
            tail = tail,
            leftOut = dropped,
        )
    }

    var low = 0
    var high = lines.size

    while (low < high) {
        val middle = (low + high) / 2

        if (draft(middle).url.length <= limit) {
            high = middle
        } else {
            low = middle + 1
        }
    }

    return draft(low)
}

private fun body(description: String, appVersion: String, sdk: Int, tail: String): String =
    buildString {
        if (description.isNotEmpty()) {
            append(description)
            append("\n\n")
        }

        append("App version: ").append(appVersion).append('\n')
        append("Android SDK: ").append(sdk).append("\n\n")
        append("```\n")
        append(tail)
        append("\n```\n")
    }

private fun issueUrl(title: String, body: String): String =
    "$ISSUE_PAGE?title=${percentEncoded(title)}&body=${percentEncoded(body)}"
