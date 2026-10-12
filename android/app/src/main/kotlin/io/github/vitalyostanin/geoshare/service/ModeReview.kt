package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Moving
import io.github.vitalyostanin.geoshare.collection.watchedInAnyGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import java.time.Duration
import java.time.Instant

/**
 * How often the mode is reconsidered while the service runs.
 *
 * A viewer who leaves quietly — a closed lid, a flat battery — writes
 * nothing more, so the subscription brings no change and the claim
 * simply ages; the device learns it has run out only on a review. Once
 * a minute, rather than once a claim, keeps the fifteen-second mode
 * from outliving the last watcher by as long again as the claim
 * itself. A review reads nothing: it only asks the same question of
 * the clock.
 */
private val MODE_REVIEW: Duration = Duration.ofMinutes(1)

/**
 * How long a group's watching is kept listened to with nothing
 * reading it: the moment between the list of groups changing and the
 * new list being followed, with room to spare.
 */
private val WATCHING_OUTLIVES_A_RELINK: Duration = Duration.ofSeconds(5)

/**
 * Works out which mode the service collects in, from the viewers in every
 * linked group, the device's movement, the geofence and the device's own map.
 *
 * Kept apart from the service so that the timer and the subscriptions behind a
 * mode live in one place: the service only says what a mode is made of, in
 * [decide], and collects in whatever comes out.
 */
internal class ModeReview(
    private val context: Context,
    /** Where each group's watching is shared, so that it lives as long as the service. */
    private val scope: CoroutineScope,
    private val links: StateFlow<List<GroupLink>>,
    private val fenceCentre: Flow<Fix?>,
    /** The mode for the viewers' claims, the movement, the geofence and the device's own map. */
    private val decide: (
        untils: List<Instant>,
        moving: Moving,
        standing: Boolean,
        ownMap: Boolean,
    ) -> CollectionMode,
) {
    /**
     * The mode as it stands, and again whenever it changes.
     *
     * Two things move it: the viewers, which arrive through the subscription
     * within about a second of somebody opening a map, and the clock and the
     * battery, which are looked at on a timer. Repeats are dropped, so a review
     * that finds the same mode does not interrupt the collection.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun modes(): Flow<CollectionMode> {
        val reviews =
            flow {
                while (true) {
                    emit(Unit)
                    delay(MODE_REVIEW.toMillis())
                }
            }

        // Every group the device is linked to, each saying when its watching
        // runs out; a group joined or left while the service runs is followed
        // from then on. A group already followed keeps its answer across that:
        // listened to afresh it would begin by saying nobody looks, and every
        // group joined would drop the device out of its mode for an instant —
        // a cancelled request, a new one and a status written for nothing.
        val followed = HashMap<GroupLink, Flow<List<Instant>>>()
        val looking =
            links.flatMapLatest { linked ->
                followed.keys.retainAll(linked.toSet())

                if (linked.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        linked.map { link ->
                            followed.getOrPut(link) {
                                link.watching().shareIn(
                                    scope,
                                    SharingStarted.WhileSubscribed(
                                        WATCHING_OUTLIVES_A_RELINK.toMillis(),
                                    ),
                                    replay = 1,
                                )
                            }
                        },
                    ) { perGroup -> watchedInAnyGroup(perGroup.toList()) }
                }
            }

        return combine(
            looking,
            movement(context),
            standingStill(context, fenceCentre),
            MapOnScreen.state,
            reviews,
        ) { untils, moving, standing, ownMap, _ ->
            decide(untils, moving, standing, ownMap)
        }.distinctUntilChanged()
    }
}
