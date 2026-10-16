// The reason shown when a name could not be written, in the page's words.

/** The key in the words for each code the database is known to refuse with. */
const REASONS = {
    'permission-denied': 'nameFailedDenied',
    unauthenticated: 'nameFailedSignedOut',
    unavailable: 'nameFailedOffline',
    'deadline-exceeded': 'nameFailedSlow',
    'resource-exhausted': 'nameFailedBusy',
};

/**
 * The sentence for a write of a name that failed. The code of the error is
 * told in words when it is one of those above; any other gets the general
 * phrase, since the raw code or the English message of the SDK means nothing
 * to the person looking at the window. The raw error stays in the console.
 */
export function nameFailureText(words, error) {
    // The SDK may write the code as 'firestore/permission-denied'.
    const code = String(error?.code ?? '').replace(/^firestore\//, '');
    const key = REASONS[code];

    if (!key) {
        console.warn('labels: unrecognised failure', error?.code ?? error?.message ?? error);
    }

    return words.nameFailed(words[key ?? 'nameFailedOther']);
}
