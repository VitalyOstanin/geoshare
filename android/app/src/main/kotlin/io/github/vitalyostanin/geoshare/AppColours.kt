package io.github.vitalyostanin.geoshare

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.vitalyostanin.geoshare.map.SOUGHT_COLOUR

/**
 * The application's colours: Material 3 with the page's accent and alarm.
 *
 * The same frame around what is selected, the same switch, the same red for a
 * search in both clients — the blue is the one on the application's icon and
 * the page's links. Everything else stays as Material 3 draws it.
 */
fun appColours(dark: Boolean): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF0B1F2E),
        primaryContainer = Color(0xFF1565C0),
        onPrimaryContainer = Color(0xFFE3F2FD),
        secondaryContainer = Color(0xFF1565C0),
        onSecondaryContainer = Color(0xFFE3F2FD),
        inversePrimary = Color(0xFF1E88E5),
        surfaceTint = Color(0xFF90CAF9),
        error = Color(0xFFEF9A9A),
        onError = Color(0xFF3B0A0A),
        errorContainer = Color(0xFF5A1D1D),
        onErrorContainer = Color(0xFFFDECEA),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF1E88E5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBBDEFB),
        onPrimaryContainer = Color(0xFF0D47A1),
        secondaryContainer = Color(0xFFBBDEFB),
        onSecondaryContainer = Color(0xFF0D47A1),
        inversePrimary = Color(0xFF90CAF9),
        surfaceTint = Color(0xFF1E88E5),
        error = Color(0xFFC62828),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFDECEA),
        onErrorContainer = Color(0xFF410E0B),
    )
}

/**
 * The red a search is written in, as words rather than a fill. By day it is
 * the pin's red; at night that red is too dark to read on a dark surface, so
 * the words take a lighter one while the fills behind white words keep theirs.
 */
fun soughtWords(dark: Boolean): Color = if (dark) Color(0xFFEF5350) else Color(SOUGHT_COLOUR)

/** [soughtWords] for the theme the screen is drawn in. */
@Composable
fun soughtWords(): Color = soughtWords(dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f)
