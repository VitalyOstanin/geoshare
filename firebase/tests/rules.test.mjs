// What the security rules let each side do.
//
// Run through tools/rules.sh, which starts the Firestore emulator around this
// file. The emulator needs no project of our own: a project id starting with
// "demo-" never reaches the real service.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, it } from 'node:test';

import {
    assertFails,
    assertSucceeds,
    initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
    collection,
    deleteDoc,
    doc,
    getDoc,
    getDocs,
    query,
    serverTimestamp,
    setDoc,
    where,
} from 'firebase/firestore';

const GROUP = 'family';
const ALICE = 'alice-uid';
const BOB = 'bob-uid';
const STRANGER = 'stranger-uid';

let testEnv;

// One document per phone: the id is the account and the phone joined by a dot.
const positionOf = (db, uid, device = 'phone') =>
    doc(db, `groups/${GROUP}/positions/${uid}.${device}`);
const watchOf = (db, uid, device = 'phone') => doc(db, `groups/${GROUP}/watch/${uid}.${device}`);
const memberOf = (db, uid) => doc(db, `groups/${GROUP}/members/${uid}`);

const someProfile = (name) => ({
    displayName: name,
    photoUrl: 'https://lh3.googleusercontent.com/a/example',
    updatedAt: new Date(),
});

// A permission without an end, written as a date far enough away to read as
// never: the rules compare a timestamp, and a missing value would be a case of
// its own in every one of them.
const FOREVER = new Date('9999-12-31T00:00:00Z');
const LONG_OVER = new Date('2020-01-01T00:00:00Z');

// Who this member lets read their position, and until when. Kept in the
// profile, which is the one place the owner writes it and the one place the
// rules read it from: a copy inside a position document would go on granting
// what has been withdrawn until the phone writes again.
const profileSharing = (name, readers) => ({ ...someProfile(name), sharedWith: readers });

