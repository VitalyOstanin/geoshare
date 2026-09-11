// The map itself: an OpenStreetMap background with one marker per member.
//
// The same background the Android screen draws, so that a position looks the
// same wherever it is looked at.

import { STALE_COLOUR, memberColour } from './members.js';
import { isStale } from './age.js';

/** Close enough to tell which street somebody is on, without hunting for them. */
const START_ZOOM = 15;

const TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const ATTRIBUTION = '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

export function createMap(element) {
    const map = L.map(element, { zoomControl: true }).setView([0, 0], 2);

    L.tileLayer(TILES, { maxZoom: 19, attribution: ATTRIBUTION }).addTo(map);

    return { map, markers: new Map(), centred: false };
}

/**
 * Moves the markers that moved, adds the ones that appeared and removes the
 * ones that went away.
 *
 * Kept rather than redrawn, unlike the Android side: a Leaflet marker carries
 * its own element, and replacing it on every update would restart the CSS
 * transition that makes a moving member look like they are moving.
 */
export function showMembers(view, positions, now) {
    const seen = new Set();

    for (const position of positions) {
        seen.add(position.memberId);

        const colour = isStale(position.knownAt, now) ? STALE_COLOUR : memberColour(position.memberId);
        const existing = view.markers.get(position.memberId);

        if (existing) {
            existing.setLatLng([position.latitude, position.longitude]);
            existing.setStyle({ fillColor: colour });
        } else {
            view.markers.set(
                position.memberId,
                L.circleMarker([position.latitude, position.longitude], {
                    radius: 9,
                    // The white ring is what keeps the dot visible over a dark
                    // road or a green park.
                    color: '#ffffff',
                    weight: 3,
                    fillColor: colour,
                    fillOpacity: 1,
                }).addTo(view.map),
            );
        }
    }

    for (const [memberId, marker] of view.markers) {
        if (!seen.has(memberId)) {
            marker.remove();
            view.markers.delete(memberId);
        }
    }
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
    view.map.setView([start.latitude, start.longitude], START_ZOOM);
}

export function lookAt(view, position) {
    // Flown to rather than set, because the viewer asked to be taken
    // somewhere and the way there says how far it was.
    view.map.flyTo([position.latitude, position.longitude], Math.max(view.map.getZoom(), START_ZOOM));
}
