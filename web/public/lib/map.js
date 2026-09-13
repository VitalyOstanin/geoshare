// The map itself: an OpenStreetMap background with one marker per member.
//
// The same background the Android screen draws, so that a position looks the
// same wherever it is looked at.

// Named exports only: the module has no default, and the parts used here are
// the map, a marker, its bubble and the zoom buttons.
import {
    Map as MapLibre,
    Marker,
    NavigationControl,
    Popup,
} from 'https://cdn.jsdelivr.net/npm/maplibre-gl@6.9.0/dist/maplibre-gl.mjs';

import { STALE_COLOUR, memberColour, shortName } from './members.js';
import { showFace } from './faces.js';

import { ageText, isStale, positionAge, timeText } from './age.js';
import { clusterMembers, facesShown } from './clusters.js';
import { openModeHelp } from './mode-help.js';
import { modeText } from './modes.js';
import { WORDS } from './words.js';

/** Close enough to tell which street somebody is on, without hunting for them. */
const START_ZOOM = 15;

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
    },
    layers: [{ id: 'osm', type: 'raster', source: 'osm' }],
};

/**
 * @param element where the map is drawn
 * @param onCardChange called whenever a card opens, closes or changes who it
 *        is about, so that the panel can mark the card being looked at
 */
export function createMap(element, onCardChange = () => {}) {
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

    const view = { map, markers: new Map(), centred: false, last: null, onCardChange };

    // Which members cover each other is a question about the screen, so the
    // answer changes with every zoom: a group that was one pin has to come
    // apart once the map is close enough to tell them apart. Redrawn when the
    // map stops rather than while it moves, because regrouping under a moving
    // finger reads as flicker.
    map.on('moveend', () => {
        if (view.last) {
            draw(view, view.last);
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

        closeCards(view);
    });

    return view;
}

/** Closes whatever card is open, and says whether there was one. */
function closeCards(view) {
    let closed = false;

    for (const entry of view.markers.values()) {
        if (entry.popup.isOpen()) {
            entry.popup.remove();
            closed = true;
        }
    }

    return closed;
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

    draw(view, view.last);
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
        entry.marker.setLngLat([centreLongitude(group), centreLatitude(group)]);

        showPin(entry, names, faces, now);

        // What the bubble holds is built for the bubble that is open, and for
        // no other: the card is a heading, a face and five rows, and building
        // one per marker on every tick is work nobody sees.
        entry.latest = { names, faces, now };

        if (entry.popup.isOpen()) {
            showPopup(entry, names, faces, now);
        }
    }

    for (const [key, entry] of view.markers) {
        if (!seen.has(key)) {
            entry.marker.remove();
            view.markers.delete(key);
        }
    }
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

    // Closing on a click elsewhere on the map is the library's own default,
    // and it is what the phone does too: a tap away from everybody means the
    // card is no longer being looked at.
    // The width is left to the card rather than kept at the library's 240
    // pixels: the rows are a label and a value on one line, and a value wider
    // than that ran out over the edge of the bubble instead of widening it.
    // The stylesheet is what stops it growing past the window.
    const popup = new Popup({ offset: 16, closeButton: true, maxWidth: 'none' });
    // Held before the handlers, which reach into it, and before the marker,
    // which is added to it once it is on the map.
    const entry = {
        marker: null,
        popup,
        pin,
        faces,
        label,
        members: group,
        openedPositionId: null,
        // What the last drawing knew, kept for the moment the bubble is opened
        // between two drawings.
        latest: null,
        // Carried on the entry rather than read off the view, because the rows
        // of a group list are built with the entry alone in hand.
        onCardChange: view.onCardChange,
    };

    // One bubble at a time, the way a map shows one place at a time: the
    // library leaves every popup it opens open, and a second tap would
    // otherwise leave the first member's card hanging over the map.
    popup.on('open', () => {
        closeOthers(view, key);

        // Filled on the way open rather than kept filled: until it is asked
        // for, an empty bubble costs nothing.
        if (entry.latest) {
            showPopup(entry, entry.latest.names, entry.latest.faces, entry.latest.now);
        }

        entry.onCardChange();
    });
    // The panel marks whoever the open card is about, and a card closed by the
    // cross, by Escape or by a tap on the map leaves nobody marked.
    popup.on('close', () => {
        entry.openedPositionId = null;
        entry.onCardChange();
    });

    // Placed before it is added: a marker with nowhere to be cannot be put on
    // a map, and the caller sets the position again on every update anyway.
    // Hung by its bottom edge, which is the point of the pin: the member is
    // where the point is, and everything else stands above them.
    entry.marker = new Marker({ element, anchor: 'bottom' })
        .setLngLat([centreLongitude(group), centreLatitude(group)])
        .setPopup(popup)
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
 * What the bubble holds: one member's card, or the list of who is standing
 * under a marker that covers several of them.
 *
 * Which of the group was meant is a question only the viewer can answer, so
 * the list comes first and the card is one click further in. The choice lives
 * on the marker rather than in the bubble, so that a tick that redraws the
 * bubble does not throw the viewer back to the list.
 */
function showPopup(entry, names, faces, now) {
    const group = entry.members;

    if (group.length === 1) {
        entry.popup.setDOMContent(detail(group[0], nameOf(group[0], names), faces, now));

        return;
    }

    const chosen = group.find((member) => member.positionId === entry.openedPositionId);

    if (!chosen) {
        entry.popup.setDOMContent(groupList(entry, names, faces, now));

        return;
    }

    const card = detail(chosen, nameOf(chosen, names), faces, now);
    const back = document.createElement('button');

    back.type = 'button';
    back.className = 'group-back';
    // The arrow says which way the button goes before the words are read; an
    // arrow written as a character rather than drawn, because a set of icons
    // is a dependency of its own and this is the only one the page needs.
    back.textContent = WORDS.backToList;
    back.addEventListener('click', () => {
        entry.openedPositionId = null;
        showPopup(entry, names, faces, now);
    });

    card.prepend(back);
    entry.popup.setDOMContent(card);
}

/**
 * Whose cards are open, as position ids.
 *
 * A card about one member names that member; a list of who stands under one
 * marker names all of them, because the viewer is looking at the group.
 */
export function shownPositions(view) {
    for (const entry of view.markers.values()) {
        if (!entry.popup.isOpen()) {
            continue;
        }

        return new Set(
            entry.openedPositionId === null
                ? entry.members.map((member) => member.positionId)
                : [entry.openedPositionId],
        );
    }

    return new Set();
}

/** The members under one marker, as rows that open their cards. */
function groupList(entry, names, faces, now) {
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
        age.textContent = ageText(positionAge(member.knownAt, now));

        row.append(face, name, age);
        row.addEventListener('click', () => {
            entry.openedPositionId = member.positionId;
            showPopup(entry, names, faces, now);
            entry.onCardChange();
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

function closeOthers(view, key) {
    for (const [other, entry] of view.markers) {
        if (other !== key && entry.popup.isOpen()) {
            entry.popup.remove();
        }
    }
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
function detail(position, name, faces, now) {
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

    title.textContent = name;
    titles.append(title);

    // Only when the name does not carry it already: a member with one phone
    // has no use for its model, and a member with two has it in their name.
    if (position.deviceName && !name.includes(position.deviceName)) {
        const device = document.createElement('span');

        device.className = 'detail-device';
        device.textContent = position.deviceName;
        titles.append(device);
    }

    heading.append(face, titles);

    const table = document.createElement('table');

    const rows = [
        [WORDS.lastSeen, ageText(positionAge(position.knownAt, now))],
        [WORDS.at, timeText(position.knownAt)],
        [WORDS.mode, modeButton(position.mode)],
        [WORDS.battery, batteryText(position)],
        [WORDS.position, `${position.latitude.toFixed(5)}, ${position.longitude.toFixed(5)}`],
    ];

    for (const [title, value] of rows) {
        const row = document.createElement('tr');
        const head = document.createElement('th');
        const cell = document.createElement('td');

        head.textContent = title;

        // A value is either text or a piece of the page: the charge carries a
        // bolt of its own colour, everything else is a line of characters.
        if (value instanceof Node) {
            cell.append(value);
        } else {
            cell.textContent = value;
        }

        row.append(head, cell);
        table.append(row);
    }

    card.append(heading, table);

    return card;
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
    // Flown to rather than jumped to, because the viewer asked to be taken
    // somewhere and the way there says how far it was.
    view.map.flyTo({
        center: [position.longitude, position.latitude],
        zoom: Math.max(view.map.getZoom(), START_ZOOM),
    });

    const entry = [...view.markers.values()].find((marker) =>
        marker.members.some((member) => member.positionId === position.positionId));

    if (!entry) {
        return;
    }

    // A member asked for by name opens their own card even when the marker
    // stands for several: the question was about them, not about the place.
    entry.openedPositionId = entry.members.length > 1 ? position.positionId : null;

    if (view.last) {
        showPopup(entry, view.last.names, view.last.faces, view.last.now);
    }

    if (!entry.popup.isOpen()) {
        entry.marker.togglePopup();
    }
}

/** The map measures its element when it is created, and the sign-in screen hides it. */
export function remeasure(view) {
    view.map.resize();
}
