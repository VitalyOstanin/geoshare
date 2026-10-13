// What every file of rules tests shares: the emulator, the household it
// starts from and the shapes of the documents written into it.
//
// Run through tools/rules.sh, which starts the Firestore emulator around these
// files. The emulator needs no project of our own: a project id starting with
// "demo-" never reaches the real service.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach } from 'node:test';

import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, serverTimestamp, setDoc, writeBatch } from 'firebase/firestore';

export const GROUP = 'family';
export const ALICE = 'alice-uid';
export const BOB = 'bob-uid';
export const STRANGER = 'stranger-uid';

export let testEnv;

// One document per device: the id is the account and the device joined by a
// dot.
export const positionOf = (db, uid, device = 'phone') =>
    doc(db, `groups/${GROUP}/positions/${uid}.${device}`);
export const watchOf = (db, uid, device = 'phone') => doc(db, `groups/${GROUP}/watch/${uid}.${device}`);
export const memberOf = (db, uid) => doc(db, `groups/${GROUP}/members/${uid}`);
export const grantOf = (db, uid) => doc(db, `groups/${GROUP}/grants/${uid}`);

export const someProfile = (name) => ({
    displayName: name,
    photoUrl: 'https://lh3.googleusercontent.com/a/example',
    updatedAt: new Date(),
});

// A permission without an end, written as a date far enough away to read as
// never: the rules compare a timestamp, and a missing value would be a case of
// its own in every one of them.
export const FOREVER = new Date('9999-12-31T00:00:00Z');
export const LONG_OVER = new Date('2020-01-01T00:00:00Z');

// Who this member lets read their position, and until when, beside the devices
// they carry. Kept apart from the profile, which everybody in the group reads:
// these two answer "where is this person", and only the people given a
// permission see them. The rules read the permission from here rather than
// from a copy inside a position document, which would go on granting what has
// been withdrawn until the device writes again.
export const someGrant = (readers, devices = ['phone']) => ({ sharedWith: readers, devices });

// The fields both clients actually write, with the names they actually use.
export const somePosition = {
    latitude: 60.93,
    longitude: 4.34,
    takenAt: new Date(),
    // The server's own clock: the rules insist on it, because a reader cannot
    // tell a fresh position from an old one otherwise.
    writtenAt: serverTimestamp(),
    mode: 'Standby',
    batteryPercent: 80,
    charging: false,
};

export const positionFrom = (uid) => ({ ...somePosition, memberId: uid });

export const watchFrom = (uid) => ({ memberId: uid, since: serverTimestamp() });



// What an invitation looks like, and how long one lasts. Eight characters
// without the letters and digits that are read for each other, so that a code
// read aloud arrives as it was sent.
export const CODE = 'ABCD2345';
export const OTHER_CODE = 'KLMN6789';

export const groupDoc = (db, id = GROUP) => doc(db, `groups/${id}`);
export const projectGroupDoc = (db) => doc(db, 'project/group');
export const invitationOf = (db, code) => doc(db, `groups/${GROUP}/invitations/${code}`);
export const claimOf = (db, uid) => doc(db, `groups/${GROUP}/joins/${uid}`);

// Founding a group the way the clients do it (ADR-0036): the group and the
// document naming it in one batch. `data` is what the group is written with;
// `named` is what `project/group` says, the group's own id unless a test means
// otherwise.
export const foundGroup = (db, groupId, data, named = groupId) =>
    writeBatch(db)
        .set(groupDoc(db, groupId), data)
        .set(projectGroupDoc(db), { group: named })
        .commit();

export const inTenMinutes = () => new Date(Date.now() + 10 * 60 * 1000);

// The group as it stands once somebody runs it, which the household that
// started before invitations existed does not: its roster has no
// administrators at all and is still edited from the console.
export const plantAnAdministrator = () =>
    testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(groupDoc(context.firestore()), { members: [ALICE, BOB], admins: [ALICE] });
    });

// Made before invitations named an account: admits whoever holds the code.
export const plantAnInvitation = (code = CODE, expiresAt = inTenMinutes()) =>
    testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(invitationOf(context.firestore(), code), { createdBy: ALICE, expiresAt });
    });

// The address the stranger signs in with, and an invitation made out to it.
export const STRANGER_EMAIL = 'stranger@example.com';

export const plantAnAddressedInvitation = (email = STRANGER_EMAIL, code = CODE) =>
    testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(invitationOf(context.firestore(), code), {
            createdBy: ALICE,
            expiresAt: inTenMinutes(),
            email,
        });
    });

// The stranger signed in with a Google account whose address is `email`.
export const signedInAs = (email, verified = true) =>
    testEnv.authenticatedContext(STRANGER, { email, email_verified: verified }).firestore();


/**
 * The emulator for a file of tests, and the household every test starts from.
 */
export function setUpRules() {
    before(async () => {
        testEnv = await initializeTestEnvironment({
            projectId: 'demo-geoshare',
            firestore: {
                rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8'),
            },
        });
    });

    after(async () => {
        await testEnv.cleanup();
    });

    // The group document itself is written by hand from the console, so the tests
    // plant it the same way: with the rules out of the way.
    beforeEach(async () => {
        await testEnv.clearFirestore();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(doc(db, `groups/${GROUP}`), { members: [ALICE, BOB] });
            await setDoc(memberOf(db, ALICE), someProfile('Alice'));
            await setDoc(memberOf(db, BOB), someProfile('Bob'));
            await setDoc(grantOf(db, ALICE), someGrant({ [BOB]: FOREVER }));
            await setDoc(grantOf(db, BOB), someGrant({ [ALICE]: FOREVER }));
            await setDoc(positionOf(db, BOB), positionFrom(BOB));
        });
    });

}
