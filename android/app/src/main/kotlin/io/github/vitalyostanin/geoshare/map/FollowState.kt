package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * How long [FollowMode.ONE] waits, once picked with nobody yet chosen to sit
 * on, before giving up and letting the strip stop asking.
 *
 * Long enough that turning to tap a card is not a race against the menu
 * closing on its own; short enough that a member who opened the menu by
 * mistake is not left with a strip ringed in a colour they never meant to
 * see.
 */
private const val AWAITING_TARGET_MS = 15_000L

/**
 * Who the follow button's [FollowMode.ONE] centres on.
 *
 * The name is captured at the moment the member's card is opened rather than
 * looked up again from [MemberPosition] each time the button is drawn: once
 * the member's position drops out of the positions this device is being shown,
 * there is nowhere left to look the name up from, and the button still has to
 * say whose card it was.
 */
internal data class FollowTarget(val positionId: String, val name: String)

/** Keeps [FollowTarget] across a recreated screen, the way the mode is kept. */
private val FollowTargetSaver = listSaver<FollowTarget?, String>(
    save = { target -> target?.let { listOf(it.positionId, it.name) } ?: emptyList() },
    restore = { saved -> if (saved.isEmpty()) null else FollowTarget(saved[0], saved[1]) },
)

/** What the camera follows, and the button that says so. */
internal class FollowState(
    modeState: MutableState<FollowMode>,
    targetState: MutableState<FollowTarget?>,
) {
    var mode by modeState
    var target by targetState

    /** Whether a hand is holding the following still; the button says so. */
    var paused by mutableStateOf(false)

    /**
     * Bumped on every real change of the mode — from the menu, or from a
     * target finally being chosen for [awaitingTarget] — so the button's
     * label can unroll again each time rather than only on the first change:
     * a key that stayed the same (say, ALL picked while ALL already ran)
     * would leave [LaunchedEffect] with nothing to restart.
     */
    var changes by mutableIntStateOf(0)

    /**
     * Set once ONE is picked with nobody yet chosen to sit on, and cleared the
     * moment a target is noted — by any of the three ways a card opens, by
     * picking a different mode from the menu, by the back gesture, or on its
     * own once nobody has answered it for a while.
     */
    var awaitingTarget by mutableStateOf(false)

    /**
     * Notes whoever a card was just opened about, from whichever of the three
     * ways it happened to open: a tap on the strip, a tap on the member's own
     * marker, or a pick out of a group's list. All three end up here so that
     * ONE always has a name for whoever the last card was about.
     *
     * Also where ONE's own wait ends: the same three ways of opening a card
     * are the three ways the strip's ring is answered, and a name arriving
     * now is exactly the one [awaitingTarget] was left waiting for.
     */
    fun note(positionId: String, name: String) {
        target = FollowTarget(positionId = positionId, name = name)

        if (awaitingTarget) {
            mode = FollowMode.ONE
            awaitingTarget = false
            changes++
        }
    }

    /**
     * A pick from the button's menu. ONE picked with nobody to sit on does not
     * become the mode yet: the strip is asked instead, and it is [note], once
     * somebody answers, that turns the wait into the mode itself.
     */
    fun pick(picked: FollowMode, hasTarget: Boolean) {
        if (picked == FollowMode.ONE && !hasTarget) {
            awaitingTarget = true
        } else {
            mode = picked
            awaitingTarget = false
            changes++
        }
    }
}

/**
 * The follow state of the map screen. The mode and the target are saveable: a
 * change of theme or of the screen's orientation recreates the screen, and
 * the camera is to go on following.
 */
@Composable
internal fun rememberFollowState(): FollowState {
    val mode = rememberSaveable { mutableStateOf(FollowMode.NONE) }
    val target =
        rememberSaveable(stateSaver = FollowTargetSaver) { mutableStateOf<FollowTarget?>(null) }
    val follow = remember(mode, target) { FollowState(mode, target) }

    // Nobody answering the strip for that long is a menu opened by mistake
    // rather than a member still deciding, and the ring left running on the
    // strip forever would read as broken rather than as waiting.
    LaunchedEffect(follow.awaitingTarget) {
        if (follow.awaitingTarget) {
            delay(AWAITING_TARGET_MS)
            follow.awaitingTarget = false
        }
    }

    return follow
}
