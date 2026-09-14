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
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.service.MapOnScreen
import io.github.vitalyostanin.geoshare.service.groupRoster
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.watchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * The positions, collected only while a screen of ours is up.
 *
 * A subscription costs the other phones nothing, but it does hold a connection
 * open, and there is no one to show a moved marker to behind a dark screen.
 *
 * Driven by the grants, because they are what names the documents to read: a
 * member who picks up a second phone adds it to their grant, and the
 * subscriptions are rebuilt around the new list.
 */
@Composable
fun rememberMemberPositions(grants: Map<String, MemberGrant>): State<MembersInView> {
    val documents = remember(grants) { positionDocuments(grants) }
    val positions = remember(documents) { memberPositions(documents) }

    return positions.collectAsStateWithLifecycle(initialValue = MembersInView())
}

/**
 * What the members give out, collected while a screen is up.
 *
 * Follows the profiles, which is the one list of the household every member
 * may read: the grants themselves cannot be listed, so who to ask about has to
 * come from somewhere else.
 */
@Composable
fun rememberMemberGrants(profiles: Map<String, MemberProfile>?): State<Map<String, MemberGrant>> {
    val memberIds = remember(profiles) { profiles.orEmpty().keys.sorted() }
    val grants = remember(memberIds) { memberGrants(memberIds) }

    return grants.collectAsStateWithLifecycle(initialValue = emptyMap())
}

/**
 * Who else has the map open, collected while a screen of ours is up.
 *
 * The same claims the service reads to decide how hard to work; read here a
 * second time because the screen wants the members behind them rather than the
 * moments they run out. Own marker aside — a phone showing this screen is the
 * one looking — the list is what the strip under the map names.
 */
@Composable
fun rememberWatchers(memberId: String, deviceId: String): State<List<Watcher>> {
    val looking = remember(memberId, deviceId) { watchers(positionId(memberId, deviceId)) }

    return looking.collectAsStateWithLifecycle(initialValue = emptyList())
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
 * The roster, collected while a screen is up.
 *
 * Nothing arrives for somebody outside the group, and nothing arrives before
 * the group is made: the document is closed to everybody not on it, and a
 * document that does not exist has nothing to send. Both come out as no
 * roster at all, which is why the screen offers founding a group and entering
 * a code side by side — from here the two cannot be told apart.
 */
@Composable
fun rememberGroupRoster(): State<Roster?> {
    val roster = remember { groupRoster() }

    return roster.collectAsStateWithLifecycle(initialValue = null)
}

/**
 * The permissions this member has given, collected while a screen is up.
 *
 * Read back out of the database rather than held on this phone: they are set
 * on whichever phone the member has to hand, and the screen has to show what
 * the other one wrote.
 */
@Composable
fun rememberOwnSharedWith(memberId: String): State<Map<String, Instant>> {
    val permissions = remember(memberId) { ownSharedWith(memberId) }

    return permissions.collectAsStateWithLifecycle(initialValue = emptyMap())
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

            // Two documents, because the two are read by different people:
            // the name and the picture by the whole group, the phone by the
            // readers this member named. Remembered only when both were taken,
            // so a phone missing from the list is written again next time.
            val named =
                publishProfile(
                    memberId = memberId,
                    displayName = name,
                    pictureUrl = pictureUrl,
                )
            val listed = publishDevice(memberId = memberId, deviceId = deviceId)

            if (named && listed) {
                published.remember(memberId, name, pictureUrl, deviceId)
            }
        }
    }
}

/**
 * Tells everybody that somebody is looking, while somebody is — the other
 * members through the database, and this phone's own collection directly.
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
            // Said twice on purpose: once to the other members through the
            // database, and once to this phone's own collection, which never
            // hears the first because a phone ignores its own claim.
            MapOnScreen.opened()

            try {
                writer.keepLooking()
            } finally {
                MapOnScreen.closed()

                // The claim is being withdrawn because this coroutine was
                // cancelled, so the withdrawal needs a scope that survives the
                // cancellation to reach the database at all.
                withContext(NonCancellable) { writer.stopLooking() }
            }
        }
    }
}
