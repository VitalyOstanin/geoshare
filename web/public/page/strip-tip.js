// The window over an entry of the strip, saying in full who and what it is.

import { elements } from './state.js';

/** The gap between the window's arrow and the entry under it, in pixels. */
const TIP_GAP_PX = 10;

/** How close to the edge of the panel the window may come, in pixels. */
const TIP_MARGIN_PX = 8;

// One window for the whole strip, kept in the panel rather than in an entry:
// the strip scrolls inside itself, and a window inside an entry would be cut
// off at the strip's top edge, which is exactly where it is drawn.
const tip = document.createElement('div');
tip.id = 'strip-tip';
tip.hidden = true;
tip.setAttribute('aria-hidden', 'true');

const tipFace = document.createElement('span');
const tipText = document.createElement('span');
tipText.className = 'strip-tip-text';
const tipPerson = document.createElement('span');
tipPerson.className = 'strip-tip-person';
const tipDevice = document.createElement('span');
tipDevice.className = 'strip-tip-device';
tipText.append(tipPerson, tipDevice);
tip.append(tipFace, tipText);
elements.panel.append(tip);

// A strip scrolled under a resting pointer has moved the entry away from the
// window drawn over it.
elements.members.addEventListener('scroll', hideTip, { passive: true });

/**
 * Shows the window over [entry] while a mouse rests on it.
 *
 * A mouse only: a touch has no hover, and the window would be left standing
 * after the tap that opens the card anyway.
 *
 * Not over a member whose card is open: the card already says in full who and
 * what they are, and the window would cover the map for nothing. The click
 * that opens the card takes the window away for the same reason.
 */
export function keepTipOver(entry) {
    entry.addEventListener('pointerenter', (event) => {
        if (event.pointerType === 'mouse' && !entry.classList.contains('shown')) {
            showTip(entry);
        }
    });
    entry.addEventListener('pointerleave', hideTip);
    entry.addEventListener('click', hideTip);
}

function showTip(entry) {
    const face = entry.querySelector('.member-face');

    // The entry's own face, picture or letter and ring alike, so the window
    // reads as the same member as the pin and the entry.
    tip.firstElementChild.replaceWith(face.cloneNode(true));

    tipPerson.textContent = entry.dataset.person ?? '';
    tipDevice.textContent = entry.dataset.device ?? '';
    tipDevice.hidden = !entry.dataset.device;
    tip.hidden = false;

    const panel = elements.panel.getBoundingClientRect();
    const box = entry.getBoundingClientRect();
    const width = tip.offsetWidth;
    const centre = box.left + box.width / 2 - panel.left;
    const left = Math.min(
        Math.max(centre - width / 2, TIP_MARGIN_PX),
        panel.width - width - TIP_MARGIN_PX,
    );

    tip.style.left = `${left}px`;
    tip.style.bottom = `${panel.bottom - box.top + TIP_GAP_PX}px`;
    // The arrow points at the entry even where the window was pushed in from
    // the edge.
    tip.style.setProperty('--arrow-x', `${centre - left}px`);
}

function hideTip() {
    tip.hidden = true;
}
