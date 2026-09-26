// The map itself: an OpenStreetMap background with one marker per member.
//
// The same background the Android screen draws, so that a position looks the
// same wherever it is looked at.

// Named exports only: the module has no default, and the parts used here are
// the map, a marker and the zoom buttons. The card a marker opens is none of
// the library's business: it is plain DOM, drawn into the element the caller
// hands to createMap, because it belongs to the strip of members rather than
// to the pin it is about.
//
// Read from this site rather than from a content network. Across origins the
// library starts its worker from a blob that imports the real file and drops
// the blob's address in the same breath; the worker dies without a word, and
// everything the worker cuts into tiles — every GeoJSON layer, the accuracy
// ring among them — silently never appears. From here the worker is a plain
// address and starts.
import { Map as MapLibre, Marker, NavigationControl } from './vendor/maplibre-gl.mjs';

import { STALE_COLOUR, memberColour, shortName } from './members.js';
import { showFace } from './faces.js';

import { ageText, isStale, isStuck, positionAge, timeText } from './age.js';
import { clusterMembers, facesShown } from './clusters.js';
import {
    FOLLOW_ALL,
    FOLLOW_NONE,
    FOLLOW_ONE,
    boundsAreClose,
    boundsOf,
    isPaused,
    nextMode,
    targetPosition,
} from './following.js';
import { openModeHelp } from './mode-help.js';
import { openReasonHelp } from './reason-help.js';
import {
    listShown,
    markerTapped,
    openMember,
    selectedPositionId,
    shownPositionIds,
} from './selection.js';
import { markerOrder } from './stacking.js';
import { modeText } from './modes.js';
import { accuracyRing, widthIsWide, widthText } from './width.js';
import { foundText, leftOutText } from './found.js';
import { distanceFromViewer, distanceText } from './distance.js';
import { SPEAKING, WORDS } from './words.js';

/**
 * How far the fitted view is padded from the window's edge, in screen pixels.
 *
 * Enough that a marker at the corner of the rectangle is not touching the
 * account button or the zoom controls that sit over the same corners.
 */
const FOLLOW_ALL_PADDING = 60;

/**
 * How close "following everybody" is ever allowed to come.
 *
 * A group of one has a rectangle with no size at all, and fitting the map to
 * it without a limit would zoom in on that single roof the way [CLOSEST_ZOOM]
 * stops the circle of doubt from doing.
 */
const FOLLOW_ALL_MAX_ZOOM = 17;

/** The ring around the marker being looked at: the fill, the line, the source. */
const WIDTH_SOURCE = 'width';
const WIDTH_FILL_LAYER = 'width-fill';
const WIDTH_LINE_LAYER = 'width-line';

/**
 * How much of the marker's colour the fill keeps.
 *
 * Twelve percent is enough to see where the circle ends and light enough to
 * read the streets through it, which is the whole point of drawing it on the
 * map rather than beside it.
 */
const WIDTH_FILL_OPACITY = 0.12;

/** The line round the ring, in screen pixels rather than in metres. */
const WIDTH_LINE_WIDTH = 1.5;

/** Close enough to tell which street somebody is on, without hunting for them. */
const START_ZOOM = 15;

/**
 * How much of the window the circle of doubt is aimed to fill.
 *
 * Half of the shorter side: enough that the ring reads as a claim about an
 * area rather than as a halo round the pin, and not so much that the streets
 * it is there to be compared against leave the window.
 */
const RING_SHARE_OF_THE_VIEW = 0.5;

/**
 * How close the map is ever taken by a circle.
 *
 * Eighteen is a courtyard across the window. An answer five metres wide would
 * otherwise pull the map down onto a single roof, where nothing around it says
 * where that roof is.
 */
const CLOSEST_ZOOM = 18;

/** How many metres a pixel covers at the equator at zoom nought. */
const EQUATOR_METRES_PER_PIXEL = 156543.03392;


/**
 * How close two pins have to come before they are drawn as one.
 *
 * A little less than the width of a pin: at that distance the heads already
 * overlap, and the labels above them have been unreadable for a while.
 */
const CLUSTER_RADIUS = 44;

/**
 * The colour a group of members wears.
 *
 * Deliberately none of the member colours: a wide pin in somebody's own colour
 * would read as that member standing there with the others tucked behind them.
 */
const GROUP_COLOUR = '#5c6bc0';

/** The head of a pin, the step between its faces, and the inset of the first one. */
const PIN_HEAD = 48;
const FACE_STEP = 28;
const FACE_INSET = 6;

/**
 * The tiles, in the two shades a browser asks for.
 *
 * A tile is a picture drawn on the server, so its colours cannot be changed
 * here: a dark map is a different set of tiles rather than a setting. The
 * light ones are the ones the phones draw; the dark ones are Carto's rendering
 * of the same OpenStreetMap data, which is what makes the page at night as
 * readable as the rest of it.
 */
