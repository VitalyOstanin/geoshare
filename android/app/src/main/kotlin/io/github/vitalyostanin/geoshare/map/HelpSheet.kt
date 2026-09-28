package io.github.vitalyostanin.geoshare.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/** How a button is drawn on its screen, so the help can draw it the same. */
enum class HelpLook {
    /** A round button over the map, in the ink of the page. */
    Plain,

    /** Outlined in the alarm red, like the siren. */
    Alarm,

    /** Filled with the alarm red, like a search standing. */
    AlarmFilled,
}

/** One line of a screen's help: the button as it looks and what it does. */
data class HelpEntry(@DrawableRes val icon: Int, val look: HelpLook, val text: String)

/** The question mark that opens a screen's help, in the corner of its title. */
@Composable
fun HelpButton(onClick: () -> Unit) {
    OutlinedIconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_help),
            contentDescription = stringResource(R.string.help_open),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * What the buttons of one screen do, drawn the way the screen draws them.
 *
 * A button carrying a picture rather than a word saves the row its width and
 * says nothing to somebody who has not pressed it yet; this says it, on the
 * screen where the question comes up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(title: String, entries: List<HelpEntry>, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)

            for (entry in entries) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HelpMark(entry)
                    Text(text = entry.text, style = MaterialTheme.typography.bodyMedium)
                }
            }

            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(R.string.help_close))
            }
        }
    }
}

@Composable
private fun HelpMark(entry: HelpEntry) {
    val alarm = Color(SOUGHT_COLOUR)
    val (ground, ink, edge) =
        when (entry.look) {
            HelpLook.Plain ->
                Triple(
                    Color.Transparent,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.outlineVariant,
                )

            HelpLook.Alarm -> Triple(Color.Transparent, alarm, alarm)

            HelpLook.AlarmFilled -> Triple(alarm, Color.White, alarm)
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .size(36.dp)
            .background(ground, CircleShape)
            .border(BorderStroke(1.dp, edge), CircleShape),
    ) {
        Icon(
            painter = painterResource(entry.icon),
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(20.dp),
        )
    }
}
