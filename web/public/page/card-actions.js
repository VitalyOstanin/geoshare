// The round buttons on the card of a device: the reset of its place and the
// siren, the same two the panels carry, beside the name, with what they leave
// on the device in a line under it.

import { ADMINISTRATOR, standingIn } from '../lib/membership.js';
import { mayActOnDevice } from '../lib/card-actions.js';
import { redrawSelection, selectedPosition } from '../lib/map.js';
import { WORDS } from '../lib/words.js';
import { state } from './state.js';
import { iconsInRow, sirenButtonFor } from './group-panel.js';
import { followResets, resetButtonFor } from './reset-ui.js';
import { followSirens } from './siren-ui.js';

/** The device of a card's position, as the buttons of the panels take it. */
function phoneOf(position) {
    return { phoneId: position.positionId, name: position.deviceName || WORDS.unnamedDevice };
}

function mayActOn(position) {
    const viewer = state.member?.memberId ?? null;
    const runs = viewer !== null && standingIn(state.roster, viewer) === ADMINISTRATOR;

    return mayActOnDevice(viewer, position.memberId, runs);
}

/**
 * The buttons for the card about [position] and the line they leave, or null
 * where the viewer is not offered them. Built with the card, on every drawing.
 */
export function deviceActions(position, now = Date.now()) {
    if (!mayActOn(position)) {
        return null;
    }

    const phone = phoneOf(position);
    const status = document.createElement('div');
    const buttons = document.createElement('div');

    status.className = 'detail-status';
    buttons.className = 'admin-actions detail-actions';
    buttons.append(resetButtonFor(phone, now, status));
    buttons.append(sirenButtonFor(phone, position.memberId, now, status));
    iconsInRow(buttons);

    return { buttons, status: status.childElementCount > 0 ? status : null };
}

/** The device whose card is open and offers the buttons, or null. */
export function cardDeviceId() {
    const position = state.view ? selectedPosition(state.view) : null;

    return position !== null && mayActOn(position) ? position.positionId : null;
}

/**
 * Follows the calls and requests of the device whose card is open, which no
 * panel draws: called whenever the map is drawn, it asks for nothing once the
 * device is followed already.
 */
export function followCardDevice() {
    const phoneId = cardDeviceId();

    if (phoneId !== null && !state.sirensShown.has(phoneId)) {
        state.sirensShown.add(phoneId);
        followSirens();
        followResets();
    }
}

/** Draws the open card again, for an answer to a call or request that its buttons are showing. */
export function redrawCardDevice() {
    if (cardDeviceId() !== null) {
        redrawSelection(state.view);
    }
}
