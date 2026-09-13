// The stand-in household the emulator starts with.
//
// Run by tools/run-services.sh once the emulators answer. Three accounts, a
// group with the three of them on the roster, a profile each, and a position
// for the two the phone under test is meant to be looking at.
//
// Written against the emulators' own REST interfaces rather than against a
// library: no credentials exist here, an owner token is whatever the emulator
// is told, and the alternative is an npm install for four requests.
import { readFileSync } from 'node:fs';

const AUTH = process.env.AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
const FIRESTORE = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080';
const PROJECT = process.env.GEOSHARE_PROJECT ?? projectOfTheApplication();
const GROUP = process.env.GEOSHARE_GROUP ?? 'family';

/**
 * The project the phone's database lives under.
 *
 * Pointing the Firebase library at an emulator changes the host it calls, not
 * the project it calls about: the phone keeps asking about the project named
 * in google-services.json, and the emulator keeps a separate database per
 * project. Seeding any other name fills a database nothing reads, and the
 * screens come up empty with the rules refusing every read.
 */
function projectOfTheApplication() {
    const services = new URL('../../android/app/google-services.json', import.meta.url);

    return JSON.parse(readFileSync(services, 'utf8')).project_info.project_id;
}

// The same password for all three: the sign-in screen of a build pointed at
// the emulator offers the accounts as buttons, and nobody types this.
const PASSWORD = 'geoshare';

// A permission without an end, written as one far enough away to read as
// never: the rules compare a timestamp, and a missing value would have to be
// a case of its own in every one of them.
const FOREVER = '9999-12-31T00:00:00Z';

const MEMBERS = [
    { email: 'parent@geoshare.test', name: 'Parent', device: 'emulator' },
    { email: 'spouse@geoshare.test', name: 'Spouse', device: 'spouse-phone' },
    { email: 'child@geoshare.test', name: 'Child', device: 'child-phone' },
];

// Around the the city flat the real group moves about, so the map
// opens on something that looks like a household rather than on null island.
const PLACES = {
    'spouse@geoshare.test': { latitude: 61.0327, longitude: 4.403 },
    'child@geoshare.test': { latitude: 61.0295, longitude: 4.4108 },
};

const authUrl = (path) => `http://${AUTH}/identitytoolkit.googleapis.com/v1/${path}?key=fake-api-key`;

const documentUrl = (path) =>
    `http://${FIRESTORE}/v1/projects/${PROJECT}/databases/(default)/documents/${path}`;

async function call(url, body) {
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

/** The account as it is, created if the emulator has not seen it before. */
async function account(email) {
    try {
        const created = await call(authUrl('accounts:signUp'), {
            email,
            password: PASSWORD,
            returnSecureToken: true,
        });

        return created.localId;
    } catch (error) {
        if (!`${error}`.includes('EMAIL_EXISTS')) {
            throw error;
        }

        const known = await call(authUrl('accounts:signInWithPassword'), {
            email,
            password: PASSWORD,
            returnSecureToken: true,
        });

        return known.localId;
    }
}

/** A whole document, replaced rather than merged: the seed is the truth here. */
async function write(path, fields) {
    const response = await fetch(documentUrl(path), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
        body: JSON.stringify({ fields }),
    });

    if (!response.ok) {
        throw new Error(`${path}: ${await response.text()}`);
    }
}

const stringValue = (value) => ({ stringValue: value });
const timeValue = (value) => ({ timestampValue: value });

/** Who may read this member's positions, and until when. Kept in the grant alone. */
const sharedWith = (readers) => ({
    mapValue: { fields: Object.fromEntries(readers.map((uid) => [uid, timeValue(FOREVER)])) },
});

const uids = new Map();

for (const member of MEMBERS) {
    uids.set(member.email, await account(member.email));
}

const roster = MEMBERS.map((member) => uids.get(member.email));

// The first of the three runs the group, so that a build pointed at the
// emulator can make an invitation and watch somebody use it. A roster with
// nobody running it is the household that predates invitations, and that one
// is edited from the console.
await write(`groups/${GROUP}`, {
    members: { arrayValue: { values: roster.map(stringValue) } },
    admins: { arrayValue: { values: [stringValue(roster[0])] } },
});

for (const member of MEMBERS) {
    const uid = uids.get(member.email);

    await write(`groups/${GROUP}/members/${uid}`, {
        displayName: stringValue(member.name),
        photoUrl: { nullValue: null },
        updatedAt: timeValue(new Date().toISOString()),
    });

    // Apart from the profile, which everybody in the group reads: who may
    // watch and which phones they would be watching are for the member and
    // their readers alone.
    await write(`groups/${GROUP}/grants/${uid}`, {
        // Everyone else in the household, so that a build pointed at the
        // emulator has something to read on its first launch.
        sharedWith: sharedWith(roster.filter((reader) => reader !== uid)),
        // Which documents hold this member's positions: the collection cannot
        // be listed, so a reader learns the devices from here.
        devices: { arrayValue: { values: [stringValue(member.device)] } },
    });

    const place = PLACES[member.email];

    if (place === undefined) {
        continue;
    }

    await write(`groups/${GROUP}/positions/${uid}.${member.device}`, {
        memberId: stringValue(uid),
        deviceName: stringValue(member.device),
        latitude: { doubleValue: place.latitude },
        longitude: { doubleValue: place.longitude },
        takenAt: timeValue(new Date().toISOString()),
        writtenAt: timeValue(new Date().toISOString()),
        mode: stringValue('Standby'),
        batteryPercent: { integerValue: '74' },
    });
}

for (const member of MEMBERS) {
    console.log(`${member.email}  ${uids.get(member.email)}`);
}