const STYLE = {
    version: 8,
    sources: {
        osm: {
            type: 'raster',
            tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
            tileSize: 256,
            maxzoom: 19,
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        },
        // Declared with the style rather than added to it once the map has
        // loaded: a source put in afterwards was accepted, kept the data it was
        // given and never went on to build anything out of it — loaded() stayed
        // false, nothing was ever rendered from it, and no error was raised
        // anywhere to say so. Part of the style from the start, it behaves.
        [WIDTH_SOURCE]: { type: 'geojson', data: { type: 'FeatureCollection', features: [] } },
    },
    layers: [
        { id: 'osm', type: 'raster', source: 'osm' },
        {
            id: WIDTH_FILL_LAYER,
            type: 'fill',
            source: WIDTH_SOURCE,
            paint: {
                'fill-color': ['get', 'colour'],
                'fill-opacity': WIDTH_FILL_OPACITY,
            },
        },
        {
            id: WIDTH_LINE_LAYER,
            type: 'line',
            source: WIDTH_SOURCE,
            paint: {
                'line-color': ['get', 'colour'],
                'line-width': WIDTH_LINE_WIDTH,
            },
        },
    ],
};

/**
 * @param element where the map is drawn
 * @param onCardChange called whenever a card opens, closes or changes who it
 *        is about, so that the panel can mark the card being looked at
 * @param cardElement where the card is drawn — over the strip of members,
 *        rather than pinned to the marker it is about, so that opening one
 *        never covers the very place on the map the viewer is reading
 */
export function createMap(element, onCardChange = () => {}, cardElement) {
    const map = new MapLibre({
        container: element,
        style: STYLE,
        center: [0, 0],
        zoom: 1,
        // Nothing here is drawn in three dimensions, and a map that rotates
        // under a finger is a map the viewer then has to straighten.
        pitchWithRotate: false,
        dragRotate: false,
    });

    // Down in the far corner rather than the near one: the account button
    // sits at the top right, and the two of them overlapped there — the
    // library places its controls at the very edge, under a button the page
    // draws over the map.
    map.addControl(new NavigationControl({ showCompass: false }), 'bottom-right');

    // A tile that does not arrive, a style that cannot be read: the library
    // keeps those to itself unless somebody listens, and the page then shows
    // an empty map with nothing said anywhere about why.
    map.on('error', (event) => {
        console.error('map:', event.error?.message ?? event.error ?? event);
    });

    const view = {
        map,
        markers: new Map(),
        centred: false,
        last: null,
        onCardChange,
        card: cardElement,
        // The single card open at any one time, or null: which marker it is
        // about and, when that marker covers several members, which one of
        // them. The map, the strip and the circle of doubt all ask that
        // question, so the answer is kept here as plain data, by the rules in
        // lib/selection.js, rather than inside whatever happens to be drawn.
        selection: null,
        // The circle the follow button walks, and who it currently follows.
        // Read and advanced from outside through the exported follow* and
        // advanceFollow functions rather than written here directly.
        mode: FOLLOW_NONE,
        targetPositionId: null,
        // When the last manual pan or zoom happened; null before any gesture.
        // Read through isPaused rather than compared directly, because the
        // window it opens is a duration rather than a single moment.
        pausedAt: null,
        // The rectangle "follow everybody" last fitted the map to, so that a
        // rectangle barely different from it does not nudge the view again.
        followedBounds: null,
        // Asked to name a phone or a person, from the card: `(kind, key)`,
        // where kind is `devices` or `members`. Set by the page; a card with
        // nobody to ask offers no names.
        onName: null,
        ownMemberId: null,
        personName: null,
    };

    // The ring's source and layers come with the style; what has to wait is the
    // drawing, because positions can arrive before the style has.
    const ringIsReady = () => {
        if (view.ringReady || !map.isStyleLoaded()) {
            return;
        }

        view.ringReady = true;
        drawWidth(view);
    };

    map.on('load', ringIsReady);
    map.on('styledata', ringIsReady);

    // Which members cover each other is a question about the screen, so the
    // answer changes with every zoom: a group that was one pin has to come
    // apart once the map is close enough to tell them apart. Redrawn when the
    // map stops rather than while it moves, because regrouping under a moving
    // finger reads as flicker.
    //
    // A flight cut short by another also ends with moveend, fired from wherever
    // the first one had got to, and the second flight starts before the event
    // has been handled. Grouping there would group at the height of the flight:
    // two members a few hundred metres apart become one marker between them
    // for the rest of the way, and the circle of the member flown to stands on
    // its own. Looked at once the flight that cut in has had its chance to
    // start, and left alone while the map is still on its way.
    map.on('moveend', () => {
        setTimeout(() => {
            if (map.isMoving()) {
                return;
            }

            if (view.last && draw(view, view.last)) {
                // Unlike the draw a render triggers, nothing runs after this
                // one to read view.selection again — the panel has to be told
                // itself.
                view.onCardChange();
            }
        }, 0);
    });

    // A pan or a pinch is a hand on the map; the same kind of movement also
    // happens under easeTo and fitBounds, which the follow modes call below.
    // The library tells the two apart with originalEvent: present on a
    // gesture, absent on a call from code. Only a hand earns the pause — a
    // follow mode moving the map on its own must not immediately pause
    // itself.
    map.on('movestart', (event) => {
        if (event.originalEvent) {
            view.pausedAt = Date.now();
            // The hand has taken the map somewhere the fitted rectangle no
            // longer describes. Forgetting the rectangle is what lets
            // "everybody" fit the view again once the pause wears off:
            // otherwise a household that has not moved hands back the same
            // rectangle, it is found close to the one already fitted, and the
            // map stays where the hand left it with the mode still on.
            view.followedBounds = null;
        }
    });

    // A tap away from everybody means the card is no longer being looked at,
    // which is how the phone reads the same tap. A tap on a marker is caught
    // first, in addMarker, and stopped there from bubbling — without that,
    // this handler would close the card the very tap that opened it was meant
    // to leave open.
    map.on('click', () => {
        if (closeCards(view)) {
            view.onCardChange();
            drawWidth(view);
        }
    });

    // The way a dialog is dismissed everywhere else: the card covers the map,
    // and reaching for the small cross is the long way round. Listened for on
    // the document rather than on the map element, because the card takes the
    // focus when it opens and the map no longer has it.
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') {
            return;
        }

        if (closeCards(view)) {
            view.onCardChange();
            drawWidth(view);
        }
    });

    return view;
}

