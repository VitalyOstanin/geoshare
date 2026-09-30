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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** When a search ends: the day as well as the time, the same reading a permission's end has. */
internal val SOUGHT_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

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
                SOUGHT_ENDS_AT.format(search.endsAt.atZone(ZoneId.systemDefault())),
            ) + " · " + stringResource(R.string.search_opened_by, openedByName),
            style = MaterialTheme.typography.bodySmall,
            color = Color(SOUGHT_COLOUR),
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
                Text(text = stringResource(R.string.search_found), color = Color(SOUGHT_COLOUR))
            }
        }
    }
}

/**
 * What is wrong with the position, as one paragraph, or nothing when nothing is.
 *
 * Each sentence only while it is true: a marker left behind by a phone that is
 * still online, answers being rejected, a receiver that has not settled. Put
 * together rather than as rows, because they are read as one account of why
 * the marker should not be trusted. The reason is underlined and pressed like
 * the mode: it names what the phone saw without saying what it means.
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
            R.string.map_detail_stuck_sentence,
            ageText(positionAge(member.knownAt, now)),
        ).takeIf { stuck }
    val lead =
        count?.let {
            stringResource(
                R.string.map_detail_left_out_lead,
                pluralStringResource(R.plurals.map_detail_left_out_answers, it, it),
            )
        }
    // A source the member switched off is named: which of the five it was is
    // what they open the card to find out.
    val reason =
        sourceLeftOut(member.leftOutReason)
            ?.let {
                stringResource(
                    R.string.left_out_by_source_named,
                    stringResource(sourceChoiceLabel(it)),
                )
            }
            ?: stringResource(leftOutReasonLabel(member.leftOutReason))
    val settling = stringResource(R.string.map_detail_settling_sentence).takeIf { member.settling }
    // The answers refused for lying in the same place are that place again, so
    // they are counted into its sentence rather than given one of their own.
    val refusedHere = member.inFalsePlace && count != null &&
        member.leftOutReason == JumpReason.FalsePlace.name
    val falsePlaceLead =
        when {
            refusedHere -> stringResource(R.string.map_detail_false_place_and_after, count ?: 0)
            member.inFalsePlace -> stringResource(R.string.map_detail_false_place_lead)
            else -> null
        }
    val falsePlace =
        if (refusedHere) {
            stringResource(R.string.map_detail_false_place_where)
        } else {
            stringResource(leftOutReasonLabel(JumpReason.FalsePlace.name))
                .replaceFirstChar { it.lowercase(locale) }
        }
    val reasonLink =
        LinkAnnotation.Clickable(
            tag = "reason",
            styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
        ) { helpOpen = true }

    val paragraph =
        buildAnnotatedString {
            falsePlaceLead?.let {
                append(it)
                append(' ')
                withLink(reasonLink) { append(falsePlace) }
                append('.')
            }

            stuckSentence?.let {
                if (length > 0) append(' ')

                append(it)
            }

            if (lead != null && !refusedHere) {
                if (length > 0) append(' ')

                append(lead)
                append(' ')
                withLink(reasonLink) {
                    append(reason.replaceFirstChar { it.lowercase(locale) })
                }
                append('.')
            }

            settling?.let {
                if (length > 0) append(' ')

                append(it)
            }
        }

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
