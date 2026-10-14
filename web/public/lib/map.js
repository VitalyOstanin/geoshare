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
import { Map as MapLibre, Marker, NavigationControl } from './vendor/maplibre-gl-6.14.0/maplibre-gl.mjs';

import { STALE_COLOUR, freshestOf, memberColour, pinColour } from './members.js';
import { showFace } from './faces.js';

import { isGrey } from './false-places.js';
import { clusterMembers, facesShown } from './clusters.js';
import { FOLLOW_NONE } from './following.js';
import { markerTapped, openMember, selectedPositionId, shownPositionIds } from './selection.js';
import { HIGHEST, markerOrder } from './stacking.js';
import { accuracyRing } from './width.js';
import { WORDS } from './words.js';
import {
    applyFollowMode,
    bringTheRingIntoSight,
    pauseFollowing,
    setFollowTarget,
} from './map-follow.js';
import { closeCards, groupLabel, nameOf, showCard } from './map-card.js';

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
export const START_ZOOM = 15;

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

/**
 * How many metres a pixel covers at the equator at zoom nought.
 *
 * On this library's tiles, which are 512 pixels across: the figure usually
 * quoted, 156543, is for tiles of 256, and with it every ring came out twice
 * the size it was aimed at.
 */
const EQUATOR_METRES_PER_PIXEL = 78_271.51696;

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
 * light ones are the ones the devices draw; the dark ones are Carto's rendering
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
    const view = newView(newMap(element), onCardChange, cardElement);

    listenToTheMap(view);

    return view;
}

/** The map itself, with its controls and an ear for its errors. */
function newMap(element) {
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

    return map;
}

/** What the page knows about the map beyond the map itself. */
function newView(map, onCardChange, cardElement) {
    return {
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
        // The mode picked in the follow menu, and who it currently follows.
        // Read and set from outside through the exported follow* functions
        // rather than written here directly.
        mode: FOLLOW_NONE,
        targetPositionId: null,
        // "Follow a member" picked while there was nobody to follow: the next
        // member whose card opens becomes the one followed.
        awaitingTarget: false,
        // When the last manual pan or zoom happened; null before any gesture.
        // Read through isPaused rather than compared directly, because the
        // window it opens is a duration rather than a single moment.
        pausedAt: null,
        // The timer that moves the map once the pause has worn off.
        pauseEnds: null,
        // The rectangle "follow everybody" last fitted the map to, so that a
        // rectangle barely different from it does not nudge the view again.
        followedBounds: null,
        // Asked to name a device or a person, from the card: `(kind, key)`,
        // where kind is `devices` or `members`. Set by the page; a card with
        // nobody to ask offers no names.
        onName: null,
        ownMemberId: null,
        personName: null,
        // What the card's ages are as of while the database is silent, empty
        // while it answers. Set by the page, which judges the link.
        knownNote: null,
        // The round buttons for a device's card and the line they leave:
        // `(position) => { buttons, status } | null`. Set by the page.
        deviceActions: null,
    };
}

/** What the map's own events change: the ring, the grouping, the pause and the card. */
function listenToTheMap(view) {
    const map = view.map;

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
            pauseFollowing(view);
        }
    });

    closeOnATapOrEscape(view);
}

