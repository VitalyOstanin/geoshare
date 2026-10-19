package io.github.vitalyostanin.geoshare.map

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.BuildConfig
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.download.DownloadSetting
import io.github.vitalyostanin.geoshare.download.NetworkKind
import io.github.vitalyostanin.geoshare.download.asksBeforeUpdate
import io.github.vitalyostanin.geoshare.download.networkKind
import io.github.vitalyostanin.geoshare.update.ApkDownload
import io.github.vitalyostanin.geoshare.update.LatestRelease
import io.github.vitalyostanin.geoshare.update.ReleaseNotes
import io.github.vitalyostanin.geoshare.update.UpdateBlockState
import io.github.vitalyostanin.geoshare.update.UpdateBoard
import io.github.vitalyostanin.geoshare.update.UpdateNote
import io.github.vitalyostanin.geoshare.update.UpdateStore
import io.github.vitalyostanin.geoshare.update.afterCancel
import io.github.vitalyostanin.geoshare.update.afterFailedDownload
import io.github.vitalyostanin.geoshare.update.afterInstallStart
import io.github.vitalyostanin.geoshare.update.afterManualCheck
import io.github.vitalyostanin.geoshare.update.checkForUpdateNow
import io.github.vitalyostanin.geoshare.update.downloadApk
import io.github.vitalyostanin.geoshare.update.downloadFraction
import io.github.vitalyostanin.geoshare.update.initialUpdateState
import io.github.vitalyostanin.geoshare.update.megabytes
import io.github.vitalyostanin.geoshare.update.startDownload
import io.github.vitalyostanin.geoshare.update.startInstall
import io.github.vitalyostanin.geoshare.update.updateDirectory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The update block of the settings: the switch for the daily check, then
 * whatever [UpdateBlockState] says. The state lives here rather than in
 * the screen's caller because nothing else needs it; the dot on the account
 * button reads [UpdateBoard] instead.
 */
@Composable
internal fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { UpdateStore(context.applicationContext) }
    var auto by remember { mutableStateOf(store.autoCheck) }
    var state by remember {
        mutableStateOf(
            initialUpdateState(store.found.takeIf { store.autoCheck }, BuildConfig.VERSION_NAME),
        )
    }
    var job by remember { mutableStateOf<Job?>(null) }
    var shownNotes by remember { mutableStateOf<Pair<LatestRelease, ReleaseNotes>?>(null) }
    var asked by remember { mutableStateOf<Pair<LatestRelease, NetworkKind>?>(null) }
    val language = appLocale().language
    val begin = { release: LatestRelease ->
        state = startDownload(release)
        job = scope.launch { state = download(context, release) { state = it } }
    }

    ShownNotes(shownNotes, context, onDismiss = { shownNotes = null })
    AskedNetwork(
        asked,
        onGo = { release ->
            asked = null
            begin(release)
        },
        onDismiss = { asked = null },
    )

    AutoCheckSwitch(checked = auto) {
        auto = it
        store.autoCheck = it
        UpdateBoard.refresh(store, BuildConfig.VERSION_NAME)
        // The block follows the switch at once, as the dot does; a
        // download already under way is left to finish or be cancelled.
        if (state is UpdateBlockState.Idle || state is UpdateBlockState.Available) {
            state = initialUpdateState(store.found.takeIf { _ -> it }, BuildConfig.VERSION_NAME)
        }
    }

    UpdateBody(
        state = state,
        onCheck = {
            state = UpdateBlockState.Checking
            scope.launch { state = afterManualCheck(checkForUpdateNow(context)) }
        },
        onWhatsNew = { release ->
            // A release whose text yields no sections still has its page,
            // which is what the button did before the dialog.
            val notes = ReleaseNotes.parse(release.notes, language)

            if (notes == null) openPage(context, release.pageUrl) else shownNotes = release to notes
        },
        onUpdate = { release ->
            val on = networkKind(context)

            if (asksBeforeUpdate(on, DownloadSetting(context).allowed)) {
                asked = release to on
            } else {
                begin(release)
            }
        },
        onCancel = { release ->
            job?.cancel()
            state = afterCancel(release)
        },
    )
}

