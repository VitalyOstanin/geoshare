package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One library the application carries, as the licences screen lists it. */
internal data class LicenceLine(
    val name: String,
    val version: String?,
    /** What it comes under, by SPDX id where the library names one. */
    val labels: List<String>,
    /** The full texts, one after another, for the dialog a tap opens. */
    val text: String,
)

/**
 * The lines of the screen, read off the list the build collected from the
 * dependencies: by name, so that a library is found where one looks for it.
 *
 * A licence the list carries no text for is given by its address instead;
 * a licence is a notice to be read, and an empty dialog reads as nothing.
 */
internal fun licenceLines(json: String): List<LicenceLine> = Libs
    .Builder()
    .withJson(json)
    .build()
    .libraries
    .map { library ->
        // A library given a text of its own in config/libraries keeps the
        // licence its POM names as well, and that SPDX text has a blank where
        // the holder goes: the plugin adds the one to the other rather than
        // replacing it. Where both are there, the text naming the holder
        // stands for the licence.
        val ownTexts =
            library.licenses
                .filter { it.hash != it.spdxId }
                .mapNotNull { it.spdxId }
                .toSet()
        val licences =
            library.licenses
                .filterNot { it.hash == it.spdxId && it.spdxId in ownTexts }
                .sortedBy { it.spdxId ?: it.name }

        LicenceLine(
            name = library.name,
            version = library.artifactVersion,
            labels = licences.map { it.spdxId ?: it.name },
            text =
            licences.joinToString(separator = "\n\n") { licence ->
                licence.licenseContent?.replace("<br />", "")?.trim()
                    ?: licence.url
                    ?: licence.name
            },
        )
    }.sortedBy { it.name.lowercase() }

/**
 * Every library inside the application and the licence it comes under, as
 * those licences ask of whoever passes the library on: the Apache licence
 * wants its text handed to the recipient, the BSD ones their notice in the
 * material that comes with the program.
 *
 * Drawn over the settings it is opened from, like the device list.
 */
@Composable
fun LicencesScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    // Read off the main thread: the list carries the full text of every
    // licence.
    val lines by produceState<List<LicenceLine>?>(initialValue = null) {
        value =
            withContext(Dispatchers.IO) {
                val json =
                    resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use {
                        it.readText()
                    }

                licenceLines(json)
            }
    }
    var shown by remember { mutableStateOf<LicenceLine?>(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.licences_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
            }

            HorizontalDivider()

            LazyColumn {
                item {
                    Text(
                        text = stringResource(R.string.licences_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )

                    HorizontalDivider()
                }

                items(lines.orEmpty(), key = { "${it.name}:${it.version}" }) { line ->
                    LicenceRow(line = line, onOpen = { shown = line })

                    HorizontalDivider()
                }
            }
        }
    }

    shown?.let { line ->
        AlertDialog(
            onDismissRequest = { shown = null },
            title = { Text(text = line.name) },
            text = {
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { shown = null }) {
                    Text(text = stringResource(R.string.settings_close))
                }
            },
        )
    }
}

/** A library: its name and version on one line, its licences as labels under it. */
@Composable
private fun LicenceRow(line: LicenceLine, onOpen: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = line.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            line.version?.let { version ->
                Text(
                    text = version,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            line.labels.forEach { label ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