/** Closes whatever card is open, and says whether there was one. */
function closeCards(view) {
    if (view.selection === null) {
        return false;
    }

    view.selection = null;
    view.card.hidden = true;
    view.card.replaceChildren();

    return true;
}

/**
 * Which marker a member is standing under right now, or null if none of them
 * covers that member.
 *
 * Asked rather than remembered, because a marker is a group of whoever is
 * close together on screen and the screen keeps changing: coming closer takes
 * one marker apart into several, and going away puts them back. A member with
 * no id at all — which is how a card over a list of members names nobody in
 * particular — is nowhere by definition.
 */
function markerKeyFor(view, positionId) {
    if (positionId === null) {
        return null;
    }

    for (const [key, entry] of view.markers) {
        if (entry.members.some((member) => member.positionId === positionId)) {
            return key;
        }
    }

    return null;
}

/**
 * Moves the markers that moved, adds the ones that appeared and removes the
 * ones that went away.
 *
 * Kept rather than redrawn, unlike the Android side: a marker here carries its
 * own element, and replacing it on every update would close a detail bubble
 * the viewer had just opened.
 *
 * What a marker stands for is a group of members rather than one member: two
 * people in one flat are one pin at every zoom the map has, and two markers
 * there would cover each other's labels.
 */
export function showMembers(view, positions, names, faces, ownMemberId, now) {
    // Held so that the map can redraw itself when it is moved: grouping is
    // about the screen, and the screen changes without the positions doing so.
    view.last = { positions, names, faces, now };
    // The card offers to name a person to everybody but the viewer, who is
    // named on their account.
    view.ownMemberId = ownMemberId;

    // Not while the map is on its way somewhere, for the reason moveend waits:
    // grouping at the height of a flight joins members the destination tells
    // apart. The moveend at the end draws what is kept above.
    if (view.map.isMoving()) {
        return;
    }

    draw(view, view.last);
    applyFollowMode(view);
}

/**
 * Moves the map to match the follow mode, once a fresh set of positions has
 * been drawn.
 *
 * Nothing here runs on a timer of its own: a drawing is the only moment new
 * positions are in hand to follow, so this is called from nowhere else.
 */
function applyFollowMode(view) {
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
        { padding: FOLLOW_ALL_PADDING, maxZoom: FOLLOW_ALL_MAX_ZOOM },
    );
}

/** Remembers who a card just opened for, for the "one" follow mode to centre on. */
function setFollowTarget(view, positionId) {
    view.targetPositionId = positionId;
}

function draw(view, { positions, names, faces, now }) {
    const placed = positions.map((position) => {
        const point = view.map.project([position.longitude, position.latitude]);

        return { id: position.positionId, x: point.x, y: point.y };
    });

    const byId = new Map(positions.map((position) => [position.positionId, position]));
    const seen = new Set();

    for (const cluster of clusterMembers(placed, CLUSTER_RADIUS)) {
        const group = cluster.map((memberId) => byId.get(memberId)).filter(Boolean);

        if (group.length === 0) {
            continue;
        }

        // The positions in it are what a marker is: a group that gains or
        // loses one is a different marker, with a different number of faces.
        const key = cluster.join(',');

        seen.add(key);

        const entry = view.markers.get(key) ?? addMarker(view, key, group);

        entry.members = group;

        const at = [centreLongitude(group), centreLatitude(group)];

        entry.marker.setLngLat(at);

        // Restated on every draw rather than once: the pile is an order of
        // what is on screen, and panning the map changes which member is in
        // front of which without changing anything about the members.
        entry.marker.getElement().style.zIndex = markerOrder(view.map.project(at).y);

        showPin(entry, names, faces, now);

        // What the card holds is built for the card that is open, and for no
        // other: the card is a heading, a face and five rows, and building
        // one per marker on every tick is work nobody sees.
        entry.latest = { names, faces, now };

        if (view.selection && view.selection.key === key) {
            showCard(view, names, faces, now);
        }
    }

    for (const [key, entry] of view.markers) {
        if (!seen.has(key)) {
            entry.marker.remove();
            view.markers.delete(key);
        }
    }

    // The marker a card was about can vanish between two drawings: coming
    // closer takes a group apart into a marker each, and going away puts them
    // back together. The card is about a member rather than about a pile of
    // pins, so it moves to whichever marker now covers them, and only closes
    // when the member themselves has left the positions.
    //
    // A card over a list has nobody to follow — the list is the pile — so it
    // goes when the pile does.
    //
    // Whether it closed is the answer rather than a call to onCardChange from
    // inside a drawing: a drawing is usually something the panel asked for,
    // and showMembers reads the selection again the moment this returns.
    let closed = false;

    if (view.selection && !seen.has(view.selection.key)) {
        const moved = markerKeyFor(view, view.selection.positionId);

        if (moved === null) {
            closeCards(view);
            closed = true;
        } else {
            view.selection = openMember(moved, view.selection.positionId);
            showCard(view, names, faces, now);
        }
    }

    // After the markers that went away have gone: a ring is drawn around a
    // marker whose card is open, and a marker taken off the map has none.
    drawWidth(view);

    return closed;
}

