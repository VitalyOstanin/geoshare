// Following: keeping the map on one member, on everybody, or on nobody, and
// the pause a hand on the map buys against a follow mode pulling the view
// back out from under it.
//
// Split out of map.js because the two halves read each other's state rather
// than share code: a marker never asks who is being followed, and a follow
// mode never builds a pin. What crosses between the two files is the view
// object and a handful of calls, not data of its own.

import { closeEnoughForTheRing, selectedPosition, START_ZOOM } from './map.js';
import { accuracyRing } from './width.js';
import {
    FOLLOW_ALL,
    FOLLOW_ONE,
    PAUSE_AFTER_A_GESTURE_MS,
    boundsAreClose,
    boundsOf,
    isPaused,
    targetPosition,
} from './following.js';

/**
 * How far the fitted view is padded from the window's edge, in screen pixels,
 * at the least.
 *
 * Enough that a marker at the corner of the rectangle is not touching the
 * account button or the zoom controls that sit over the same corners.
 */
const FOLLOW_ALL_PADDING = 60;

/** The gap left between a fitted marker's outermost edge and the window's. */
const FOLLOW_ALL_MARKER_GAP = 16;

/**
 * How close "following everybody" is ever allowed to come.
 *
 * A group of one has a rectangle with no size at all, and fitting the map to
 * it without a limit would zoom in on that single roof the way [CLOSEST_ZOOM]
 * stops the circle of doubt from doing.
 */
const FOLLOW_ALL_MAX_ZOOM = 17;

/**
 * How many pixels across a ring may come out before a card opened on the map
 * brings it closer.
 *
 * A map fitted round everybody can span a region, and there a courtyard of
 * doubt is smaller than a pixel: the card asks where exactly the member is and
 * the map has no way to say. Twenty is about the head of the pin, the size at
 * which a ring stops being hidden behind the marker it belongs to.
 */
const RING_TOO_SMALL_PIXELS = 20;

/**
 * Moves the map to match the follow mode, once a fresh set of positions has
 * been drawn.
 *
 * Nothing here runs on a timer of its own: a drawing is the only moment new
 * positions are in hand to follow, so this is called from nowhere else.
 */
export function applyFollowMode(view) {
    if (followPaused(view)) {
        return;
    }

    const positions = view.last?.positions ?? [];

    if (view.mode === FOLLOW_ONE) {
        const position = targetPosition(positions, view.targetPositionId);

        // Gone quiet rather than never chosen: the map holds still on the
        // last place it knew rather than snapping back to the world.
        if (position) {
            view.map.easeTo({ center: [position.longitude, position.latitude] });
        }

        return;
    }

    if (view.mode !== FOLLOW_ALL) {
        return;
    }

    const bounds = boundsOf(positions);

    if (!bounds) {
        return;
    }

    if (view.followedBounds && boundsAreClose(bounds, view.followedBounds)) {
        return;
    }

    view.followedBounds = bounds;
    view.map.fitBounds(
        [[bounds.west, bounds.south], [bounds.east, bounds.north]],
        { padding: followAllPadding(view), maxZoom: FOLLOW_ALL_MAX_ZOOM },
    );
}

/**
 * The padding that keeps every marker whole inside the fitted view.
 *
 * The rectangle is fitted by the points of the pins, and a pin hangs above its
 * point with the name over it, and reaches half its width to either side. A
 * padding the same on all four sides left the member at the top with the
 * head of their pin past the edge of the window. Measured off the markers
 * themselves rather than written down: a pin for several members is wider
 * than one for a single member, and a long name wider still.
 */
function followAllPadding(view) {
    let tallest = 0;
    let widest = 0;

    for (const entry of view.markers.values()) {
        const element = entry.marker.getElement();

        tallest = Math.max(tallest, element.offsetHeight);
        widest = Math.max(widest, element.offsetWidth);
    }

    const side = Math.max(FOLLOW_ALL_PADDING, widest / 2 + FOLLOW_ALL_MARKER_GAP);

    return {
        top: Math.max(FOLLOW_ALL_PADDING, tallest + FOLLOW_ALL_MARKER_GAP),
        bottom: FOLLOW_ALL_PADDING,
        left: side,
        right: side,
    };
}

/**
 * Remembers who a card just opened for, for the "one" follow mode to centre on.
 *
 * A viewer who asked to follow a member before there was one is answered
 * here: every way a card opens comes through this function, so the card that
 * opens next is the choice they were asked to make.
 */
export function setFollowTarget(view, positionId) {
    view.targetPositionId = positionId;

    if (view.awaitingTarget) {
        view.awaitingTarget = false;
        startFollowing(view, FOLLOW_ONE);
    }
}

