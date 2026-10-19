package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.leftOutReasonLabel
import io.github.vitalyostanin.geoshare.collection.sourceChoiceLabel
import io.github.vitalyostanin.geoshare.collection.sourceLeftOut
import io.github.vitalyostanin.geoshare.soughtWords
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * When a search ends, short enough for a narrow card of the strip: the time
 * alone while the search ends today, the day as well once it does not. The
 * full form, with the day always, stays on the open card, where there is room.
 */
internal fun soughtEndsShort(
    endsAt: Instant,
    now: Instant,
    locale: Locale,
    twentyFourHour: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val ends = endsAt.atZone(zone)

    return if (ends.toLocalDate() == now.atZone(zone).toLocalDate()) {
        clockFormat(locale, twentyFourHour).format(ends)
    } else {
        endsAtText(ends, locale, twentyFourHour)
    }
}

/**
 * Told on the card of a member the group is looking for: until when, who
 * opened the search, and, to whoever runs the group, the button that ends it.
 *
 * A row of its own rather than one more fact in [DetailSummary]: this is not a
 * fact about the position, it is a fact about what reading it means, and it
 * carries a colour of its own for the same reason a warning does.
 */
@Composable
internal fun DetailSought(
    search: Search,
    openedByName: String,
    showFound: Boolean,
    onFound: suspend () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        // One line rather than two: "ищем до … · Открыл(а): …" is read as one
        // fact about the position, the way the summary above it is.
        Text(
            text =
            stringResource(
                R.string.sought_until,
                endsAtText(search.endsAt.atZone(ZoneId.systemDefault())),
            ) + " · " + stringResource(R.string.search_opened_by, openedByName),
            style = MaterialTheme.typography.bodySmall,
            color = soughtWords(),
            modifier = Modifier.weight(1f),
        )

        if (showFound) {
            TextButton(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            onFound()
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) {
                Text(text = stringResource(R.string.search_found), color = soughtWords())
            }
        }
    }
}

/**
 * What is wrong with the position, as one paragraph, or nothing when nothing is.
 *
 * Each sentence only while it is true: a marker left behind by a device that is
 * still in touch, answers being rejected, a receiver that has not settled. Put
 * together rather than as rows, because they are read as one account of why
 * the marker should not be trusted. The reason is underlined and pressed like
 * the mode: it names what the device saw without saying what it means.
 */
@Composable
internal fun DetailWarnings(member: MemberPosition, now: Instant) {
    val stuck = isStuck(member.positionAt, member.knownAt, now)
    val count = member.leftOutCount?.takeIf { it > 0 }

    if (!member.inFalsePlace && !stuck && count == null && !member.settling) {
        return
    }

    var helpOpen by remember { mutableStateOf(false) }
    // The language the card is written in, read so that a change of it
    // redraws the card: the default locale changes under Compose unseen.
    val locale = LocalConfiguration.current.locales[0]

    val stuckSentence =
        stringResource(
            when (stuckCause(member.leftOutCount)) {
                StuckCause.Rejected -> R.string.map_detail_stuck_sentence
                StuckCause.Silent -> R.string.map_detail_stuck_silent_sentence
            },
            ageText(positionAge(member.knownAt, now)),
        ).takeIf { stuck }
    val lead =
        count?.let {
            stringResource(
                R.string.map_detail_left_out_lead,
                pluralStringResource(R.plurals.map_detail_left_out_answers, it, it),
            )
        }
    val reason = leftOutReasonText(member.leftOutReason)
    val settling = stringResource(R.string.map_detail_settling_sentence).takeIf { member.settling }
    // The answers refused for lying in the same place are that place again, so
    // they are counted into its sentence rather than given one of their own.
    val refusedHere = member.inFalsePlace && count != null &&
        member.leftOutReason == JumpReason.FalsePlace.name
    val (falsePlaceLead, falsePlace) = falsePlaceWords(member, count.takeIf { refusedHere }, locale)
    val reasonLink =
        LinkAnnotation.Clickable(
            tag = "reason",
            styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
        ) { helpOpen = true }

    val paragraph =
        warningParagraph(
            sentences = WarningSentences(falsePlaceLead, falsePlace, stuckSentence, lead, settling),
            reason = reason.replaceFirstChar { it.lowercase(locale) },
            reasonLink = reasonLink,
            refusedHere = refusedHere,
        )

    Text(
        text = paragraph,
        style = MaterialTheme.typography.bodyMedium,
        color = leftOutColour(),
        modifier = Modifier.padding(top = 6.dp),
    )

    if (helpOpen) {
        ReasonHelp(onClose = { helpOpen = false })
    }
}

/**
 * How the false place is told: the lead sentence, null when the member is not
 * in one, and the words the link on it carries. [refusedCount] is the answers
 * refused for lying there, when they are counted into its sentence.
 */
@Composable
private fun falsePlaceWords(
    member: MemberPosition,
    refusedCount: Int?,
    locale: Locale,
): Pair<String?, String> {
    val lead =
        when {
            refusedCount != null ->
                stringResource(R.string.map_detail_false_place_and_after, refusedCount)

            member.inFalsePlace -> stringResource(R.string.map_detail_false_place_lead)

            else -> null
        }
    val where =
        if (refusedCount != null) {
            stringResource(R.string.map_detail_false_place_where)
        } else {
            stringResource(leftOutReasonLabel(JumpReason.FalsePlace.name))
                .replaceFirstChar { it.lowercase(locale) }
        }

    return lead to where
}

/** The sentences [warningParagraph] puts together, each null while it is not true. */
private class WarningSentences(
    val falsePlaceLead: String?,
    val falsePlace: String,
    val stuck: String?,
    val leftOutLead: String?,
    val settling: String?,
)

/**
 * The sentences in the order they are read, one space between those that are
 * there. The false place and the left-out answers each end in a link on their
 * reason; [refusedHere] means the first already counted the second.
 */
private fun warningParagraph(
    sentences: WarningSentences,
    reason: String,
    reasonLink: LinkAnnotation,
    refusedHere: Boolean,
): AnnotatedString = buildAnnotatedString {
    sentences.falsePlaceLead?.let {
        append(it)
        append(' ')
        withLink(reasonLink) { append(sentences.falsePlace) }
        append('.')
    }

    sentences.stuck?.let {
        if (length > 0) append(' ')

        append(it)
    }

    if (sentences.leftOutLead != null && !refusedHere) {
        if (length > 0) append(' ')

        append(sentences.leftOutLead)
        append(' ')
        withLink(reasonLink) { append(reason) }
        append('.')
    }

    sentences.settling?.let {
        if (length > 0) append(' ')

        append(it)
    }
}

/**
 * Why answers were left out, in words. A source the member switched off is
 * named: which of the five it was is what they open the card to find out.
 */
@Composable
private fun leftOutReasonText(leftOutReason: String?): String = sourceLeftOut(leftOutReason)
    ?.let {
        stringResource(
            R.string.left_out_by_source_named,
            stringResource(sourceChoiceLabel(it)),
        )
    }
    ?: stringResource(leftOutReasonLabel(leftOutReason))