/**
 * The circle around the member being looked at, or nothing at all.
 *
 * One at a time rather than all of them: four circles of a kilometre and a
 * half cover the map in each other, and the question a circle answers is asked
 * about the member being looked at.
 *
 * Redrawn with the markers rather than only when a card opens: the position
 * under the circle moves on its own, and a ring left where the member used to
 * be would be a claim about the wrong place.
 */
function drawWidth(view) {
    if (!view.ringReady) {
        return;
    }

    const position = selectedPosition(view);
    const source = view.map.getSource(WIDTH_SOURCE);


    if (!position || position.accuracyMetres === null) {
        source?.setData(emptyRing());

        return;
    }

    // A collection of one rather than the bare feature, and the colour carried
    // inside it rather than set on the layer: the source is declared as GeoJSON
    // and a feature handed to it on its own left the layer with nothing to
    // draw, silently — no error anywhere, the layer present, the paint set, and
    // no circle on the map.
    //
    // The member's own colour rather than one colour for every circle: the
    // circle belongs to a marker and has to be readable as that marker's when
    // two members are near each other.
    source?.setData({
        type: 'FeatureCollection',
        features: [{
            type: 'Feature',
            geometry: {
                type: 'Polygon',
                coordinates: [
                    accuracyRing(position.latitude, position.longitude, position.accuracyMetres),
                ],
            },
            properties: { colour: memberColour(position.memberId) },
        }],
    });
}

/**
 * Whose card is open, where exactly one member's is.
 *
 * A list of who stands under one marker is not one of them: which member the
 * viewer means is the question that list is asking, and a circle drawn for any
 * one of them would answer it for them.
 */
function selectedPosition(view) {
    const entry = view.selection ? view.markers.get(view.selection.key) : null;

    if (!entry) {
        return null;
    }

    const positionId = selectedPositionId(view.selection);

    return entry.members.find((member) => member.positionId === positionId) ?? null;
}

/** What the source holds when there is no circle to draw. */
function emptyRing() {
    return { type: 'FeatureCollection', features: [] };
}

function addMarker(view, key, group) {
    const element = document.createElement('div');
    element.className = 'member-marker';

    // The label is always up, the way a place name is on a map: a pin on its
    // own says somebody is there, not who.
    const label = document.createElement('span');
    label.className = 'member-label';

    // A pin rather than a dot, and a face inside it: the point at the bottom
    // is the position, so the face can be this large without covering the
    // place the member actually is. The same shape the phone draws.
    const pin = document.createElement('span');
    pin.className = 'member-pin';

    const body = document.createElement('span');
    body.className = 'pin-body';

    const point = document.createElement('span');
    point.className = 'pin-point';

    pin.append(body, point);

    const circles = circleCount(group.length);

    // The head is a capsule, so it can be widened to hold the faces and keep
    // the round ends a single member has.
    pin.style.width = `${PIN_HEAD + FACE_STEP * (circles - 1)}px`;

    const faces = [];

    for (let index = 0; index < circles; index += 1) {
        const face = document.createElement('span');

        face.className = 'pin-face';
        face.style.left = `${FACE_INSET + FACE_STEP * index}px`;
        pin.append(face);
        faces.push(face);
    }

    element.append(label, pin);

    // Held before the click handler, which reads it back on every tap rather
    // than closing over the group this marker happened to have when it was
    // made — the members under one key never change, but which of them the
    // card is about does.
    const entry = {
        marker: null,
        pin,
        faces,
        label,
        members: group,
        // What the last drawing knew, kept for the moment the card is opened
        // between two drawings.
        latest: null,
    };

    // A tap on the marker is a choice about the card, and it must not reach
    // the map underneath: the map's own click handler closes whatever card is
    // open, and left alone it would close this same tap's card the instant it
    // opened.
    element.addEventListener('click', (event) => {
        event.stopPropagation();

        const positionIds = entry.members.map((member) => member.positionId);

        // Worked out before it is stored, because closeCards is what takes
        // the card off the screen and it does nothing at all once the
        // selection is already empty: writing the answer down first would
        // leave the card standing there with nothing selected behind it.
        const tapped = markerTapped(view.selection, key, positionIds);

        if (tapped === null) {
            closeCards(view);
        } else {
            view.selection = tapped;

            // A marker for one member opens straight onto their card, so
            // tapping it is as much a choice of who to follow as tapping
            // their entry in the strip. A marker for several opens onto the
            // list instead, and no one of them is meant until a row in it is
            // tapped — that click sets the target itself, in groupList.
            if (view.selection.positionId !== null) {
                setFollowTarget(view, view.selection.positionId);
            }

            // Filled on the way open rather than kept filled: until it is
            // asked for, an empty card costs nothing. entry.latest is set on
            // every drawing, and a marker cannot be tapped before its first
            // one.
            if (entry.latest) {
                showCard(view, entry.latest.names, entry.latest.faces, entry.latest.now);
            }
        }

        view.onCardChange();
        drawWidth(view);
    });

    // Placed before it is added: a marker with nowhere to be cannot be put on
    // a map, and the caller sets the position again on every update anyway.
    // Hung by its bottom edge, which is the point of the pin: the member is
    // where the point is, and everything else stands above them.
    entry.marker = new Marker({ element, anchor: 'bottom' })
        .setLngLat([centreLongitude(group), centreLatitude(group)])
        .addTo(view.map);

    view.markers.set(key, entry);

    return entry;
}

