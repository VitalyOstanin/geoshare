package io.github.vitalyostanin.geoshare.map

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.Source
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.grantsOf
import io.github.vitalyostanin.geoshare.service.permissionsOf
import io.github.vitalyostanin.geoshare.service.readBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.firstshare"
private const val FILE = "first_share"

/** The span the question offers first: long enough for a walk, short enough to lapse by itself. */
private val FIRST_SPAN = ShareSpan.Day

/**
 * Whether this device has already asked a member of a group whom to show
 * their place to, kept per group and account: the question is put once, and
 * the answer — or the refusal to give one — lives in the settings after it.
 */
class FirstShareAsked(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun asked(projectId: String, memberId: String): Boolean =
        store.readBoolean(key(projectId, memberId), false)

    fun markAsked(projectId: String, memberId: String) {
        store.edit().putBoolean(key(projectId, memberId), true).apply()
    }

    private fun key(projectId: String, memberId: String) = "$projectId/$memberId"
}

/**
 * Whether the member has given nobody their place yet, as the server has it.
 *
 * Asked of the server alone: a copy from the cache, or a refusal read as an
 * empty list, would put the question to somebody who has long answered it.
 * Null when the server could not be asked.
 */
private suspend fun nothingShared(memberId: String, connection: GroupConnection): Boolean? = try {
    val grant = grantsOf(connection).document(memberId).get(Source.SERVER).await()

    permissionsOf(grant).isEmpty()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    logFailure(TAG, "could not read whom the place is shown to", error)
    null
}

/**
 * Asks, once per group, whom to show this member's place to.
 *
 * Without it a member who has just joined is seen by nobody until they find
 * "Who sees where I am" in the settings, and the group waits for a marker
 * that never comes. Put only while the member has given nobody anything and
 * there is somebody to give it to; a group still empty asks when its first
 * other member appears.
 */
@Composable
fun FirstShareQuestion(
    connection: GroupConnection,
    memberId: String,
    /** Everyone with a profile, this member included; they are not offered themselves. */
    profiles: Map<String, MemberProfile>?,
    /** Gives the place to [readers] for [span], in one write. */
    onShare: (readers: List<String>, span: ShareSpan) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { FirstShareAsked(context) }
    var empty by remember(connection, memberId) { mutableStateOf(false) }

    LaunchedEffect(connection, memberId) {
        if (!store.asked(connection.projectId, memberId)) {
            empty = nothingShared(memberId, connection) == true
        }
    }

    val readers =
        profiles
            ?.values
            ?.filter { it.memberId != memberId }
            ?.sortedBy { it.displayName }
            .orEmpty()

    if (!empty || readers.isEmpty()) {
        return
    }

    fun done() {
        store.markAsked(connection.projectId, memberId)
        empty = false
    }

    FirstShareDialog(
        readers = readers,
        onShare = { chosen, span ->
            done()

            if (chosen.isNotEmpty()) {
                onShare(chosen, span)
            }
        },
        onLater = ::done,
    )
}

@Composable
private fun FirstShareDialog(
    readers: List<MemberProfile>,
    onShare: (List<String>, ShareSpan) -> Unit,
    onLater: () -> Unit,
) {
    // Everybody ticked to begin with: a group is mostly joined to be seen by it.
    var chosen by remember(readers) { mutableStateOf(readers.map { it.memberId }.toSet()) }
    var span by remember { mutableStateOf(FIRST_SPAN) }

    AlertDialog(
        // Not dismissed by a tap beside it: the answer is asked for once, and
        // a stray tap would spend it.
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.first_share_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(
                    modifier =
                    Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                ) {
                    for (reader in readers) {
                        val on = reader.memberId in chosen

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = on,
                                    role = Role.Checkbox,
                                    onValueChange = { wanted ->
                                        chosen =
                                            if (wanted) {
                                                chosen + reader.memberId
                                            } else {
                                                chosen - reader.memberId
                                            }
                                    },
                                ),
                        ) {
                            Checkbox(checked = on, onCheckedChange = null)
                            Text(
                                text = reader.displayName,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.first_share_span),
                        modifier = Modifier.weight(1f),
                    )

                    SpanChoice(span = span, onSpan = { span = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onShare(chosen.toList(), span) },
                enabled = chosen.isNotEmpty(),
            ) {
                Text(text = stringResource(R.string.first_share_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(text = stringResource(R.string.first_share_later))
            }
        },
    )
}

/**
 * The span as a list's chosen value, the way "Who sees where I am" draws it;
 * giving nothing is not one of the choices here.
 */
@Composable
private fun SpanChoice(span: ShareSpan, onSpan: (ShareSpan) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(spanLabel(span)),
                    style = MaterialTheme.typography.bodyMedium,
                )

                ListTriangle(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (offered in ShareSpan.entries.filter { it != ShareSpan.None }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(spanLabel(offered))) },
                    onClick = {
                        open = false
                        onSpan(offered)
                    },
                )
            }
        }
    }
}
