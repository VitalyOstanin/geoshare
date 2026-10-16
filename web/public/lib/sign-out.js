// Leaving the account, and what the page does when the leaving is refused.

/**
 * Signs out, taking the claim to be watching back first.
 *
 * The claim goes first because once signed out the rules refuse this page the
 * delete, and every device would stay in its fifteen-second mode until the
 * claim went stale. The withdrawal settles either way, so a refused one does
 * not keep the account signed in.
 *
 * A refused sign-out leaves the session alive, so the page must not be left as
 * if it had begun to leave: `resume` makes the claim again, and the refusal is
 * handed back to the caller to be said, not thrown into the void of an event
 * handler.
 *
 * @param {object} parts
 * @param {Function} parts.stopWatching Withdraws the claim, or null when none is held.
 * @param {Function} parts.leave Signs out; its promise is rejected when that fails.
 * @param {Function} parts.resume Makes the claim again after a refused sign-out.
 * @returns {Promise<Error|null>} The refusal, or null when the account was left.
 */
export async function signOutOf({ stopWatching, leave, resume }) {
    await stopWatching?.();

    try {
        await leave();
    } catch (error) {
        resume();

        return error ?? new Error('sign-out refused');
    }

    return null;
}
