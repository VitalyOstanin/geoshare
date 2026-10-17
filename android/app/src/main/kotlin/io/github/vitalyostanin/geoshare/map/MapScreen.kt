package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.Watcher

/** Above this a surface counts as pale, below it as dark. */
internal const val HALF_LIT = 0.5f

/** As much of the screen as the full list may take before it starts scrolling instead. */
internal val LIST_MAX_HEIGHT = 220.dp

/**
 * The screen a signed-in member sees: everyone on a map, and a row of buttons
 * to move the map onto one of them.
 *
 * The buttons are the point of the screen. Answering "where is X" should be a
 * tap rather than a search across a map of everybody, so two members sit in
 * the row itself and the rest are one tap further in.
 */
@Composable
fun MapScreen(
    positions: List<MemberPosition>,
    /** How many members' positions this device is being refused, if any. */
    outOfReach: Int,
    /** What each member calls themselves, by member id; absent until they sign in. */
    profiles: Map<String, MemberProfile>,
    /** Who else has the map open, as they themselves claim. */
    watchers: List<Watcher>,
    ownMemberId: String,
    /**
     * This device's own `{uid}.{deviceId}`: the administrator's panel rings
     * every device but this one.
     */
    ownPhoneId: String?,
    ownDisplayName: String?,
    ownPictureUrl: String?,
    /** The group's searches, by the device each is looking for. */
    searches: Map<String, Search>,
    /** Everything the settings screen shows, which the map only carries through. */
    settings: SettingsState,
    /** What the settings screen can ask for, likewise. */
    actions: SettingsActions,
    /** What this member calls the devices and the people, for themselves. */
    labels: Labels,
    /** Gives a device or a person a name for this member alone, or takes it away. */
    onLabel: (LabelKind, String, String?) -> Unit,
    /** The groups this device is in, for the chip that switches between them. */
    groups: GroupSwitch,
    modifier: Modifier = Modifier,
) {
    val now by currentTime()
    // The devices the group is looking for right now, worked out against the
    // same clock the ages tick by: nothing arrives from the database at the
    // moment a search runs out, so this is read afresh whenever [now] moves.
    val soughtPhones = remember(searches, now) { standingSearches(searches, now).keys }
    val panels = rememberOpenPanels()
    UpdateEntry(panels)
    val ownName = stringResource(R.string.map_member_you)
    val scene = MapScene(
        state = rememberMapScreenState(),
        follow = rememberFollowState(),
        panels = panels,
        positions = positions,
        profiles = profiles,
        searches = searches,
        now = now,
        ownName = ownName,
        // Loaded here rather than inside the map: the map draws bitmaps, and
        // fetching a picture is the screen's business, not the drawing's.
        faces = rememberMemberFaces(profiles),
        soughtPhones = soughtPhones,
        lookingNames = lookingNames(watchers, now, profiles, ownMemberId, ownName),
        ownMemberId = ownMemberId,
        ownPhoneId = ownPhoneId,
        ownDisplayName = ownDisplayName,
        ownPictureUrl = ownPictureUrl,
        settings = settings,
        actions = actions,
        groups = groups,
        outOfReach = outOfReach,
    )

    MapBackHandlers(scene)
    MapColumn(scene, modifier)
    MapOverlayScreens(scene, labels, onLabel)
}
