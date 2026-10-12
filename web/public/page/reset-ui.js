// Asking a device to forget its place: its button, the line it leaves on the
// row, and the requests followed while shown.

import { resetState, resetTold } from '../lib/resets.js';
import { SPEAKING, WORDS } from '../lib/words.js';
import { askReset, watchReset } from '../lib/store-search.js';
import { state } from './state.js';
import { askConfirm } from './dialogs.js';
import { drawGroup } from './group-panel.js';

/** A map marker with a turning arrow inside it. */
export const RESET_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21s-6-5.6-6-10.5a6 6 0 0 1 12 0c0 4.9-6 10.5-6 10.5z"/><path d="M9.8 10.5a2.2 2.2 0 1 0 2.2-2.2"/><path d="M12 6.6v1.7h1.7"/></svg>';

/** The time of a request, or of its answer: a row tells of one only for a day. */
function clockText(at) {
    return new Date(at).toLocaleTimeString(SPEAKING, {
        hour: '2-digit',
        minute: '2-digit',
    });
}

/** What a row says of a request, or null once it is old. */
export function resetText(reset, now) {
    if (!resetTold(reset, now)) {
        return null;
    }

    const told = resetState(reset, now);

    switch (told.kind) {
        case 'waiting':
            return WORDS.resetWaiting(clockText(reset.askedAt));
        case 'done':
            return WORDS.resetDone(clockText(told.at));
        default:
            return WORDS.resetNoAnswer;
    }
}

/**
 * The line a request to [device] leaves on [about], and the round button that
 * makes one. Plain rather than red: nothing on the device sounds, and whoever
 * holds it is only told once it is done.
 */
export function resetButtonFor(phone, now, about) {
    const reset = state.resets.get(phone.phoneId) ?? null;
    const told = reset === null ? null : resetText(reset, now);

    if (told !== null) {
        const status = document.createElement('span');

        status.className = 'admin-about reset-status';
        status.textContent = told;
        about.append(status);
    }

    const button = document.createElement('button');

    button.type = 'button';
    button.className = 'place-reset';
    button.title = WORDS.resetHint;
    button.setAttribute('aria-label', WORDS.resetHint);
    button.innerHTML = RESET_ICON;
    button.addEventListener('click', () => {
        askConfirm(
            { title: WORDS.resetTitle(phone.name), note: WORDS.resetNote, go: WORDS.resetGo },
            () => askReset(phone.phoneId, state.member.memberId),
        );
    });

    return button;
}

/** Follows the request of every device drawn with a reset button and lets go of the rest. */
export function followResets() {
    for (const [phoneId, stop] of state.resetWatches) {
        if (!state.sirensShown.has(phoneId)) {
            stop();
            state.resetWatches.delete(phoneId);
            state.resets.delete(phoneId);
        }
    }

    for (const phoneId of state.sirensShown) {
        if (!state.resetWatches.has(phoneId)) {
            state.resetWatches.set(
                phoneId,
                watchReset(phoneId, (reset) => {
                    state.resets.set(phoneId, reset);
                    drawGroup();
                }),
            );
        }
    }
}
