// Ringing a phone: its button, the window that asks, and the calls followed while shown.

import { ageText, positionAge } from '../lib/age.js';
import { shortName } from '../lib/members.js';
import { sirenState } from '../lib/sirens.js';
import { WORDS } from '../lib/words.js';
import { callSiren, watchSiren } from '../lib/store.js';
import { elements, state } from './state.js';
import { linkIsLost, troubleOf } from './link-ui.js';
import { drawGroup } from './group-panel.js';

/** The loudspeaker on the button that rings a phone, and the one struck through that stops it. */
export const SPEAKER_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/><path d="M19.5 5.5a9 9 0 0 1 0 13"/></svg>';
export const ALARM_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true"><path d="M6 5v9M12 5v9M18 5v9"/><circle cx="6" cy="19" r="1.3" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="1.3" fill="currentColor" stroke="none"/><circle cx="18" cy="19" r="1.3" fill="currentColor" stroke="none"/></svg>';
export const BIN_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/></svg>';
export const MUTE_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M17 9l5 6M22 9l-5 6"/></svg>';

/**
 * What a row says of the call this page made: which phone, the seconds left
 * while it sounds, or that it never answered. Nothing once it is over.
 */
export function sirenText(siren, now, phones) {
    const phone = phones.find((one) => one.phoneId === siren.phoneId)?.name
        ?? shortName(siren.phoneId.split('.')[1] ?? siren.phoneId);

    switch (now.kind) {
        case 'waiting':
            return WORDS.sirenWaiting(phone);
        case 'sounding':
            return WORDS.sirenSounding(
                phone,
                Math.max(0, Math.ceil((now.until - Date.now()) / 1000)),
            );
        case 'noAnswer':
            return WORDS.sirenNoAnswer(phone);
        case 'stopped':
            return WORDS.sirenStopped(phone);
        default:
            return null;
    }
}

/**
 * Follows the siren of every phone drawn with a siren button and lets go of
 * the rest, and redraws every second while any of them is being called or
 * sounding, for the countdown.
 */
export function followSirens() {
    for (const [phoneId, stop] of state.sirenWatches) {
        if (!state.sirensShown.has(phoneId)) {
            stop();
            state.sirenWatches.delete(phoneId);
            state.sirens.delete(phoneId);
        }
    }

    for (const phoneId of state.sirensShown) {
        if (!state.sirenWatches.has(phoneId)) {
            state.sirenWatches.set(
                phoneId,
                watchSiren(phoneId, (siren) => {
                    state.sirens.set(phoneId, siren);
                    drawGroup();
                }),
            );
        }
    }

    const now = Date.now();
    const live = [...state.sirens.values()].some((siren) => {
        const kind = siren === null ? null : sirenState(siren, now).kind;

        return kind === 'waiting' || kind === 'sounding';
    });

    if (live && state.sirenTick === null) {
        state.sirenTick = setInterval(drawGroup, 1000);
    } else if (!live && state.sirenTick !== null) {
        clearInterval(state.sirenTick);
        state.sirenTick = null;
    }
}

export function askSiren(line) {
    const last = [...line.phones]
        .sort((one, other) => (other.onlineAt ?? 0) - (one.onlineAt ?? 0))[0];

    state.sirenAsk = {
        name: line.displayName ?? shortName(line.memberId),
        phones: line.phones,
        // The phone last in touch is the likeliest to hear it.
        selected: last.phoneId,
    };
    drawSirenAsk();

    if (!elements.siren.open) {
        elements.siren.showModal();
    }
}

export function drawSirenAsk() {
    const ask = state.sirenAsk;

    if (!ask) {
        return;
    }

    const now = Date.now();

    elements.sirenTitle.textContent = WORDS.sirenTitle(ask.name);
    elements.sirenPhones.hidden = ask.phones.length < 2;
    elements.sirenPhones.replaceChildren(
        ...ask.phones.map((phone) => {
            const button = document.createElement('button');

            button.type = 'button';
            button.textContent = phone.onlineAt === null
                ? phone.name
                : WORDS.sirenPhoneChip(phone.name, ageText(positionAge(phone.onlineAt, now)));
            button.setAttribute('aria-pressed', String(phone.phoneId === ask.selected));
            button.addEventListener('click', () => {
                ask.selected = phone.phoneId;
                drawSirenAsk();
            });

            return button;
        }),
    );
    elements.sirenNote.textContent = WORDS.sirenNote;
    // Asked again whenever the link changes, so a window left open follows it.
    elements.sirenGo.disabled = linkIsLost() || ask.sending === true;
    elements.sirenError.textContent = linkIsLost() ? WORDS.linkLostAction : ask.trouble ?? '';
    elements.sirenError.hidden = elements.sirenError.textContent === '';
    elements.sirenGo.textContent = WORDS.sirenGo;
    elements.sirenCancel.textContent = WORDS.searchCancel;
}

/**
 * Rings the phone picked and waits for the database: a refusal stays on the
 * window rather than closing it on a call that never went out.
 */
elements.sirenGo.addEventListener('click', async () => {
    const ask = state.sirenAsk;

    if (!ask) {
        return;
    }

    if (linkIsLost()) {
        return;
    }

    ask.sending = true;
    ask.trouble = null;
    drawSirenAsk();

    try {
        await callSiren(ask.selected, state.member.memberId);

        if (state.sirenAsk === ask) {
            elements.siren.close();
        }
    } catch (error) {
        ask.sending = error?.code === 'unanswered';
        ask.trouble = troubleOf(error);
        drawSirenAsk();
    }
});
elements.sirenCancel.addEventListener('click', () => elements.siren.close());
elements.siren.addEventListener('close', () => {
    state.sirenAsk = null;
});
