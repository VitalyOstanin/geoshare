// What the two seeds share: the emulators' addresses, the calls made to them,
// and the documents every household starts with, whoever its people are.
//
// Written against the emulators' own REST interfaces rather than against a
// library: no credentials exist here, an owner token is whatever the emulator
// is told, and the alternative is an npm install for a handful of requests.
export const AUTH = process.env.AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
export const FIRESTORE = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080';
// Pointing the Firebase library at an emulator changes the host it calls, not
// the project it calls about, so the debug build carries this same id as its
// stand-in settings (see StandInServices.kt) rather than one read out of a
// file: seeding any other name fills a database nothing reads, and the
// screens come up empty with the rules refusing every read.
export const PROJECT = process.env.GEOSHARE_PROJECT ?? 'demo-geoshare';
export const GROUP = process.env.GEOSHARE_GROUP ?? 'family';

// The same password for all of them: the sign-in screen of a build pointed at
// the emulator offers the accounts as buttons, and nobody types this.
export const PASSWORD = 'geoshare';

// A permission without an end, written as one far enough away to read as
// never: the rules compare a timestamp, and a missing value would have to be
// a case of its own in every one of them.
export const FOREVER = '9999-12-31T00:00:00Z';

export const authUrl = (path) =>
    `http://${AUTH}/identitytoolkit.googleapis.com/v1/${path}?key=fake-api-key`;

export const documentUrl = (path) =>
    `http://${FIRESTORE}/v1/projects/${PROJECT}/databases/(default)/documents/${path}`;

export async function call(url, body) {
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
        body: JSON.stringify(body),
    });

    const answer = await response.json();

    if (!response.ok) {
        throw new Error(`${url}: ${answer.error?.message ?? response.status}`);
    }

    return answer;
}

/** A whole document, replaced rather than merged: the seed is the truth here. */
export async function write(path, fields) {
    const response = await fetch(documentUrl(path), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
        body: JSON.stringify({ fields }),
    });

    if (!response.ok) {
        throw new Error(`${path}: ${await response.text()}`);
    }
}

export const stringValue = (value) => ({ stringValue: value });
export const timeValue = (value) => ({ timestampValue: value });

/**
 * Who may read this member's positions, and until when. Kept in the grant
 * alone. Each reader is `{ uid, until }`, `until` being an ISO timestamp.
 */
export const sharedWith = (readers) => ({
    mapValue: {
        fields: Object.fromEntries(readers.map(({ uid, until }) => [uid, timeValue(until)])),
    },
});

/**
 * The group document with its roster, run by `admin`, and the mark that the
 * project has its group (ADR-0036): without it a build pointed at the emulator
 * would offer to found a second one.
 */
export async function writeGroup(roster, admin) {
    await write(`groups/${GROUP}`, {
        members: { arrayValue: { values: roster.map(stringValue) } },
        admins: { arrayValue: { values: [stringValue(admin)] } },
    });

    await write('project/group', { group: stringValue(GROUP) });
}

/** The profile everybody in the group reads, written for the member `uid`. */
export async function writeProfile(uid, displayName, updatedAt) {
    await write(`groups/${GROUP}/members/${uid}`, {
        displayName: stringValue(displayName),
        photoUrl: { nullValue: null },
        updatedAt: timeValue(updatedAt),
    });
}
