// A showcase household for screenshots: invented people in invented places.
//
// Run by hand against emulators that are already up:
//
//     node firebase/emulator/showcase-seed.mjs
//
// It writes the same shapes seed.mjs does (group, profiles, grants, device
// summaries, positions) and differs only in the data: four people in the
// middle of Tallinn, positions a few minutes old and batteries at different
// levels, so a screenshot shows a lived-in map rather than a test fixture.
// The ages are counted from the moment of the run, so run it right before
// taking the pictures.
//
// Written against the emulators' own REST interfaces, like seed.mjs: no
// credentials exist here and an owner token is whatever the emulator is told.
const AUTH = process.env.AUTH_EMULATOR_HOST ?? '127.0.0.1:9099';
const FIRESTORE = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080';
// The debug build carries this same project id (see StandInServices.kt);
// seeding any other name fills a database nothing reads.
const PROJECT = process.env.GEOSHARE_PROJECT ?? 'demo-geoshare';
const GROUP = process.env.GEOSHARE_GROUP ?? 'family';

// The same password for all of them, as in seed.mjs.
const PASSWORD = 'geoshare';

// A permission without an end, written as one far enough away to read as never.
const FOREVER = '9999-12-31T00:00:00Z';

const NOW = Date.now();
const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;

const minutesAgo = (minutes) => new Date(NOW - minutes * MINUTE).toISOString();

// The account ids are chosen rather than generated, because the colour of a
// member is taken from the id (colourIndex in web/public/lib/members.js) and
// these four give Anna amber (4), Mark red (2), Mia and Leo blue (0): three
// distinct colours on the screenshot, with the two blues far apart on the map.
const MEMBERS = [
    {
        localId: 'showanna2',
        // The debug build offers buttons only for the addresses seed.mjs
        // writes (STAND_IN_ACCOUNTS), and the screenshots are taken signed in
        // as Anna, so she borrows the first of them.
        email: 'parent@geoshare.test',
        name: 'Anna',
        device: 'pixel-8',
        model: 'Pixel 8',
        latitude: 59.437,
        longitude: 24.7454,
        ageMinutes: 2,
        battery: 81,
    },
    {
        localId: 'showmark1',
        email: 'mark@geoshare.test',
        name: 'Mark',
        device: 'galaxy-s24',
        model: 'Galaxy S24',
        latitude: 59.4386,
        longitude: 24.788,
        ageMinutes: 7,
        battery: 64,
    },
    {
        localId: 'showmia1',
        email: 'mia@geoshare.test',
        name: 'Mia',
        device: 'redmi-note-13',
        model: 'Redmi Note 13',
        latitude: 59.44,
        longitude: 24.737,
        ageMinutes: 4,
        battery: 38,
    },
    {
        localId: 'showleo2',
        email: 'leo@geoshare.test',
        name: 'Leo',
        device: 'galaxy-a54',
        model: 'Galaxy A54',
        latitude: 59.4455,
        longitude: 24.735,
        ageMinutes: 11,
        battery: 55,
        // The one whose newer answers are being held back, for the card that
        // says why: three of them since six minutes ago, each too far from the
        // last place to have been reached in time.
        leftOut: { count: 3, reason: 'Speed', sinceMinutes: 6 },
    },
];

// Anna runs the group.
const ADMIN = 'parent@geoshare.test';

// Everyone opens their place to everyone else for good, except that Mia lets
// Leo see hers only until three hours after the run: the one grant on the
// screenshots that shows a countdown.
const LIMITED = { from: 'mia@geoshare.test', to: 'leo@geoshare.test', until: NOW + 3 * HOUR };

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

/**
 * The account with the id it is given, created if the emulator has not seen it
 * before, and carrying the name it is known by.
 *
 * Unlike seed.mjs, which lets the emulator pick the id, this asks for one:
 * accounts:signUp with a localId is an administrator's call in Identity
 * Toolkit, and the emulator honours it for the owner token every request here
 * carries. An account that exists already is not an error, it is signed in to
 * learn its id, so a second run changes nothing it should not.
 *
 * The address is marked verified, as a Google account's always is.
 */
