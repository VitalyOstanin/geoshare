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
import { ageText, isStale, positionAge, timeText } from './age.js';

/** Close enough to tell which street somebody is on, without hunting for them. */
const START_ZOOM = 15;

/**
 * A style with nothing in it but the tiles.
 *
 * The library is built for vector tiles and takes a style document rather than
 * a layer; this is that document with one raster source, which is how it draws
 * the same tiles the phones do.
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

export function createMap(element) {
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

    map.addControl(new NavigationControl({ showCompass: false }));

    return { map, markers: new Map(), centred: false };
}

/**
 * Moves the markers that moved, adds the ones that appeared and removes the
 * ones that went away.
 *
 * Kept rather than redrawn, unlike the Android side: a marker here carries its
 * own element, and replacing it on every update would close a detail bubble
 * the viewer had just opened.
 */
export function showMembers(view, positions, names, ownMemberId, now) {
    const seen = new Set();

    for (const position of positions) {
        seen.add(position.memberId);

        const own = position.memberId === ownMemberId;
        const name = names.get(position.memberId) ?? shortName(position.memberId);
        const entry = view.markers.get(position.memberId) ?? addMarker(view, position, name);

        // A member who signs in for the first time gets a name where the id
        // used to be, and the label is drawn once when the marker is made.
        entry.label.textContent = name;

        entry.marker.setLngLat([position.longitude, position.latitude]);
        entry.dot.style.background = isStale(position.knownAt, now)
            ? STALE_COLOUR
            : memberColour(position.memberId);
        // The name never changes, but the age does, and the bubble is where a
        // viewer looks to find out how much to trust the dot.
        entry.popup.setDOMContent(detail(position, name, now));
    }

    for (const [memberId, entry] of view.markers) {
        if (!seen.has(memberId)) {
            entry.marker.remove();
            view.markers.delete(memberId);
        }
    }
}

function addMarker(view, position, name) {
    const element = document.createElement('div');
    element.className = 'member-marker';

    // The label is always up, the way a place name is on a map: a dot on its
    // own says somebody is there, not who.
    const label = document.createElement('span');
    label.className = 'member-label';
    label.textContent = name;

    const dot = document.createElement('span');
    dot.className = 'member-dot';

    element.append(label, dot);

    const popup = new Popup({ offset: 16, closeButton: true });

    // One bubble at a time, the way a map shows one place at a time: the
    // library leaves every popup it opens open, and a second tap would
    // otherwise leave the first member's card hanging over the map.
    popup.on('open', () => closeOthers(view, position.memberId));
    // Placed before it is added: a marker with nowhere to be cannot be put on
    // a map, and the caller sets the position again on every update anyway.
    const marker = new Marker({ element })
        .setLngLat([position.longitude, position.latitude])
        .setPopup(popup)
        .addTo(view.map);

    const entry = { marker, popup, dot, label };

    view.markers.set(position.memberId, entry);

    return entry;
}

function closeOthers(view, memberId) {
    for (const [other, entry] of view.markers) {
        if (other !== memberId && entry.popup.isOpen()) {
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
function detail(position, name, now) {
    const card = document.createElement('div');
    card.className = 'detail';

    const heading = document.createElement('h2');
    heading.textContent = name;

    const table = document.createElement('table');

    const rows = [
        ['Last seen', ageText(positionAge(position.knownAt, now))],
        ['At', timeText(position.knownAt)],
        ['Mode', position.mode ?? 'unknown'],
        ['Battery', position.batteryPercent === null ? 'unknown' : `${position.batteryPercent}%`],
        ['Position', `${position.latitude.toFixed(5)}, ${position.longitude.toFixed(5)}`],
    ];

    for (const [title, value] of rows) {
        const row = document.createElement('tr');
        const head = document.createElement('th');
        const cell = document.createElement('td');

        head.textContent = title;
        cell.textContent = value;
        row.append(head, cell);
        table.append(row);
    }

    card.append(heading, table);

    return card;
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

    const entry = view.markers.get(position.memberId);

    if (entry && !entry.popup.isOpen()) {
        entry.marker.togglePopup();
    }
}

/** The map measures its element when it is created, and the sign-in screen hides it. */
export function remeasure(view) {
    view.map.resize();
}