/** The two ways of putting the card away that are not its own cross. */
function closeOnATapOrEscape(view) {
    // A tap away from everybody means the card is no longer being looked at,
    // which is how the device reads the same tap. A tap on a marker is caught
    // first, in addMarker, and stopped there from bubbling — without that,
    // this handler would close the card the very tap that opened it was meant
    // to leave open.
    view.map.on('click', () => {
        if (closeCards(view)) {
            view.onCardChange();
            redrawSelection(view);
        }
    });

    // The way a dialog is dismissed everywhere else: the card covers the map,
    // and reaching for the small cross is the long way round. Listened for on
    // the document rather than on the map element, because the card takes the
    // focus when it opens and the map no longer has it.
    // A window or a menu opened over the card is closed first, one press
    // each (see `page/menus.js`), and the card waits for the next one.
    document.addEventListener('keydown', (event) => {
        if (
            event.key !== 'Escape' ||
            event.defaultPrevented ||
            document.querySelector('dialog[open]')
        ) {
            return;
        }

        if (closeCards(view)) {
            view.onCardChange();
            redrawSelection(view);
        }
    });
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

function draw(view, { positions, names, faces, now }) {
    const placed = positions.map((position) => {
        const point = view.map.project([position.longitude, position.latitude]);

        return { id: position.positionId, x: point.x, y: point.y };
    });

    const byId = new Map(positions.map((position) => [position.positionId, position]));
    const seen = new Set();

    const alone = view.selection ? view.selection.positionId : null;

    for (const cluster of clusterMembers(placed, CLUSTER_RADIUS, alone)) {
        const key = placeMarker(view, cluster, byId, alone, { names, faces, now });

        if (key !== null) {
            seen.add(key);
        }
    }

    for (const [key, entry] of view.markers) {
        if (!seen.has(key)) {
            entry.marker.remove();
            view.markers.delete(key);
        }
    }

    const closed = followTheCard(view, seen, { names, faces, now });

    // After the markers that went away have gone: a ring is drawn around a
    // marker whose card is open, and a marker taken off the map has none.
    drawWidth(view);

    return closed;
}

/**
 * Puts one marker where the members of [cluster] stand, made or reused, and
 * says which marker it was; null when none of them has a position any more.
 */
function placeMarker(view, cluster, byId, alone, { names, faces, now }) {
    const group = cluster.map((memberId) => byId.get(memberId)).filter(Boolean);

    if (group.length === 0) {
        return null;
    }

    // The positions in it are what a marker is: a group that gains or
    // loses one is a different marker, with a different number of faces.
    const key = cluster.join(',');

    const entry = view.markers.get(key) ?? addMarker(view, key, group);

    entry.members = group;

    const at = [centreLongitude(group), centreLatitude(group)];

    entry.marker.setLngLat(at);

    // Restated on every draw rather than once: the pile is an order of
    // what is on screen, and panning the map changes which member is in
    // front of which without changing anything about the members.
    //
    // The member whose card is open stands over everybody: they are on a
    // pin of their own (see clusterMembers), and another member in the
    // same spot would otherwise cover the pin the circle is drawn for.
    const looked = alone !== null && cluster.length === 1 && cluster[0] === alone;

    entry.marker.getElement().style.zIndex = looked
        ? HIGHEST
        : markerOrder(view.map.project(at).y);

    showPin(entry, names, faces, now);

    // Framed in the member's colour, the colour of their circle: their pin
    // stands apart and on top, and its label lying over a neighbour's
    // read as one line with it.
    entry.label.classList.toggle('open', looked);

    if (looked) {
        entry.label.style.setProperty('--frame', memberColour(group[0].memberId));
    }

    // What the card holds is built for the card that is open, and for no
    // other: the card is a heading, a face and five rows, and building
    // one per marker on every tick is work nobody sees.
    entry.latest = { names, faces, now };

    if (view.selection && view.selection.key === key) {
        showCard(view, names, faces, now);
    }

    return key;
}

/**
 * Moves the open card to whichever marker now covers its member, or closes
 * it when the member has left the positions; says whether it closed.
 *
 * The marker a card was about can vanish between two drawings: coming closer
 * takes a group apart into a marker each, and going away puts them back
 * together. The card is about a member rather than about a pile of pins.
 *
 * A card over a list follows the first member of the pile it was opened
 * over: the member whose card was open a moment ago rejoins the nearest pile
 * when that card is left (see clusterMembers), and a list closing under the
 * hand that just opened it would read as a misfire.
 *
 * Whether it closed is the answer rather than a call to onCardChange from
 * inside a drawing: a drawing is usually something the panel asked for, and
 * showMembers reads the selection again the moment this returns.
 */
function followTheCard(view, seen, { names, faces, now }) {
    if (!view.selection || seen.has(view.selection.key)) {
        return false;
    }

    const moved = markerKeyFor(
        view,
        view.selection.positionId ?? view.selection.key.split(',')[0],
    );

    if (moved === null) {
        closeCards(view);

        return true;
    }

    view.selection = openMember(moved, view.selection.positionId, view.selection.list ?? null);
    showCard(view, names, faces, now);

    return false;
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
export function drawWidth(view) {
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
 * Draws the markers and the circle again after the card changed hands.
 *
 * The markers as well as the circle: the member whose card is open stands on
 * a pin of their own (see clusterMembers), so opening or closing a card can
 * take a marker apart or put one back together.
 */
export function redrawSelection(view) {
    if (!view.last) {
        drawWidth(view);

        return;
    }

    if (draw(view, view.last)) {
        view.onCardChange();
    }
}

/**
 * Whose card is open, where exactly one member's is.
 *
 * A list of who stands under one marker is not one of them: which member the
 * viewer means is the question that list is asking, and a circle drawn for any
 * one of them would answer it for them.
 */
export function selectedPosition(view) {
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
    const { element, label, pin, faces, soughtMark } = buildPin(group.length);

    // Held before the click handler, which reads it back on every tap rather
    // than closing over the group this marker happened to have when it was
    // made — the members under one key never change, but which of them the
    // card is about does.
    const entry = {
        marker: null,
        pin,
        faces,
        label,
        element,
        soughtMark,
        members: group,
        // What the last drawing knew, kept for the moment the card is opened
        // between two drawings.
        latest: null,
    };

    // A tap on the marker is a choice about the card, and it must not reach
    // the map underneath: the map's own click handler closes whatever card is
    // open, and left alone it would close this same tap's card the instant it
    // opened.
    element.addEventListener('click', (event) => tapMarker(view, key, entry, event));

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

/** The elements of a marker holding [count] members: a label, a pin and a face for each. */
function buildPin(count) {
    const element = document.createElement('div');
    element.className = 'member-marker';

    // The label is always up, the way a place name is on a map: a pin on its
    // own says somebody is there, not who.
    const label = document.createElement('span');
    label.className = 'member-label';

    // A pin rather than a dot, and a face inside it: the point at the bottom
    // is the position, so the face can be this large without covering the
    // place the member actually is. The same shape the device draws.
    const pin = document.createElement('span');
    pin.className = 'member-pin';

    const body = document.createElement('span');
    body.className = 'pin-body';

    const point = document.createElement('span');
    point.className = 'pin-point';

    pin.append(body, point);

    const circles = circleCount(count);

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

    // The word over a member the group is looking for, above their name: a
    // red capsule found before any name is read. Kept in every marker and
    // shown only while a search stands.
    const soughtMark = document.createElement('span');
    soughtMark.className = 'sought-mark';
    soughtMark.hidden = true;

    element.append(soughtMark, label, pin);

    return { element, label, pin, faces, soughtMark };
}

/** What a tap on a marker does to the card, which it opens, moves or closes. */
function tapMarker(view, key, entry, event) {
    event.stopPropagation();

    const positionIds = entry.members.map((member) => member.positionId);

    // Worked out before it is stored, because closeCards is what takes the card
    // off the screen and it does nothing at all once the selection is already
    // empty: writing the answer down first would leave the card standing there
    // with nothing selected behind it.
    const tapped = markerTapped(view.selection, key, positionIds);

    if (tapped === null) {
        closeCards(view);
    } else {
        view.selection = tapped;

        // A marker for one member opens straight onto their card, so tapping it
        // is as much a choice of who to follow as tapping their entry in the
        // strip. A marker for several opens onto the list instead, and no one
        // of them is meant until a row in it is tapped — that click sets the
        // target itself, in groupList.
        if (view.selection.positionId !== null) {
            setFollowTarget(view, view.selection.positionId);
        }

        // Filled on the way open rather than kept filled: until it is asked
        // for, an empty card costs nothing. entry.latest is set on every
        // drawing, and a marker cannot be tapped before its first one.
        if (entry.latest) {
            showCard(view, entry.latest.names, entry.latest.faces, entry.latest.now);
        }
    }

    view.onCardChange();
    redrawSelection(view);
    bringTheRingIntoSight(view);
}

/** How many circles a pin has: one per face, and one more for the count if any are left out. */
function circleCount(count) {
    const shown = facesShown(count);

    return count > shown ? shown + 1 : shown;
}

/** What the marker looks like: its colour, its label and the faces in it. */
function showPin(entry, names, faces, now) {
    const group = entry.members;
    const sought = group.some((member) => member.sought);

    // A marker holding somebody looked for is ringed whoever else stands
    // under it: the ring is what the rest of the group is scanning the map
    // for.
    entry.element.classList.toggle('sought', sought);
    entry.soughtMark.hidden = !sought;
    entry.soughtMark.textContent = WORDS.searchMark;

    if (group.length === 1) {
        const member = group[0];
        const name = nameOf(member, names);
        const stale = isGrey(member, now);
        const colour = pinColour(member.memberId, stale);

        entry.label.textContent = name;
        entry.pin.style.setProperty('--pin', colour);
        entry.pin.classList.toggle('stale', stale);
        showFace(entry.faces[0], faces.get(member.memberId), name, colour);

        return;
    }

    // Grey only when everybody under the marker has gone quiet: one member
    // still reporting is a reason to trust the marker.
    const stale = group.every((member) => isGrey(member, now));

    entry.label.textContent = groupLabel(group.length);
    entry.pin.style.setProperty('--pin', stale ? STALE_COLOUR : GROUP_COLOUR);
    entry.pin.classList.toggle('stale', stale);

    const shown = group.slice(0, facesShown(group.length));

    shown.forEach((member, index) => {
        const name = nameOf(member, names);
        const old = isGrey(member, now);
        const face = entry.faces[index];

        const colour = pinColour(member.memberId, old);

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
 * Opens the map on the viewer's own freshest position once one arrives, or on
 * the first position of anyone when the viewer has none.
 *
 * After that the viewer is in charge of what they are looking at, and a list
 * that updates every few seconds must not drag the view back.
 */
export function centreOnce(view, positions, ownMemberId) {
    if (view.centred) {
        return;
    }

    const start = freshestOf(positions, ownMemberId) ?? positions[0];

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
    const crowd = view.markers.get(key).members.length > 1 ? key : null;

    view.selection = openMember(key, position.positionId, crowd);

    if (view.last) {
        showCard(view, view.last.names, view.last.faces, view.last.now);
    }

    // Said out loud rather than left to the next drawing: a card already open
    // over a group changes who it is about without opening again, and the
    // strip would go on marking the member the viewer has just navigated away
    // from until the next redraw — half a minute, on the clock that moves the
    // ages along.
    view.onCardChange();
    redrawSelection(view);
}

/**
 * How close the map has to come for the circle of doubt to be worth drawing.
 *
 * The circle is drawn in metres, and a browser window is three times as wide
 * as a device: at the zoom both clients start at, a hundred and sixty metres of
 * doubt comes out thirty-three pixels across on a laptop, which is the size of
 * the pin standing on top of it — the ring is there and cannot be seen. On a
 * device the same ring covers a good part of a narrow screen, which is why the
 * Android map needs nothing of the sort.
 *
 * The answer is the zoom at which the circle spans [RING_SHARE_OF_THE_VIEW] of
 * the shorter side of the window, held back at [CLOSEST_ZOOM] so that a sharp
 * answer does not drop the viewer onto a single roof. Only ever used to come
 * closer: whoever zoomed out did it on purpose.
 */
export function closeEnoughForTheRing(view, position) {
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
