package io.github.vitalyostanin.geoshare.map

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.vitalyostanin.geoshare.BuildConfig
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.Known
import io.github.vitalyostanin.geoshare.log.buildIssue
import io.github.vitalyostanin.geoshare.log.scrubLog
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "geoshare.report"

/**
 * What the application knows that a log must not carry out: the projects and
 * sites it is connected to, the groups' names, the account's address and name,
 * and the names of people and devices it holds locally. Nothing is asked of
 * the network; what is not known here is left to the shapes [scrubLog] looks
 * for.
 */
internal fun knownToTheApplication(
    context: Context,
    profiles: Map<String, MemberProfile>,
    labels: Labels,
): Known {
    val connections = Connections.all.value
    val told = knownOrSaid("the published profile") { PublishedProfile(context).told() }
    val accounts =
        connections.mapNotNull { connection ->
            knownOrSaid("the account of ${connection.projectId}") { connection.auth.currentUser }
        }

    return Known(
        projectIds = connections.map { it.projectId },
        groupNames =
        connections.flatMap { listOfNotNull(it.group, GroupNames.of(context, it.projectId)) },
        sites = connections.map { it.settings.site },
        emails = accounts.mapNotNull { it.email },
        names =
        listOfNotNull(told?.displayName) +
            accounts.mapNotNull { it.displayName } +
            profiles.values.map { it.displayName } +
            labels.phones.values +
            labels.members.values,
    )
}

/**
 * What [read] gives, or null when it fails, said in the log: whatever [what]
 * holds then goes into the report unscrubbed, and the member reading the log
 * before sending it is the one left to notice.
 */
internal fun <T> knownOrSaid(what: String, read: () -> T): T? = runCatching(read)
    .onFailure { AppLog.w(TAG, "$what could not be read, so the report keeps it", it) }
    .getOrNull()

/**
 * The way to tell what went wrong: what the member writes, the part of the log
 * that goes into a new issue on the project's page, and the whole log to be
 * sent as a file instead.
 *
 * The log is shown as it will leave, scrubbed, so that the member sees what is
 * sent before sending it. Drawn over the settings it is opened from, like the
 * accuracy screen.
 */
@Composable
fun ReportScreen(
    known: Known,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    readLog: () -> String = AppLog::read,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var description by remember { mutableStateOf("") }
    // Read and scrubbed off the main thread: the log is up to a quarter of a megabyte.
    val log by produceState(initialValue = "") {
        value = withContext(Dispatchers.IO) { scrubLog(readLog(), known) }
    }
    val draft =
        remember(description, log) {
            buildIssue(description, BuildConfig.VERSION_NAME, Build.VERSION.SDK_INT, log)
        }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.report_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.accuracy_back))
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(R.string.report_what_went_wrong)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.report_what_is_sent),
                    style = MaterialTheme.typography.titleMedium,
                )

                // Opened at its end: the last lines are the ones about the trouble.
                val tailScroll = rememberScrollState()

                LaunchedEffect(draft.tail) { tailScroll.scrollTo(tailScroll.maxValue) }

                Text(
                    text = draft.tail,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(tailScroll),
                )

                Button(
                    onClick = { openIssue(context, draft.url) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.report_open_issue))
                }

                OutlinedButton(
                    onClick = { scope.launch { shareLog(context, log) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.report_share_log))
                }
            }
        }
    }
}

private fun openIssue(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        // No browser to open the page in; the log can still be shared as a file.
    }
}

/**
 * Writes the scrubbed log where only the provider hands it out from, and
 * offers it to whichever application the member picks.
 */
private suspend fun shareLog(context: Context, log: String) {
    val file =
        withContext(Dispatchers.IO) {
            File(context.cacheDir, "report").apply { mkdirs() }.let {
                File(it, "geoshare-log.txt").apply { writeText(log) }
            }
        }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.reports", file)
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
        context.startActivity(
            Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        // Nothing on the device takes a file.
    }
}