/** How many circles a pin has: one per face, and one more for the count if any are left out. */
function circleCount(count) {
    const shown = facesShown(count);

    return count > shown ? shown + 1 : shown;
}

/** What the marker looks like: its colour, its label and the faces in it. */
function showPin(entry, names, faces, now) {
    const group = entry.members;

    if (group.length === 1) {
        const member = group[0];
        const name = nameOf(member, names);
        const stale = isStale(member.knownAt, now);
        const colour = stale ? STALE_COLOUR : memberColour(member.memberId);

        entry.label.textContent = name;
        entry.pin.style.setProperty('--pin', colour);
        entry.pin.classList.toggle('stale', stale);
        showFace(entry.faces[0], faces.get(member.memberId), name, colour);

        return;
    }

    // Grey only when everybody under the marker has gone quiet: one member
    // still reporting is a reason to trust the marker.
    const stale = group.every((member) => isStale(member.knownAt, now));

    entry.label.textContent = groupLabel(group.length);
    entry.pin.style.setProperty('--pin', stale ? STALE_COLOUR : GROUP_COLOUR);
    entry.pin.classList.toggle('stale', stale);

    const shown = group.slice(0, facesShown(group.length));

    shown.forEach((member, index) => {
        const name = nameOf(member, names);
        const old = isStale(member.knownAt, now);
        const face = entry.faces[index];

        const colour = old ? STALE_COLOUR : memberColour(member.memberId);

        showFace(face, faces.get(member.memberId), name, colour);
        face.classList.toggle('stale', old);
    });

    const counter = entry.faces[shown.length];

    if (counter) {
        // A plus and a number, which is the same in every language the labels
        // above the pins could be written in.
        counter.className = 'pin-face more';
        counter.style.backgroundImage = '';
        counter.textContent = `+${group.length - shown.length}`;
    }
}

/**
 * What is left in the phone, written green while it is filling: a low
 * percentage on a charger and the same one off it mean opposite things, and
 * the colour says which without taking any room on the card.
 */
export function batteryText(position, into = document.createElement('span')) {
    into.replaceChildren();
    into.classList.toggle('charging', position.charging && position.batteryPercent !== null);
    into.textContent =
        position.batteryPercent === null ? WORDS.unknown : `${position.batteryPercent}%`;

    return into;
}

/** What a marker for several members writes above itself. */
function groupLabel(count) {
    return count === 1 ? WORDS.oneMember : WORDS.members(count);
}

/**
 * Draws the one card the whole page ever shows at once: one member's detail,
 * or the list of who is standing under a marker that covers several of them.
 *
 * Which of the group was meant is a question only the viewer can answer, so
 * the list comes first and the detail is one click further in. That choice
 * lives in view.selection rather than in the card itself, so that a tick that
 * redraws the card does not throw the viewer back to the list.
 *
 * Nothing here decides whether a card should be open at all — that is what
 * closeCards is for — only what the open one looks like.
 */
function showCard(view, names, faces, now) {
    const entry = view.markers.get(view.selection.key);

    // The marker this card names is not on the map right now. draw() closes
    // a card left stranded like this once a drawing has actually happened;
    // reached from a click instead, a stale selection would otherwise be
    // rendered as a card about nobody.
    if (!entry) {
        closeCards(view);

        return;
    }

    const group = entry.members;
    const chosen = group.find((member) => member.positionId === view.selection.positionId);

    let content;

    if (group.length === 1) {
        content = detail(view, group[0], nameOf(group[0], names), faces, now);
    } else if (!chosen) {
        content = groupList(view, entry, names, faces, now);
    } else {
        content = detail(view, chosen, nameOf(chosen, names), faces, now);

        const back = document.createElement('button');

        back.type = 'button';
        back.className = 'group-back';
        // The arrow says which way the button goes before the words are read; an
        // arrow written as a character rather than drawn, because a set of icons
        // is a dependency of its own and this is the only one the page needs.
        back.textContent = WORDS.backToList;
        back.addEventListener('click', () => {
            view.selection = listShown(view.selection);
            showCard(view, names, faces, now);
            view.onCardChange();
            drawWidth(view);
        });

        content.prepend(back);
    }

    // Named in the language the rest of the card is read in: a cross drawn
    // out of two lines has no text inside it for a reader who is listening to
    // the page rather than looking at it.
    const close = document.createElement('button');

    close.type = 'button';
    close.className = 'card-close';
    close.setAttribute('aria-label', WORDS.close);
    close.addEventListener('click', () => {
        if (closeCards(view)) {
            view.onCardChange();
            drawWidth(view);
        }
    });

    // Scrolling lives on this wrapper rather than on #card itself: #card is
    // also the close button's positioned ancestor, and an ancestor that
    // scrolls its own box carries every descendant along with it, the close
    // button included — the corner it is meant to stay pinned to would drift
    // out of view with the rest of the card. A plain content box underneath
    // it, scrolling on its own, leaves the button where it was tapped from.
    const body = document.createElement('div');

    body.className = 'card-body';
    body.append(content);

    view.card.replaceChildren(close, body);
    view.card.hidden = false;
}