// The fields both clients actually write, with the names they actually use.
const somePosition = {
    deviceName: 'phone',
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

const positionFrom = (uid) => ({ ...somePosition, memberId: uid });

const watchFrom = (uid) => ({ memberId: uid, since: serverTimestamp() });

before(async () => {
    testEnv = await initializeTestEnvironment({
        projectId: 'demo-geoshare',
        firestore: {
            rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
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
        await setDoc(memberOf(db, ALICE), profileSharing('Alice', { [BOB]: FOREVER }));
        await setDoc(memberOf(db, BOB), profileSharing('Bob', { [ALICE]: FOREVER }));
        await setDoc(positionOf(db, BOB), positionFrom(BOB));
    });
});

describe('positions', () => {
    it("a member reads another member's position", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(positionOf(db, BOB)));
    });

    it('a member writes their own position', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(positionOf(db, ALICE), positionFrom(ALICE)));
    });

    // The case the whole change is for: one account on two phones, each
    // writing where it is without touching the other's document.
    it('a member writes a position for each of their phones', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(positionOf(db, ALICE, 'one'), positionFrom(ALICE)));
        await assertSucceeds(setDoc(positionOf(db, ALICE, 'two'), positionFrom(ALICE)));
    });

    // A document whose whole id is the account, naming no owner, is what the
    // builds before the change wrote. Such a document would be a second marker
    // for a member whose phones already write one each, so it is refused.
    it('a position in the shape of the older builds is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(doc(db, `groups/${GROUP}/positions/${ALICE}`), somePosition));
    });

    it("a member cannot write another member's position", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(positionOf(db, BOB), positionFrom(BOB)));
    });

    // The owner is what the readers group by, so a document may not claim an
    // owner other than the account that wrote it.
    it("a member cannot sign their position with another member's name", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(positionOf(db, ALICE), positionFrom(BOB)));
    });

    // The point of the permissions: the roster says who is in the household,
    // and the owner says which of them may watch.
    it('a member cannot read a position not shared with them', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(memberOf(context.firestore(), BOB), profileSharing('Bob', {}));
        });

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    // The point of reading the permission from the profile: the owner takes it
    // back, and it is gone from that moment, whatever their phone is doing —
    // standing still with nothing to write, asleep for the night, switched
    // off, lost, or without the application on it at all. The position
    // document here is the one the owner wrote while sharing, and it is not
    // touched.
    it('a permission taken back stops the reading at once', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(positionOf(db, BOB)));

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(memberOf(context.firestore(), BOB), profileSharing('Bob', {}));
        });

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    // What an expiry is for: the permission ends on its own, without the owner
    // having to remember to withdraw it.
    it('a member cannot read a position whose permission has run out', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(
                memberOf(context.firestore(), BOB),
                profileSharing('Bob', { [ALICE]: LONG_OVER }),
            );
        });

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    // The owner needs no permission of their own, and a build that has not
    // written one yet still shows its own phone on the map.
    it('a member reads their own position without a permission', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(memberOf(context.firestore(), ALICE), profileSharing('Alice', {}));
        });

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        await assertSucceeds(getDoc(positionOf(db, ALICE)));
    });

    // How both clients have to read the map: a document at a time, by name.
    // A query is allowed only when its own filters prove the rule, and an
    // expiry weighed against the server's clock cannot be proved by anything
    // a client sends, so the collection itself stays closed.
    it('a member cannot read the collection of positions', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();
        const positions = collection(db, `groups/${GROUP}/positions`);

        await assertFails(getDocs(positions));
        await assertFails(getDocs(query(positions, where('memberId', '==', BOB))));
    });

    it('a stranger reads nothing', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    it('a signed out client reads nothing', async () => {
        const db = testEnv.unauthenticatedContext().firestore();

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    it('a stranger cannot write a position of their own', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(setDoc(positionOf(db, STRANGER), positionFrom(STRANGER)));
    });

    // Every other client fetches this document and draws what is in it. A
    // field nobody asked for is a field nobody checked, and a document holds a
    // megabyte of them.
    it('a position with a field of its own is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), note: 'anything' }),
        );
    });

    it('a phone name longer than a card can hold is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), deviceName: 'n'.repeat(41) }),
        );
    });

    // A degree count far outside the globe is what hung the map on the phone,
    // and the rule is what keeps it from being written in the first place.
    it('a coordinate off the globe is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), longitude: 1e15 }),
        );
        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), latitude: 91 }),
        );
    });

    // The age of a position is the whole of what the readers judge it by, so
    // the moment on it has to be the server's and not a phone's.
    it("a position stamped by the phone's own clock is refused", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), writtenAt: new Date() }),
        );
    });

    it('a charge outside what a battery reads is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), batteryPercent: 140 }),
        );
    });
});

describe('watch', () => {
    it('a member marks themselves as watching', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(watchOf(db, ALICE), watchFrom(ALICE)));
    });

    // A member looking at the map on one phone must not clear the marker the
    // other phone of the same account had just written.
    it('each phone of a member marks itself', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(watchOf(db, ALICE, 'one'), watchFrom(ALICE)));
        await assertSucceeds(setDoc(watchOf(db, ALICE, 'two'), watchFrom(ALICE)));
    });

    // The same shape the builds before the change wrote: the whole id is the
    // account and no owner is named.
    it('a marker in the shape of the older builds is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(doc(db, `groups/${GROUP}/watch/${ALICE}`), { until: new Date() }),
        );
    });

    it("a member reads another member's marker", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(watchOf(db, BOB)));
    });

    it("a member cannot forge another member's marker", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(watchOf(db, BOB), watchFrom(BOB)));
    });

    // The claim is worth three minutes from the moment the server wrote it.
    // Dated by the viewer instead, it would be worth whatever their clock
    // says: years, if they cared to, and every phone of the group would spend
    // its battery in the fifteen-second mode for as long.
    it("a claim dated by the viewer's own clock is refused", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(watchOf(db, ALICE), { memberId: ALICE, since: new Date() }));
    });

    it('a claim that names its own end is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(watchOf(db, ALICE), {
                memberId: ALICE,
                since: serverTimestamp(),
                until: FOREVER,
            }),
        );
    });

    // Closing the map deletes the marker, so the delete has to pass the same
    // check as the write.
    it('a member clears their own marker', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(watchOf(db, ALICE), watchFrom(ALICE));

        await assertSucceeds(deleteDoc(watchOf(db, ALICE)));
    });
});