async function account({ localId, email, name }) {
    let id;

    try {
        const created = await call(authUrl('accounts:signUp'), {
            localId,
            email,
            password: PASSWORD,
            returnSecureToken: true,
        });

        id = created.localId;
    } catch (error) {
        if (!`${error}`.includes('EMAIL_EXISTS') && !`${error}`.includes('DUPLICATE_LOCAL_ID')) {
            throw error;
        }

        const known = await call(authUrl('accounts:signInWithPassword'), {
            email,
            password: PASSWORD,
            returnSecureToken: true,
        });

        id = known.localId;
    }

    await call(authUrl('accounts:update'), { localId: id, displayName: name, emailVerified: true });

    return id;
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
    mapValue: {
        fields: Object.fromEntries(readers.map(({ uid, until }) => [uid, timeValue(until)])),
    },
});

// Everything the emulators hold goes first. seed.mjs runs when they start and
// has already made parent@ under an id of its own choosing, which would give
// Anna the wrong colour; and its household would otherwise share the map.
async function clear(url) {
    const response = await fetch(url, { method: 'DELETE', headers: { Authorization: 'Bearer owner' } });

    if (!response.ok) {
        throw new Error(`${url}: ${response.status} ${await response.text()}`);
    }
}

await clear(`http://${AUTH}/emulator/v1/projects/${PROJECT}/accounts`);
await clear(`http://${FIRESTORE}/emulator/v1/projects/${PROJECT}/databases/(default)/documents`);

const uids = new Map();

for (const member of MEMBERS) {
    uids.set(member.email, await account(member));
}

const roster = MEMBERS.map((member) => uids.get(member.email));

await write(`groups/${GROUP}`, {
    members: { arrayValue: { values: roster.map(stringValue) } },
    admins: { arrayValue: { values: [stringValue(uids.get(ADMIN))] } },
});

for (const member of MEMBERS) {
    const uid = uids.get(member.email);
    const seen = minutesAgo(member.ageMinutes);

    await write(`groups/${GROUP}/members/${uid}`, {
        displayName: stringValue(member.name),
        photoUrl: { nullValue: null },
        updatedAt: timeValue(new Date(NOW).toISOString()),
    });

    const readers = MEMBERS.filter((other) => other !== member).map((other) => ({
        uid: uids.get(other.email),
        until:
            member.email === LIMITED.from && other.email === LIMITED.to
                ? new Date(LIMITED.until).toISOString()
                : FOREVER,
    }));

    await write(`groups/${GROUP}/grants/${uid}`, {
        sharedWith: sharedWith(readers),
        devices: { arrayValue: { values: [stringValue(member.device)] } },
    });

    await write(`groups/${GROUP}/devices/${uid}`, {
        [member.device]: {
            mapValue: {
                fields: {
                    model: stringValue(member.model),
                    onlineAt: timeValue(seen),
                    mode: stringValue('Standby'),
                    batteryPercent: { integerValue: String(member.battery) },
                    charging: { booleanValue: false },
                },
            },
        },
    });

    await write(`groups/${GROUP}/positions/${uid}.${member.device}`, {
        memberId: stringValue(uid),
        latitude: { doubleValue: member.latitude },
        longitude: { doubleValue: member.longitude },
        takenAt: timeValue(seen),
        writtenAt: timeValue(seen),
        mode: stringValue('Standby'),
        batteryPercent: { integerValue: String(member.battery) },
        ...(member.leftOut && {
            leftOutCount: { integerValue: String(member.leftOut.count) },
            leftOutReason: stringValue(member.leftOut.reason),
            leftOutSince: timeValue(minutesAgo(member.leftOut.sinceMinutes)),
        }),
    });
}

for (const { email } of MEMBERS) {
    console.log(`${email}  ${uids.get(email)}`);
}