/** Switches to `mode` as a press asks for, which is right away. */
function startFollowing(view, mode) {
    view.mode = mode;

    // Choosing a mode is a viewer asking for the map to move now. The pause
    // exists to keep a followed member from yanking the view out from under
    // a hand, which is the opposite of what a choice means; and the
    // rectangle last fitted describes a view the viewer may since have left.
    view.pausedAt = null;
    view.followedBounds = null;
    clearTimeout(view.pauseEnds);
}

/**
 * Holds the follow mode off for a while, the way a hand on the map does.
 *
 * Also what a card opened on the map does when it has to bring a ring into
 * sight: the viewer asked about one member, and a mode that fitted the view
 * round everybody again a moment later would take the answer away.
 */
export function pauseFollowing(view) {
    view.pausedAt = Date.now();
    // The hand has taken the map somewhere the fitted rectangle no longer
    // describes. Forgetting the rectangle is what lets "everybody" fit the view
    // again once the pause wears off: otherwise a household that has not moved
    // hands back the same rectangle, it is found close to the one already
    // fitted, and the map stays where the hand left it with the mode still on.
    view.followedBounds = null;
    // Nothing else is bound to move the map when the pause wears off: the next
    // batch of positions may be half an hour away for a member standing still,
    // and a map left where the hand put it with the mode on reads as broken. So
    // the end of the pause is an event of its own, and the button is drawn
    // again both ways.
    clearTimeout(view.pauseEnds);
    view.pauseEnds = setTimeout(() => {
        applyFollowMode(view);
        view.onCardChange();
    }, PAUSE_AFTER_A_GESTURE_MS);
    view.onCardChange();
}

/**
 * Takes the map close enough to see the ring of the card just opened on it,
 * where the ring would otherwise be lost under its pin.
 *
 * The strip always flies to the member, but a marker is tapped on the map the
 * viewer already has, and a map already close stays where it is. One fitted
 * round a family spread over a region is not a choice of zoom to respect: it
 * is a mode, and it pauses the way a hand pauses it.
 *
 * Measured on the screen rather than worked out from the zoom, because what
 * matters is the pixels the ring comes out at on this map.
 */
export function bringTheRingIntoSight(view) {
    const position = selectedPosition(view);

    if (!position?.accuracyMetres) {
        return;
    }

    const [north] = accuracyRing(position.latitude, position.longitude, position.accuracyMetres, 4);
    const centre = view.map.project([position.longitude, position.latitude]);
    const edge = view.map.project(north);

    if (2 * (centre.y - edge.y) >= RING_TOO_SMALL_PIXELS) {
        return;
    }

    pauseFollowing(view);
    view.map.flyTo({
        center: [position.longitude, position.latitude],
        zoom: Math.max(view.map.getZoom(), START_ZOOM, closeEnoughForTheRing(view, position)),
    });
}

/**
 * Switches the map to the mode picked in the follow menu and moves it to
 * match straight away. Returns false, and changes nothing but the wait, when
 * "one" is picked with nobody left to centre on: the viewer is then asked to
 * pick a member, and setFollowTarget finishes the switch.
 *
 * Whether the member behind "one" is still worth landing on comes from the
 * current positions rather than from the target id alone: an id kept from
 * before is still remembered once the member goes quiet, and following a
 * member who is not on the map would be a mode that does nothing.
 */
export function chooseFollow(view, mode) {
    if (mode === FOLLOW_ONE && !hasFollowTarget(view)) {
        view.awaitingTarget = true;
        view.onCardChange();

        return false;
    }

    view.awaitingTarget = false;
    startFollowing(view, mode);

    view.onCardChange();
    applyFollowMode(view);

    return true;
}

/** Whether "one" has a member on the map to centre on. */
export function hasFollowTarget(view) {
    return targetPosition(view.last?.positions ?? [], view.targetPositionId) !== null;
}

/** Whether the page is waiting for the viewer to pick a member to follow. */
export function awaitingFollowTarget(view) {
    return view.awaitingTarget;
}

/** Stops waiting for a member to follow; the mode stays what it was. */
export function stopAwaitingFollowTarget(view) {
    if (view.awaitingTarget) {
        view.awaitingTarget = false;
        view.onCardChange();
    }
}

/** The mode the follow button is on right now. */
export function followMode(view) {
    return view.mode;
}

/** The position id the "one" mode centres on, or null if nobody has been picked yet. */
export function followTarget(view) {
    return view.targetPositionId;
}

/** Whether a manual pan or zoom is still holding the map still. */
export function followPaused(view) {
    return isPaused(view.pausedAt, Date.now());
}