/**
 * Whose cards are open, as position ids.
 *
 * A card about one member names that member; a list of who stands under one
 * marker names all of them, because the viewer is looking at the group.
 */
export function shownPositions(view) {
    if (view.selection === null) {
        return new Set();
    }

    const entry = view.markers.get(view.selection.key);

    if (!entry) {
        return new Set();
    }

    return shownPositionIds(view.selection, entry.members.map((member) => member.positionId));
}

/** The members under one marker, as rows that open their cards. */
function groupList(view, entry, names, faces, now) {
    const list = document.createElement('div');

    list.className = 'group';

    const heading = document.createElement('h2');

    heading.textContent = groupLabel(entry.members.length);
    list.append(heading);

    for (const member of entry.members) {
        const row = document.createElement('button');

        row.type = 'button';
        row.className = 'group-row';

        const stale = isStale(member.knownAt, now);
        const colour = stale ? STALE_COLOUR : memberColour(member.memberId);
        const label = nameOf(member, names);

        // The same circle the panel and the pin carry, so that one member is
        // one drawing wherever they are named.
        const face = document.createElement('span');

        face.className = stale ? 'member-face stale' : 'member-face';
        face.style.setProperty('--ring', colour);
        showFace(face, faces.get(member.memberId), label, colour);

        const name = document.createElement('span');

        name.className = 'group-name';
        name.textContent = label;

        const age = document.createElement('span');

        age.className = stale ? 'group-age stale' : 'group-age';
        age.textContent = ageText(positionAge(member.positionAt, now));

        row.append(face, name, age);
        row.addEventListener('click', () => {
            view.selection = openMember(view.selection.key, member.positionId);
            setFollowTarget(view, member.positionId);
            showCard(view, names, faces, now);
            view.onCardChange();
            drawWidth(view);
        });

        list.append(row);
    }

    return list;
}

function nameOf(member, names) {
    return names.get(member.positionId) ?? shortName(member.memberId);
}

/**
 * Where a marker for several members is put: the middle of the people under it.
 *
 * The middle rather than one of them, because the marker stands for all of
 * them; putting it on the first id would move the whole group whenever that
 * member walked off.
 */
function centreLatitude(group) {
    return group.reduce((total, member) => total + member.latitude, 0) / group.length;
}

/**
 * The same middle, along the other axis.
 *
 * Longitudes are averaged as offsets from the first member rather than
 * outright: on a map of the whole world two members either side of the date
 * line are a few pixels apart, and their longitudes are half a globe apart as
 * numbers.
 */
function centreLongitude(group) {
    const first = group[0].longitude;
    const offset =
        group.reduce((total, member) => total + wrapDegrees(member.longitude - first), 0) /
        group.length;

    return wrapDegrees(first + offset);
}

/** The same angle, written as the turn of at most half a circle either way. */
function wrapDegrees(degrees) {
    return ((((degrees + 180) % 360) + 360) % 360) - 180;
}

/**
 * What the bubble says once the viewer asks.
 *
 * Everything the position document carries beyond the two coordinates: when it
 * was taken, how the phone was collecting at the time and how much battery it
 * had left, which together answer "why is this old" without a second question.
 *
 * Built as elements rather than as text, so that a member id — written by
 * whoever holds the account — has no way of being read as markup.
 */
function detail(view, position, name, faces, now) {
    const card = document.createElement('div');
    card.className = 'detail';

    // The same face the marker carries, so that a card opened from the panel
    // is recognisably the person whose pin is on the map.
    const heading = document.createElement('div');

    heading.className = 'detail-head';

    // Old means grey here as well as on the pin and in the strip: a card in
    // full colour beside a faded pin reads as a member who is there right now.
    const stale = isStale(position.knownAt, now);
    const colour = stale ? STALE_COLOUR : memberColour(position.memberId);

    const face = document.createElement('span');

    face.className = stale ? 'detail-face stale' : 'detail-face';
    face.style.setProperty('--ring', colour);
    showFace(face, faces.get(position.memberId), name, colour);

    const titles = document.createElement('div');
    const title = document.createElement('h2');
    const device = document.createElement('span');
    const own = position.memberId === view.ownMemberId;

    // The person and the phone on lines of their own, whatever the markers
    // say, because each of the two is what is pressed to rename it: a row of
    // its own for renaming took a line of a card that is short of them.
    // Oneself is named on the account, not here.
    const person = view.personName?.(position) ?? name;

    title.append(nameWord(view, person, own ? null : ['members', position.memberId]));
    device.className = 'detail-device';
    device.append(nameWord(view, position.deviceName || WORDS.unnamedDevice, ['devices', position.positionId]));
    titles.append(title, device);

    heading.append(face, titles);

    card.append(heading, summary(view, position, now));

    const warnings = warningsOf(position, now);

    if (warnings) {
        card.append(warnings);
    }

    // Everything else behind one button, and the choice kept: a viewer who
    // opens the details wants them on the next card too, and one who closed
    // them opened the card for the summary.
    const details = detailTable(position);
    const more = document.createElement('button');

    view.detailsOpen ??= readDetailsOpen();
    details.id = 'card-details';
    more.type = 'button';
    more.className = 'detail-more';
    more.setAttribute('aria-controls', details.id);

    const showDetails = (open) => {
        details.hidden = !open;
        more.setAttribute('aria-expanded', String(open));
        more.textContent = open ? WORDS.fewerDetails : WORDS.moreDetails;
    };

    showDetails(view.detailsOpen);
    more.addEventListener('click', () => {
        view.detailsOpen = !view.detailsOpen;
        writeDetailsOpen(view.detailsOpen);
        showDetails(view.detailsOpen);
    });

    card.append(details, more);

    return card;
}

