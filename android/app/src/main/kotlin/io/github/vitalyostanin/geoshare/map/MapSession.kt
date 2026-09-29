package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.service.CurrentGroup
import io.github.vitalyostanin.geoshare.service.MapOnScreen
import io.github.vitalyostanin.geoshare.service.databaseLink
import io.github.vitalyostanin.geoshare.service.groupRoster
import io.github.vitalyostanin.geoshare.service.groupSearches
import io.github.vitalyostanin.geoshare.service.namedFalsePlaces
import io.github.vitalyostanin.geoshare.service.ownSharedWith
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.watchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
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
 * subscriptions are rebuilt around the new list. Also driven by the standing
 * searches, whose phones open without a grant at all: see [soughtPhoneIds].
 */
@Composable
fun rememberMemberPositions(
    grants: Map<String, MemberGrant>,
    /** This phone's own position document, followed whatever the grants say. */
    own: String,
    /** The group's searches, whose standing phones this phone may read regardless of a grant. */
    searches: Map<String, Search> = emptyMap(),
): State<MembersInView> {
    val writing = groupNow()
    val sought = soughtPhoneIds(searches)
    val documents = remember(grants, own, sought) { positionDocuments(grants, own, sought) }
    val positions = remember(writing, documents) { memberPositions(documents) }
    val written = positions.collectAsStateWithLifecycle(initialValue = MembersInView())
    val falsePlaces = remember(writing) { namedFalsePlaces() }
    val places = falsePlaces.collectAsStateWithLifecycle(initialValue = emptyList())

    // What the phones said about themselves since their positions were
    // written is laid over them here, so that every screen reading a position
    // reads the charge, the cable and the moment of contact as they are now;
    // and so is whether the position lies where the lie puts phones.
    return remember(written, places, grants) {
        derivedStateOf {
            written.value.let { seen ->
                val current = withPhones(seen.positions, grants)

                seen.copy(positions = withFalsePlaces(current, places.value))
            }
        }
    }
}

/**
 * Which group this phone is writing into, as something a subscription can be
 * rebuilt on.
 *
 * Every path below is built around the name, and founding a group or joining
 * one changes where this phone writes while the screen is open. Without this
 * the subscriptions would go on asking a question the rules refuse — either
 * about the group the phone has left, or about the one it had not yet been let
 * into — and a refused subscription is only taken out again after a wait that
 * doubles, so somebody who had just joined would watch an empty screen for
 * minutes.
 */
@Composable
private fun groupNow(): CurrentGroup.Writing =
    CurrentGroup.writings.collectAsStateWithLifecycle().value

/**
 * The group's searches, collected while a screen is up.
 *
 * Followed the same way the roster is: everybody in the group reads the
 * collection, and a refusal is read as no searches rather than as a reason to
 * stop asking.
 */
@Composable
fun rememberSearches(): State<Map<String, Search>> {
    val writing = groupNow()
    val searches = remember(writing) { groupSearches() }

    return searches.collectAsStateWithLifecycle(initialValue = emptyMap())
}

/**
 * How often [rememberMemberPositions] checks the searches against the clock,
 * to notice one running out without anything else changing.
 *
 * Half a minute, the same age the map's own clock ticks at: a phone dropping
 * out of a standing search is worth being current about, and nothing this
 * short costs a subscription that only reads a moment already held in memory.
 */
private const val SOUGHT_CHECK_MILLIS = 30_000L

/**
 * The phones the group is looking for right now, freshly worked out against
 * the clock rather than only when [searches] itself changes.
 *
 * A search starting or ending changes which positions this phone may read
 * without a grant: see [positionDocuments]. This is what
 * [rememberMemberPositions] rebuilds its subscriptions on, rather than on
 * every renewal — a search kept open for hours would otherwise tear a
 * listener down and stand it back up for nothing each time it is renewed.
 */
@Composable
private fun soughtPhoneIds(searches: Map<String, Search>): List<String> {
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(SOUGHT_CHECK_MILLIS)
            now = Instant.now()
        }
    }

    return remember(searches, now) { standingSearches(searches, now).keys.sorted() }
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
    val writing = groupNow()
    val memberIds = remember(profiles) { profiles.orEmpty().keys.sorted() }
    val grants = remember(writing, memberIds) { memberGrants(memberIds) }

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
    val writing = groupNow()
    val looking =
        remember(writing, memberId, deviceId) { watchers(positionId(memberId, deviceId)) }

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
    val writing = groupNow()
    val profiles = remember(writing) { memberProfiles() }

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
    val writing = groupNow()
    val roster = remember(writing) { groupRoster() }

    return roster.collectAsStateWithLifecycle(initialValue = null)
}

/**
 * Whether the database answers, collected while a screen is up; answering
 * until the group document's snapshots say otherwise (see [DatabaseLink]).
 */
@Composable
fun rememberDatabaseLink(): State<LinkState> {
    val writing = groupNow()
    val link = remember(writing) { databaseLink() }

    return link.collectAsStateWithLifecycle(initialValue = LinkState())
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
    val writing = groupNow()
    val permissions = remember(writing, memberId) { ownSharedWith(memberId) }

    return permissions.collectAsStateWithLifecycle(initialValue = emptyMap())
}

/**
 * What this member calls the others, collected while a screen is up.
 *
 * Starts from the copy kept on the phone, so the names are there from the
 * first frame rather than from the first answer of the database.
 */
@Composable
fun rememberOwnLabels(memberId: String): State<Labels> {
    val writing = groupNow()
    val context = LocalContext.current
    val cache = remember(context) { LabelCache(context) }
    val labels = remember(writing, memberId) { ownLabels(memberId, cache) }

    val kept = remember(memberId) { cache.read(memberId) }

    return labels.collectAsStateWithLifecycle(initialValue = kept)
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
    // A profile belongs to a group, so moving to another one is as good as
    // never having published: the new group has been told nothing. Written
    // again on being let into a group as well, because the attempt made before
    // this phone was on the roster was refused.
    val writing = groupNow()
    val telling =
        ProfileTold(
            group = writing.name,
            memberId = memberId,
            displayName = name,
            pictureUrl = pictureUrl,
            deviceId = deviceId,
        )

    LaunchedEffect(writing, telling, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (published.told() == telling) {
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
                published.remember(telling)
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
