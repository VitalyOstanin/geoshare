// What the page refuses to start with in firebase-config.js. The file is
// written by tools/web-config.sh and by the setup page, so a fault here is a
// file edited by hand or left half-written; said at once, it names the field,
// where the Firebase library would fail later with an error of its own.

/** The fields the page cannot reach its project without. */
export const REQUIRED_FIELDS = ['projectId', 'appId', 'apiKey', 'authDomain'];

/**
 * What is wrong with [config], or null when nothing is.
 *
 * Emulators are named only beside a project id starting with `demo-`: a real
 * project id with emulators beside it is a configuration mixed up somewhere,
 * and refusing it is better than writing to either.
 */
export function configProblem(config) {
    const missing = REQUIRED_FIELDS.filter((field) => !config?.[field]);

    if (missing.length > 0) {
        return `firebase-config.js has no ${missing.join(', ')}; tools/web-config.sh writes it`;
    }

    if (config.emulators && !config.projectId.startsWith('demo-')) {
        return `emulators are only for a demo- project, not ${config.projectId}`;
    }

    return null;
}
