import { ATTENTION, IDLE_AFTER_MS, pauseDatabase, resumeDatabase } from '../lib/store.js';
import { pauseLink, resumeLink } from './link-ui.js';
import { state } from './state.js';

/** How often a tab left alone is asked whether it has been left long enough. */
const PAUSE_CHECK_MS = 30 * 1000;

/**
 * Stops the page listening once nobody has looked at it for five minutes, and
 * starts it again at the first sign of somebody back.
 *
 * A map left open all day — on another desktop, or by somebody who walked away
 * — still reads every write of the group, one read each. Paused, it reads
 * nothing, and says so over the map: the markers it shows stop moving.
 *
 * Looking is a visible tab and a move, a key or a focus within the last five
 * minutes; a window that is merely on screen with nobody at it is not.
 */
export function followAttention() {
    if (state.stopAttention) {
        return;
    }

    let attended = Date.now();

    const noticed = () => {
        if (document.visibilityState === 'hidden') {
            return;
        }

        attended = Date.now();

        if (state.pausedAt !== null) {
            state.pausedAt = null;
            resumeDatabase();
            resumeLink();
        }
    };
    const review = () => {
        if (state.pausedAt === null && Date.now() - attended >= IDLE_AFTER_MS) {
            state.pausedAt = Date.now();
            pauseDatabase();
            pauseLink();
        }
    };
    const timer = setInterval(review, PAUSE_CHECK_MS);

    ATTENTION.forEach((event) => window.addEventListener(event, noticed, { passive: true }));
    window.addEventListener('focus', noticed);
    document.addEventListener('visibilitychange', noticed);

    state.stopAttention = () => {
        clearInterval(timer);
        ATTENTION.forEach((event) => window.removeEventListener(event, noticed));
        window.removeEventListener('focus', noticed);
        document.removeEventListener('visibilitychange', noticed);

        // Signing out is no reason to leave the next account deaf.
        if (state.pausedAt !== null) {
            state.pausedAt = null;
            resumeDatabase();
        }
    };
}

export function unfollowAttention() {
    state.stopAttention?.();
    state.stopAttention = null;
}
