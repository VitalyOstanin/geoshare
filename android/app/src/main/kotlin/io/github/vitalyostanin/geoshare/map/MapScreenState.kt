package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Keeps the positions under an opened marker across a recreated screen, as the card is kept. */
private val PositionIdsSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it },
)

/**
 * What the map screen itself keeps hold of: which card is open, what the map
 * was asked to look at, and the dialogs raised from it. The screens it opens
 * over itself are [OpenPanels], and the follow mode is [FollowState].
 */
internal class MapScreenState(
    showAllState: MutableState<Boolean>,
    openedPositionIdState: MutableState<String?>,
    detailsOpenState: MutableState<Boolean>,
    openedGroupState: MutableState<List<String>>,
) {
    // Kept across a recreated screen, as the follow mode is: turning the
    // device or a change of the system theme would otherwise close the card
    // somebody was reading.
    var showAll by showAllState

    // Held by id rather than by value: the position keeps arriving, and a card
    // showing the one from when it was opened would quietly go stale.
    var openedPositionId by openedPositionIdState

    // Kept across cards: a member who opened the details wants them on the
    // next card too, and one who closed them opened the card for the summary.
    var detailsOpen by detailsOpenState

    // The positions under one marker, held by id for the same reason a single
    // one is: the list keeps arriving, and the card has to follow it.
    var openedGroup by openedGroupState

    // The name being asked for, held by what it names rather than by the card:
    // the card may be closed and the dialog still up.
    var naming by mutableStateOf<Pair<LabelKind, String>?>(null)
    var focus by mutableStateOf<MapFocus?>(null)
    var mapHelpOpen by mutableStateOf(false)
    var groupsOpen by mutableStateOf(false)
    var leaveAsk by mutableStateOf<LeaveAsk?>(null)

    // Where the buttons of the strip and the map came out across the screen:
    // a card stands over the button of the member it is about.
    val stripCentres = mutableStateMapOf<String, Float>()
    var mapLeft by mutableFloatStateOf(0f)

    // Where the row of buttons over the map ends, for following everybody
    // to keep the top pin below it.
    var topCover by mutableStateOf(0)

    /** Opens the card of one member, in place of whatever was open. */
    fun openCard(positionId: String) {
        openedPositionId = positionId
        openedGroup = emptyList()
    }

    /** Opens the list of everybody under one marker, in place of a single card. */
    fun openGroup(positionIds: List<String>) {
        openedPositionId = null
        openedGroup = positionIds
    }

    /** Closes the card or the list, whichever is open. */
    fun closeCard() {
        openedPositionId = null
        openedGroup = emptyList()
    }

    // A tap is a request to look at somebody, not a state: tapping the same
    // member twice has to bring the map back both times, and the counter is
    // what makes the second tap a change the map can see.
    fun lookAt(positionId: String) {
        focus = MapFocus(positionId = positionId, request = (focus?.request ?: 0) + 1)
        openCard(positionId)
    }
}

@Composable
internal fun rememberMapScreenState(): MapScreenState {
    val showAllState = rememberSaveable { mutableStateOf(false) }
    val openedPositionIdState = rememberSaveable { mutableStateOf<String?>(null) }
    val detailsOpenState = rememberSaveable { mutableStateOf(false) }
    val openedGroupState = rememberSaveable(stateSaver = PositionIdsSaver) {
        mutableStateOf(emptyList())
    }

    return remember {
        MapScreenState(showAllState, openedPositionIdState, detailsOpenState, openedGroupState)
    }
}
