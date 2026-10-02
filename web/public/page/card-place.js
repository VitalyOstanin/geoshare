// Where the card stands: over the entry of the strip it is about.

import { elements } from './state.js';

/** How close to the edge of the panel the card may come, in pixels. */
const CARD_MARGIN_PX = 8;

/** How far the arrow keeps from the card's rounded corners, in pixels. */
const ARROW_INSET_PX = 16;

// The card's size changes with what it holds — a list, a member, a row that
// wraps — and a card that grew in place would stop being centred over its
// entry.
new ResizeObserver(placeCard).observe(elements.card);

// The strip scrolls inside itself and the window changes width, and either
// moves the entry out from under a card that stays put.
elements.members.addEventListener('scroll', placeCard, { passive: true });
window.addEventListener('resize', placeCard);

/**
 * Puts the open card over the entry of the member it is about, whichever way
 * it was opened: the highlighted entry and the card read as one thing only
 * when they stand together.
 *
 * A card over a list stands over the first of its members in the strip. At
 * the edge of the panel the card is pushed back inside, and the arrow goes on
 * pointing at the entry.
 */
export function placeCard() {
    const card = elements.card;

    if (card.hidden) {
        return;
    }

    const entry = elements.members.querySelector('.member.shown');

    // Measured with the left edge pinned and the right one let go: the width
    // the card takes then is its own, not what is left of the panel.
    card.style.right = 'auto';
    card.style.left = `${CARD_MARGIN_PX}px`;

    if (!entry) {
        card.classList.remove('pointing');

        return;
    }

    const panel = elements.panel.getBoundingClientRect();
    const box = entry.getBoundingClientRect();
    const width = card.offsetWidth;
    const centre = box.left + box.width / 2 - panel.left;
    const left = Math.min(
        Math.max(centre - width / 2, CARD_MARGIN_PX),
        panel.width - width - CARD_MARGIN_PX,
    );
    const arrow = Math.min(Math.max(centre - left, ARROW_INSET_PX), width - ARROW_INSET_PX);

    card.style.left = `${left}px`;
    card.style.setProperty('--arrow-x', `${arrow}px`);
    card.classList.add('pointing');
}
