// The stand-in household the emulator starts with.
//
// Run by tools/run-services.sh once the emulators answer. Three accounts, a
// group with the three of them on the roster, a profile each, and a position
// for the two the device under test is meant to be looking at.
//
// Written against the emulators' own REST interfaces rather than against a
// library: no credentials exist here, an owner token is whatever the emulator
// is told, and the alternative is an npm install for four requests.
import {
    GROUP,
    PASSWORD,
    FOREVER,
    authUrl,
    call,
    write,
    stringValue,
    timeValue,
    sharedWith,
    writeGroup,
    writeProfile,
} from './rest.mjs';

const MEMBERS = [
    { email: 'parent@geoshare.test', name: 'Parent', device: 'emulator' },
    { email: 'spouse@geoshare.test', name: 'Spouse', device: 'spouse-phone' },
    { email: 'child@geoshare.test', name: 'Child', device: 'child-phone' },
];

// Accounts on no roster at all, for the way into a group: one founds a group
// of their own, the other joins it on the invitation the first hands out.
// Nothing is written for them beyond the account itself — everything else in
// the household is closed to somebody outside it, which is the point of them.
const OUTSIDERS = [
    { email: 'stranger@geoshare.test', name: 'Stranger' },
    { email: 'newcomer@geoshare.test', name: 'Newcomer' },
];

// A pair of places a few hundred metres apart, so the map opens on something
// that looks like a household rather than on null island.
const PLACES = {
    'spouse@geoshare.test': { latitude: 61.0327, longitude: 4.403 },
    'child@geoshare.test': { latitude: 61.0295, longitude: 4.4108 },
};

/**
 * The account as it is, created if the emulator has not seen it before, and
 * carrying the name it is known by.
 *
 * The name is the account's own rather than the group's: a build reads it off
 * whoever is signed in and writes the profile from that, so an account without
 * one has nothing to tell the group and writes no profile at all. Set on every
 * run, because an account the emulator already held was made before this and
 * would otherwise stay nameless.
 *
 * The address is marked verified, as a Google account's always is: an
 * invitation lets in only an account whose verified address it names, and an
 * address made up with a password here would otherwise never be one.
 */
async function account(email, displayName) {
    const localId = await accountOf(email);

    await call(authUrl('accounts:update'), { localId, displayName, emailVerified: true });

    return localId;
}

async function accountOf(email) {
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

const uids = new Map();

for (const member of MEMBERS) {
    uids.set(member.email, await account(member.email, member.name));
}

const roster = MEMBERS.map((member) => uids.get(member.email));

// The first of the three runs the group, so that a build pointed at the
// emulator can make an invitation and watch somebody use it. A roster with
// nobody running it is the household that predates invitations, and that one
// is edited from the console.
await writeGroup(roster, roster[0]);

for (const member of MEMBERS) {
    const uid = uids.get(member.email);

    await writeProfile(uid, member.name, new Date().toISOString());

    // Apart from the profile, which everybody in the group reads: who may
    // watch and which devices they would be watching are for the member and
    // their readers alone.
    await write(`groups/${GROUP}/grants/${uid}`, {
        // Everyone else in the household, so that a build pointed at the
        // emulator has something to read on its first launch.
        sharedWith: sharedWith(
            roster
                .filter((reader) => reader !== uid)
                .map((reader) => ({ uid: reader, until: FOREVER })),
        ),
        // Which documents hold this member's positions: the collection cannot
        // be listed, so a reader learns the devices from here.
        devices: { arrayValue: { values: [stringValue(member.device)] } },
    });

    // How each device last said it is, which the administrator's panel lists and
    // the siren is called on: without it a member shows no devices to ring.
    await write(`groups/${GROUP}/devices/${uid}`, {
        [member.device]: {
            mapValue: {
                fields: {
                    model: stringValue(member.device),
                    onlineAt: timeValue(new Date().toISOString()),
                    mode: stringValue('Standby'),
                    batteryPercent: { integerValue: '74' },
                    charging: { booleanValue: false },
                },
            },
        },
    });

    const place = PLACES[member.email];

    if (place === undefined) {
        continue;
    }

    await write(`groups/${GROUP}/positions/${uid}.${member.device}`, {
        memberId: stringValue(uid),
        latitude: { doubleValue: place.latitude },
        longitude: { doubleValue: place.longitude },
        takenAt: timeValue(new Date().toISOString()),
        writtenAt: timeValue(new Date().toISOString()),
        mode: stringValue('Standby'),
        batteryPercent: { integerValue: '74' },
    });
}

for (const outsider of OUTSIDERS) {
    uids.set(outsider.email, await account(outsider.email, outsider.name));
}

for (const { email } of [...MEMBERS, ...OUTSIDERS]) {
    console.log(`${email}  ${uids.get(email)}`);
}
