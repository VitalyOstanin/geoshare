package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
 *
 * Driven by the profiles, because they are what names the documents to read:
 * a member who picks up a second phone adds it to their profile, and the
 * subscriptions are rebuilt around the new list.
 */
@Composable
fun rememberMemberPositions(profiles: Map<String, MemberProfile>?): State<List<MemberPosition>> {
    val documents = remember(profiles) { positionDocuments(profiles.orEmpty()) }
    val positions = remember(documents) { memberPositions(documents) }

    return positions.collectAsStateWithLifecycle(initialValue = emptyList())
}

/**
 * The profiles, collected alongside the positions.
 *
 * Separate from the positions rather than joined to them: a member who has not
 * written a position yet still has a name, and a position without a profile is
 * still a place to draw. Null until the first answer arrives, which is what
 * tells "nobody has a profile" apart from "the answer is still on its way".
 */
@Composable
fun rememberMemberProfiles(): State<Map<String, MemberProfile>?> {
    val profiles = remember { memberProfiles() }

    return profiles.collectAsStateWithLifecycle(initialValue = null)
}

/**
 * Tells the group who this member is, when the account says something this
 * phone has not had accepted yet.
 *
 * What was accepted is remembered on the phone rather than read back out of
 * the subscription: a write shows up there before the server has taken it and
 * disappears again if the server refuses, and a client that answered the
 * disappearance with another write would keep doing so for as long as the
 * screen was up.
 *
 * Tried again on every return to the screen, because the first attempt can be
 * refused for a reason that goes away without the application hearing about
 * it — a member added to the group a minute after they first signed in is
 * exactly that case, and without the retry they would stay nameless until the
 * next launch.
 */
@Composable
fun PublishProfile(memberId: String, displayName: String?, pictureUrl: String?, deviceId: String) {
    // An account with no name of its own has nothing to say about itself that
    // the id does not already say, so nothing is written.
    val name = displayName?.takeIf { it.isNotBlank() } ?: return
    val context = LocalContext.current
    val published = remember(context) { PublishedProfile(context) }
    val owner = LocalLifecycleOwner.current

    LaunchedEffect(memberId, name, pictureUrl, deviceId, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (published.matches(memberId, name, pictureUrl, deviceId)) {
                return@repeatOnLifecycle
            }

            val accepted =
                publishProfile(
                    memberId = memberId,
                    displayName = name,
                    pictureUrl = pictureUrl,
                    deviceId = deviceId,
                )

            if (accepted) {
                published.remember(memberId, name, pictureUrl, deviceId)
            }
        }
    }
}

/**
 * Tells the other members that somebody is looking, while somebody is.
 *
 * Tied to the resumed state rather than to the composition: a map left behind
 * a lock screen is not somebody looking, and keeping the claim alive there
 * would hold every other phone in its fifteen-second mode for nothing.
 */
@Composable
fun KeepWatching(memberId: String, deviceId: String) {
    val owner = LocalLifecycleOwner.current

    LaunchedEffect(memberId, deviceId, owner) {
        val writer = WatchWriter(memberId, deviceId)

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
