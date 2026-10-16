package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Told to everybody while the database is silent, over the map where the
 * banner of a search goes: what the screen shows is what it last heard, and a
 * call would not get through.
 *
 * Ochre rather than red, the way the page draws it: a silent database is to
 * be noticed, and nothing on this screen can be done about it.
 */
@Composable
fun LinkBanner(link: LinkState) {
    val since = link.since ?: return
    val locale = appLocale()
    val clock24 = twentyFourHour()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(leftOutColour()),
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.link_lost, clockText(since, locale, clock24)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                    link.knownAt?.let {
                        stringResource(R.string.link_known_at, clockText(it, locale, clock24))
                    } ?: stringResource(R.string.link_known_never),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * What a row adds to an age while the database is silent: the ages are of
 * what the device last heard, not of now. Null while the link holds.
 */
@Composable
fun knownNote(): String? {
    val knownAt = LocalLink.current.takeIf { it.lost }?.knownAt ?: return null

    return stringResource(R.string.known_at, clockText(knownAt, appLocale(), twentyFourHour()))
}

/** A time of day, with the date as well once it is not today's. */
private fun clockText(moment: Instant, locale: Locale, twentyFourHour: Boolean): String {
    val local = moment.atZone(ZoneId.systemDefault())
    val today = local.toLocalDate() == LocalDate.now()

    val format =
        if (today) {
            clockFormat(locale, twentyFourHour)
        } else {
            DateTimeFormatter.ofPattern(
                dateAndClockPattern(FormatStyle.SHORT, locale, twentyFourHour),
                locale,
            )
        }

    return local.format(format)
}