/**
 * The line under the name: how far, how old the position, how long since the
 * phone was online, and the charge.
 *
 * The two ages side by side because under a spoof they are hours apart, and a
 * card naming only one of them is read as naming the other.
 */
function summary(view, position, now) {
    const line = document.createElement('div');

    line.className = 'detail-summary';

    const away = distanceFromViewer(position, view.last?.positions ?? [], view.ownMemberId, now);
    const parts = [
        ...(away === null ? [] : [WORDS.fromYou(distanceText(away))]),
        WORDS.positionAgo(ageText(positionAge(position.positionAt, now))),
        WORDS.onlineAgo(ageText(positionAge(position.knownAt, now))),
    ];

    for (const text of parts) {
        const part = document.createElement('span');

        part.textContent = text;
        line.append(part);
    }

    line.append(batteryText(position));

    return line;
}

/**
 * What is wrong with the position, as one paragraph, or null when nothing is.
 *
 * Each sentence only while it is true: a marker left behind by a phone that is
 * still online, answers being rejected, a receiver that has not settled. Put
 * together rather than as rows, because they are read as one account of why
 * the marker should not be trusted.
 */
function warningsOf(position, now) {
    // Each sentence is a list of pieces: the one about rejected answers holds
    // the reason as a word to press.
    const sentences = [];

    if (isStuck(position.positionAt, position.knownAt, now)) {
        sentences.push([WORDS.stuckSentence(ageText(positionAge(position.knownAt, now)))]);
    }

    if (position.leftOutCount > 0) {
        const reason = reasonButton(position.leftOutReason);

        reason.textContent = lowerFirst(reason.textContent);
        const lead = WORDS.leftOutLead(WORDS.leftOutAnswers(position.leftOutCount));

        sentences.push([lead, ' ', reason, '.']);
    }

    if (position.settling) {
        sentences.push([WORDS.settlingSentence]);
    }

    if (sentences.length === 0) {
        return null;
    }

    const paragraph = document.createElement('p');

    paragraph.className = 'detail-warn';
    sentences.forEach((pieces, index) => paragraph.append(...(index > 0 ? [' '] : []), ...pieces));

    return paragraph;
}

/** The first letter made small, for a reason read in the middle of a sentence. */
function lowerFirst(text) {
    return text.charAt(0).toLocaleLowerCase(SPEAKING) + text.slice(1);
}

/** The rows behind the button: the two moments, and how the answer was arrived at. */
function detailTable(position) {
    const table = document.createElement('table');

    const rows = [
        [WORDS.at, timeText(position.positionAt)],
        [WORDS.online, timeText(position.knownAt)],
        [WORDS.mode, modeButton(position.mode)],
        [WORDS.foundBy, foundText(position.foundBy)],
        [WORDS.width, widthValue(position.accuracyMetres)],
        [WORDS.position, `${position.latitude.toFixed(5)}, ${position.longitude.toFixed(5)}`],
    ];

    for (const [title, value] of rows) {
        const row = document.createElement('tr');
        const head = document.createElement('th');
        const cell = document.createElement('td');

        head.textContent = title;

        if (value instanceof Node) {
            cell.append(value);
        } else {
            cell.textContent = value;
        }

        row.append(head, cell);
        table.append(row);
    }

    return table;
}

/** Where the choice of details is kept, in this browser only. */
const DETAILS_KEY = 'geoshare.cardDetails';

function readDetailsOpen() {
    try {
        return localStorage.getItem(DETAILS_KEY) === 'open';
    } catch {
        return false;
    }
}

function writeDetailsOpen(open) {
    try {
        localStorage.setItem(DETAILS_KEY, open ? 'open' : 'closed');
    } catch {
        // A browser that keeps nothing forgets the choice with the tab.
    }
}

/**
 * A name at the head of the card: a word to press where the page has asked to
 * be told and the name is one the viewer may give, plain text otherwise.
 */
function nameWord(view, text, naming) {
    if (!view.onName || !naming) {
        return document.createTextNode(text);
    }

    const button = document.createElement('button');

    button.type = 'button';
    button.className = 'detail-rename';
    button.textContent = text;
    button.addEventListener('click', () => view.onName(...naming));

    return button;
}

/**
 * The width as the card writes it, ochre once the answer names a block rather
 * than a building.
 *
 * The colour rather than a word beside the number, and only here now that the
 * strip has stopped carrying the figure: the card is where a width is read at
 * all, and the whole point of the colour is that the eye stops on it. The same
 * hundred metres the application colours at, because the two clients share a
 * database and nothing else, and one width read two ways is read as two.
 */
function widthValue(metres) {
    if (metres === null) {
        return WORDS.unknown;
    }

    if (!widthIsWide(metres)) {
        return widthText(metres);
    }

    const wide = document.createElement('span');

    wide.className = 'detail-width wide';
    wide.textContent = widthText(metres);

    return wide;
}