/** The switch for the daily check, with what it does under its name. */
@Composable
private fun AutoCheckSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SwitchRow(checked = checked, onCheckedChange = onCheckedChange, verticalPadding = 8.dp) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.update_auto))
            Text(
                text = stringResource(R.string.update_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The notes dialog of the release the member asked about, if one is open. */
@Composable
private fun ShownNotes(
    shown: Pair<LatestRelease, ReleaseNotes>?,
    context: Context,
    onDismiss: () -> Unit,
) {
    if (shown == null) return

    ReleaseNotesDialog(
        version = shown.first.version,
        notes = shown.second,
        onOpenPage = { openPage(context, shown.first.pageUrl) },
        onDismiss = onDismiss,
    )
}

/**
 * The question before an update comes down over a network switched off
 * under Downloads (ADR-0039), if one is open: the size and which network,
 * then "Download" or "Not now".
 */
@Composable
private fun AskedNetwork(
    asked: Pair<LatestRelease, NetworkKind>?,
    onGo: (LatestRelease) -> Unit,
    onDismiss: () -> Unit,
) {
    if (asked == null) return

    val (release, on) = asked
    val text = when (on) {
        NetworkKind.WiFi -> R.string.update_ask_wifi
        NetworkKind.Roaming -> R.string.update_ask_roaming
        else -> R.string.update_ask_mobile
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_ask_title)) },
        text = { Text(text = stringResource(text, megabytes(release.apk.size))) },
        confirmButton = {
            TextButton(onClick = { onGo(release) }) {
                Text(text = stringResource(R.string.downloads_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.downloads_not_now))
            }
        },
    )
}

/**
 * Downloads [release] and hands the file to the installer, reporting
 * progress through [onState]; the returned state is the one to rest in.
 * Cancelling the calling coroutine stops the download and deletes the part.
 * [onState] runs on the calling coroutine's thread and not after a cancel,
 * so "Cancel" may set its own state right after cancelling the job.
 */
private suspend fun download(
    context: Context,
    release: LatestRelease,
    onState: (UpdateBlockState) -> Unit,
): UpdateBlockState {
    val result = downloadApk(release, updateDirectory(context.cacheDir)) { received, total ->
        onState(UpdateBlockState.Downloading(release, received, total))
    }

    return when (result) {
        is ApkDownload.Done -> afterInstallStart(release, startInstall(context, result.file))
        else -> afterFailedDownload(release, result)
    }
}

@Composable
private fun UpdateBody(
    state: UpdateBlockState,
    onCheck: () -> Unit,
    onWhatsNew: (LatestRelease) -> Unit,
    onUpdate: (LatestRelease) -> Unit,
    onCancel: (LatestRelease) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is UpdateBlockState.Idle -> {
                TextButton(onClick = onCheck) {
                    Text(text = stringResource(R.string.update_check))
                }
                state.note?.let { NoteLine(it) }
            }

            UpdateBlockState.Checking -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            is UpdateBlockState.Available -> {
                AvailableRows(state.release, onWhatsNew, onUpdate)
                state.note?.let { NoteLine(it) }
            }

            is UpdateBlockState.Downloading -> DownloadingRows(state, onCancel)
        }
    }
}

@Composable
private fun AvailableRows(
    release: LatestRelease,
    onWhatsNew: (LatestRelease) -> Unit,
    onUpdate: (LatestRelease) -> Unit,
) {
    Text(
        text = stringResource(R.string.update_available, release.version),
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        text = releaseDetails(release),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onWhatsNew(release) }) {
            Text(text = stringResource(R.string.update_whats_new))
        }

        Button(onClick = { onUpdate(release) }) {
            Text(text = stringResource(R.string.update_install))
        }
    }
}

@Composable
private fun DownloadingRows(
    state: UpdateBlockState.Downloading,
    onCancel: (LatestRelease) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.update_downloading, state.release.version),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.update_progress,
                megabytes(state.received),
                megabytes(state.total),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    LinearProgressIndicator(
        progress = { downloadFraction(state.received, state.total) },
        modifier = Modifier.fillMaxWidth(),
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { onCancel(state.release) }) {
            Text(text = stringResource(R.string.update_cancel))
        }
    }
}

@Composable
private fun NoteLine(note: UpdateNote) {
    val text = when (note) {
        UpdateNote.Latest -> R.string.update_latest
        UpdateNote.CheckFailed -> R.string.update_check_failed
        UpdateNote.Damaged -> R.string.update_damaged
        UpdateNote.Failed -> R.string.update_failed
    }

    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = if (note == UpdateNote.Latest) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
    )
}

/** "12 MB · released 20 October"; the date is left out when GitHub gave none. */
@Composable
private fun releaseDetails(release: LatestRelease): String {
    val size = megabytes(release.apk.size)
    val published = release.publishedAt ?: return stringResource(R.string.update_size_only, size)
    val locale = appLocale()
    val day = DateTimeFormatter.ofPattern(dayPattern(locale, monthInFull = true), locale)
        .format(published.atZone(ZoneId.systemDefault()))

    return stringResource(R.string.update_details, size, day)
}

/** The release page in the browser; a device with none just does nothing. */
private fun openPage(context: Context, url: String) {
    if (!url.startsWith("https://")) return

    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (missing: ActivityNotFoundException) {
        // No browser: nothing to show, and nothing worth an error line.
    }
}