describe('profiles', () => {
    it("a member reads another member's profile", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(memberOf(db, BOB)));
    });

    it('a member writes their own profile', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(memberOf(db, ALICE), someProfile('Alice')));
    });

    // A name on a marker is a name somebody else reads, so the account it
    // describes has to be the account that wrote it.
    // Where the permissions are kept: the phone reads them from here and
    // copies them into every position it writes, so a new phone of the same
    // member shares with the same people without being told again.
    it('a profile carries who the member shares with', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(memberOf(db, ALICE), {
                ...someProfile('Alice'),
                sharedWith: { [BOB]: FOREVER },
            }),
        );
    });

    // Which documents to read a member's positions from: the collection cannot
    // be listed, so the profile is where a reader learns what a member carries.
    it('a profile names the devices of the member', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(memberOf(db, ALICE), {
                ...someProfile('Alice'),
                devices: ['one', 'two'],
            }),
        );
    });

    it("a member cannot write another member's profile", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(memberOf(db, BOB), someProfile('Not Bob')));
    });

    // What both clients actually write for an account with no picture: the
    // field is there and empty, so that losing a picture loses it everywhere.
    it('a picture written as nothing is accepted', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(memberOf(db, ALICE), {
                displayName: 'Alice',
                photoUrl: null,
                updatedAt: new Date(),
            }),
        );
    });

    it('an account without a picture still has a profile', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(memberOf(db, ALICE), { displayName: 'Alice', updatedAt: new Date() }),
        );
    });

    // The page and the phone read these fields and show them; a document that
    // carries anything else is a document neither client asked for.
    it('a profile with a field of its own is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(memberOf(db, ALICE), { ...someProfile('Alice'), colour: '#000000' }),
        );
    });

    // A reader opens one subscription per device named here, and one per
    // member it shares with is one more document to keep an eye on. A profile
    // naming thousands of either would put every screen in the group out of
    // action, so the counts are held to what a household has.
    it('a profile naming more devices than a member carries is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(memberOf(db, ALICE), {
                ...someProfile('Alice'),
                devices: Array.from({ length: 11 }, (unused, index) => `phone-${index}`),
            }),
        );
    });

    it('a profile sharing with more readers than a household holds is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();
        const readers = Object.fromEntries(
            Array.from({ length: 51 }, (unused, index) => [`reader-${index}`, FOREVER]),
        );

        await assertFails(
            setDoc(memberOf(db, ALICE), { ...someProfile('Alice'), sharedWith: readers }),
        );
    });

    it('a name too long for a marker is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(memberOf(db, ALICE), someProfile('A'.repeat(101))));
    });

    it('a picture address too long to be one is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(memberOf(db, ALICE), {
                displayName: 'Alice',
                photoUrl: `https://example.com/${'a'.repeat(500)}`,
                updatedAt: new Date(),
            }),
        );
    });

    it('a member clears their own profile', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(deleteDoc(memberOf(db, ALICE)));
    });

    it('a stranger reads no profile', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(memberOf(db, BOB)));
    });
});

describe('membership', () => {
    // The group document is the roster, and it is maintained from the console.
    // A phone that could edit it could add itself to the group.
    it('a member cannot add anybody to the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(doc(db, `groups/${GROUP}`), { members: [ALICE, BOB, STRANGER] }));
    });
});