/**
 * The mode as a thing to press, because a name alone answers nothing.
 *
 * "Standby" says what the phone was doing only to somebody who already knows
 * the five; the help behind it is where they are read against each other, and
 * a card is the one place a member meets a mode at all.
 */
function modeButton(mode) {
    const button = document.createElement('button');

    button.className = 'mode-help-open';
    button.type = 'button';
    button.textContent = modeText(mode);
    button.addEventListener('click', () => openModeHelp());

    return button;
}

/**
 * The reason as a thing to press, for the same reason the mode is one.
 *
 * "Two positions at one time" names what the phone saw without saying what it
 * means, and a card is the one place a member meets a reason at all. The word
 * shown is the one the card would have shown anyway, the switched-off source
 * named among it.
 */
function reasonButton(reason) {
    const button = document.createElement('button');

    button.className = 'mode-help-open left-out';
    button.type = 'button';
    button.textContent = leftOutText(reason);
    button.addEventListener('click', () => openReasonHelp());

    return button;
}

/**
 * Opens the map on the first position to arrive, once.
 *
 * After that the viewer is in charge of what they are looking at, and a list
 * that updates every few seconds must not drag the view back.
 */
export function centreOnce(view, positions, ownMemberId) {
    if (view.centred) {
        return;
    }

    const start = positions.find((position) => position.memberId === ownMemberId) ?? positions[0];

    if (!start) {
        return;
    }

    view.centred = true;
    view.map.jumpTo({ center: [start.longitude, start.latitude], zoom: START_ZOOM });
}

/** Takes the map to one member and opens their detail, the way tapping a place does. */
export function lookAt(view, position) {
    // A tap in the strip is as much a choice of who to follow as tapping the
    // marker itself, and it is the one place that choice can be made without
    // a marker already being on screen to tap.
    setFollowTarget(view, position.positionId);

    // Flown to rather than jumped to, because the viewer asked to be taken
    // somewhere and the way there says how far it was.
    view.map.flyTo({
        center: [position.longitude, position.latitude],
        zoom: Math.max(view.map.getZoom(), START_ZOOM, closeEnoughForTheRing(view, position)),
    });

    const key = markerKeyFor(view, position.positionId);

    if (key === null) {
        return;
    }

    // A member asked for by name opens their own card even when the marker
    // stands for several: the question was about them, not about the place.
    view.selection = openMember(key, position.positionId);

    if (view.last) {
        showCard(view, view.last.names, view.last.faces, view.last.now);
    }

    // Said out loud rather than left to the next drawing: a card already open
    // over a group changes who it is about without opening again, and the
    // strip would go on marking the member the viewer has just navigated away
    // from until the next redraw — half a minute, on the clock that moves the
    // ages along.
    view.onCardChange();
    drawWidth(view);
}

/**
 * How close the map has to come for the circle of doubt to be worth drawing.
 *
 * The circle is drawn in metres, and a browser window is three times as wide
 * as a phone: at the zoom both clients start at, a hundred and sixty metres of
 * doubt comes out thirty-three pixels across on a laptop, which is the size of
 * the pin standing on top of it — the ring is there and cannot be seen. On a
 * phone the same ring covers a good part of a narrow screen, which is why the
 * Android map needs nothing of the sort.
 *
 * The answer is the zoom at which the circle spans [RING_SHARE_OF_THE_VIEW] of
 * the shorter side of the window, held back at [CLOSEST_ZOOM] so that a sharp
 * answer does not drop the viewer onto a single roof. Only ever used to come
 * closer: whoever zoomed out did it on purpose.
 */
function closeEnoughForTheRing(view, position) {
    const metres = position.accuracyMetres;

    if (!metres) {
        return 0;
    }

    const canvas = view.map.getCanvas();
    const side = Math.min(canvas.clientWidth, canvas.clientHeight);
    const wanted = (2 * metres) / (side * RING_SHARE_OF_THE_VIEW);
    const across = EQUATOR_METRES_PER_PIXEL * Math.cos((position.latitude * Math.PI) / 180);

    return Math.min(Math.log2(across / wanted), CLOSEST_ZOOM);
}

/** The map measures its element when it is created, and the sign-in screen hides it. */
export function remeasure(view) {
    view.map.resize();
}

/**
 * Walks the follow button's circle one tap forward and moves the map to
 * match straight away, unless a gesture is holding it still. Returns the mode
 * the button is on after the tap, which is what the caption is drawn from.
 *
 * Whether the member behind "one" is still worth landing on comes from the
 * current positions rather than from the target id alone: an id kept from
 * before is still remembered once the member goes quiet, but nextMode has to
 * be told there is nowhere left to centre on, or the circle would stall on a
 * mode that does nothing.
 */
export function advanceFollow(view) {
    const hasTarget = targetPosition(view.last?.positions ?? [], view.targetPositionId) !== null;

    view.mode = nextMode(view.mode, hasTarget);

    // Pressing the button is a viewer asking for the map to move now. The
    // pause exists to keep a followed member from yanking the view out from
    // under a hand, which is the opposite of what a press means; and the
    // rectangle last fitted describes a view the viewer may since have left.
    view.pausedAt = null;
    view.followedBounds = null;

    view.onCardChange();
    applyFollowMode(view);

    return view.mode;
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
