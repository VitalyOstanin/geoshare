package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Subscriptions the rules turned away, each waiting to be made again.
 *
 * A refused client hears nothing of a permission handed back, so each one is
 * made again on a timer; and all of them at once when a member says they have
 * changed whom they share with, so that a new reader does not sit the timer
 * out before the marker appears.
 */
internal class Refusals(
    private val scope: CoroutineScope,
    private val after: Duration = RETRY_AFTER,
) {
    private val waiting = mutableMapOf<String, Pair<Job, () -> Unit>>()

    fun retryLater(key: String, again: () -> Unit) {
        waiting.remove(key)?.first?.cancel()

        val timer =
            scope.launch {
                delay(after.toMillis())
                waiting.remove(key)
                again()
            }

        waiting[key] = timer to again
    }

    fun retryNow() {
        val due = waiting.values.toList()

        waiting.clear()

        for ((timer, again) in due) {
            timer.cancel()
            again()
        }
    }
}

/** The moment some member last touched their profile; null while nobody has. */
fun lastProfileChange(profiles: Map<String, MemberProfile>?): Instant? =
    profiles?.values?.mapNotNull { it.updatedAt }?.maxOrNull()

/**
 * Says each time a member touches their profile after the screen opened.
 *
 * Granting a place touches the granter's profile, which every member follows
 * anyway: the one signal a reader still turned away by the grant can hear.
 */
@Composable
internal fun rememberProfileChanges(profiles: Map<String, MemberProfile>?): Flow<Unit> {
    val latest = rememberUpdatedState(lastProfileChange(profiles))

    return remember { snapshotFlow { latest.value }.filterNotNull().drop(1).map { } }
}
