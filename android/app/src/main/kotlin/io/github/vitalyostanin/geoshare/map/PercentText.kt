package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import java.text.NumberFormat
import java.util.Locale

/**
 * A charge level written the way [locale] writes a percentage: "64%" in
 * English, with a space before the sign in Russian.
 */
internal fun percentText(percent: Int, locale: Locale): String =
    NumberFormat.getPercentInstance(locale).format(percent / 100.0)

/** The same, in the app's current language. */
@Composable
internal fun percentText(percent: Int): String = percentText(percent, appLocale())
