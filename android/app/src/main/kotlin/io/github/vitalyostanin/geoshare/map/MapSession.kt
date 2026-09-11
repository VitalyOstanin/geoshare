package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * The positions, collected only while a screen of ours is up.
 *
 * A subscription costs the other phones nothing, but it does hold a connection
 * open, and there is no one to show a moved marker to behind a dark screen.
 */
@Composable
fun rememberMemberPositions(): State<List<MemberPosition>> {
    val positions = remember { memberPositions() }

    return positions.collectAsStateWithLifecycle(initialValue = emptyList())
}

/**
 * Tells the other members that somebody is looking, while somebody is.
 *
 * Tied to the resumed state rather than to the composition: a map left behind
 * a lock screen is not somebody looking, and keeping the claim alive there
 * would hold every other phone in its fifteen-second mode for nothing.
 */
@Composable
fun KeepWatching(memberId: String) {
    val owner = LocalLifecycleOwner.current

    LaunchedEffect(memberId, owner) {
        val writer = WatchWriter(memberId)

        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                writer.keepLooking()
            } finally {
                // The claim is being withdrawn because this coroutine was
                // cancelled, so the withdrawal needs a scope that survives the
                // cancellation to reach the database at all.
                withContext(NonCancellable) { writer.stopLooking() }
            }
        }
    }
}
