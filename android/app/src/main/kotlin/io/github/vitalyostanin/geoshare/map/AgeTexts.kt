package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
private const val AGE_TICK_MILLIS = 30_000L

/** The moment itself, for the card a member opens rather than for the buttons. */
@Composable
internal fun momentText(moment: Instant): String {
    val format =
        remember {
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
        }

    return format.format(moment)
}

@Composable
internal fun ageText(age: PositionAge): String = when (age) {
    PositionAge.JustNow -> stringResource(R.string.map_age_just_now)
    is PositionAge.Minutes -> pluralAge(R.plurals.map_age_minutes, age.count)
    is PositionAge.Hours -> pluralAge(R.plurals.map_age_hours, age.count)
    is PositionAge.Days -> pluralAge(R.plurals.map_age_days, age.count)
}

/**
 * The same age in as few characters as it can be said in.
 *
 * A card of the panel holds a face, a name and an age across a third of a
 * phone; written out in full the age lost its last word to the edge of the
 * card, which reads as a broken card rather than as a short one.
 */
@Composable
internal fun shortAgeText(age: PositionAge): String = when (age) {
    PositionAge.JustNow -> stringResource(R.string.map_age_short_now)
    is PositionAge.Minutes -> stringResource(R.string.map_age_short_minutes, age.count)
    is PositionAge.Hours -> stringResource(R.string.map_age_short_hours, age.count)
    is PositionAge.Days -> stringResource(R.string.map_age_short_days, age.count)
}

@Composable
private fun pluralAge(plural: Int, count: Long): String =
    pluralStringResource(plural, count.toInt(), count.toInt())

/**
 * A clock that ticks slowly enough to be free and often enough to be right.
 *
 * Without it the ages would only change when a position does, and a member who
 * has gone quiet — the case worth noticing — is exactly the one whose entry
 * would never update.
 */
@Composable
internal fun currentTime() = produceState(initialValue = Instant.now()) {
    while (true) {
        delay(AGE_TICK_MILLIS)
        value = Instant.now()
    }
}
