/**
 * Hands the setup page's access token back to Google and forgets it.
 *
 * The token reaches every project of the account, so it must not outlive the
 * steps that use it. [state] is cleared before Google answers: nothing on the
 * page may reach for the token while the revoke is in flight. A refusal is only
 * logged through [warn]: the token expires on its own, and the person setting
 * up the group can do nothing about it.
 *
 * Does nothing when no token is held, so it can be called from every exit.
 */
export function releaseToken(state, oauth2, warn) {
    const token = state.token;

    if (!token) {
        return;
    }

    state.token = null;
    state.api = null;

    try {
        oauth2.revoke(token, (response) => {
            if (response?.error) {
                warn('Revoking the access token failed:', response.error);
            }
        });
    } catch (error) {
        warn('Revoking the access token failed:', error);
    }
}
