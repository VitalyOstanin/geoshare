// What the security rules let each side do.
//
// Run through tools/rules.sh, which starts the Firestore emulator around this
// file. The emulator needs no project of our own: a project id starting with
// "demo-" never reaches the real service.
import assert from 'node:assert/strict';
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
    arrayUnion,
    setDoc,
    updateDoc,
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
const grantOf = (db, uid) => doc(db, `groups/${GROUP}/grants/${uid}`);

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

// Who this member lets read their position, and until when, beside the phones
// they carry. Kept apart from the profile, which everybody in the group reads:
// these two answer "where is this person", and only the people given a
// permission see them. The rules read the permission from here rather than
// from a copy inside a position document, which would go on granting what has
// been withdrawn until the phone writes again.
const someGrant = (readers, devices = ['phone']) => ({ sharedWith: readers, devices });

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
        await setDoc(memberOf(db, ALICE), someProfile('Alice'));
        await setDoc(memberOf(db, BOB), someProfile('Bob'));
        await setDoc(grantOf(db, ALICE), someGrant({ [BOB]: FOREVER }));
        await setDoc(grantOf(db, BOB), someGrant({ [ALICE]: FOREVER }));
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

    // The whole point of the three fields: a phone under a transmitter writes
    // no positions at all, and its card has to say that rather than let the
    // marker quietly get older.
    it('a member says their answers are being left out', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        await assertSucceeds(updateDoc(positionOf(db, ALICE), {
            leftOutCount: 217,
            leftOutReason: 'Drifted',
            leftOutSince: new Date(),
        }));
    });

    // What such a write leaves alone: the position and the moment the server
    // stamped it. A marker whose writtenAt moved on while the phone wrote no
    // position would stop ageing under a transmitter, and the age of a marker
    // is what a reader decides by. The rules cannot forbid an owner to write
    // their own document whole — nor should they — so what is checked here is
    // that saying this much does not require it.
    it('saying so leaves the position and its moment alone', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        const before = await getDoc(positionOf(db, ALICE));

        await assertSucceeds(updateDoc(positionOf(db, ALICE), {
            leftOutCount: 5,
            leftOutReason: 'Contradicted',
            leftOutSince: new Date(),
        }));

        const after = await getDoc(positionOf(db, ALICE));

        assert.deepEqual(after.data().writtenAt, before.data().writtenAt);
        assert.equal(after.data().latitude, before.data().latitude);
    });

    // Nor may it move the phone: the coordinates of a position are written
    // whole or not at all.
    it('saying so does not move the position', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        await assertFails(updateDoc(positionOf(db, ALICE), {
            leftOutCount: 5,
            latitude: 61.5,
        }));
    });

    // A source the member switched off is written as two words joined by a
    // colon, so that the card can name which of the five it was.
    it('a reason may name the switch that was turned off', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        await assertSucceeds(updateDoc(positionOf(db, ALICE), {
            leftOutReason: 'Source:OtherRadios',
        }));
    });

    it('a count of answers left out is held to a number', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        await assertFails(updateDoc(positionOf(db, ALICE), { leftOutCount: 'many' }));
    });

    // The reason is shown on everybody's card, so it is held to the length of
    // a word rather than left to be a sentence of somebody's choosing.
    it('a reason longer than a word is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(positionOf(db, ALICE), positionFrom(ALICE));

        await assertFails(updateDoc(positionOf(db, ALICE), {
            leftOutReason: 'the answers have walked away from a phone nobody touched',
        }));
    });

    it("a member cannot say another member's answers are being left out", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(updateDoc(positionOf(db, BOB), { leftOutCount: 3 }));
    });

    // A phone whose answers were being thrown out before it ever wrote a
    // position has nowhere to say so: a document of these fields alone would
    // be a marker with no coordinates on everybody's map.
    it('there is nowhere to say it before a position has been written', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(positionOf(db, ALICE, 'fresh'), {
            memberId: ALICE,
            leftOutCount: 3,
            leftOutReason: 'Drifted',
        }));
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
            await setDoc(grantOf(context.firestore(), BOB), someGrant({}));
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
            await setDoc(grantOf(context.firestore(), BOB), someGrant({}));
        });

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    // What an expiry is for: the permission ends on its own, without the owner
    // having to remember to withdraw it.
    it('a member cannot read a position whose permission has run out', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(grantOf(context.firestore(), BOB), someGrant({ [ALICE]: LONG_OVER }));
        });

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    // The owner needs no permission of their own, and a build that has not
    // written one yet still shows its own phone on the map.
    it('a member reads their own position without a permission', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(grantOf(context.firestore(), ALICE), someGrant({}));
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

    // How wide the provider called its answer, which the readers draw as a
    // circle around the marker. A tower answers about a district, and that is
    // a width of well over a kilometre.
    it('a position says how wide its answer is', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), accuracyMetres: 1700 }),
        );
    });

    // A radius is a distance and cannot be negative; and past a hundred
    // kilometres a circle stops describing a place, while a number of any size
    // would let one phone paint over the whole map.
    it('a width no answer has is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), accuracyMetres: -1 }),
        );
        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), accuracyMetres: 100001 }),
        );
        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), accuracyMetres: 'wide' }),
        );
    });

    // How the phone says it arrived at the answer. The word is the phone's
    // own — the satellites, an assistance service, the towers and the Wi-Fi —
    // and a reader shows it in its own language.
    it('a position says how it was arrived at', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), foundBy: 'Satellites' }),
        );
    });

    // A reader puts this on a card, so a phone must not be able to write a
    // sentence of its own choosing into everybody's panel.
    it('a source that is not one word is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), foundBy: 7 }),
        );
        await assertFails(
            setDoc(positionOf(db, ALICE), {
                ...positionFrom(ALICE),
                foundBy: 'the satellites, most of them anyway',
            }),
        );
    });

    // Whether the receiver had settled when the fix was taken. A phone that
    // has just been switched on answers narrowly and wrongly, and this is what
    // lets a card say so.
    it('a position says its receiver had not settled', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), settling: true }),
        );
    });

    // A flag and nothing else: the readers show a sentence for it, and a
    // number or a word here would be a phone writing into everybody's card.
    it('a receiver note that is not a flag is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), settling: 23 }),
        );
        await assertFails(
            setDoc(positionOf(db, ALICE), { ...positionFrom(ALICE), settling: 'not yet' }),
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

    // The shape the builds before the split wrote: everybody in the group read
    // this document, so a member's phones and the people they shared with were
    // on show to members who had been given nothing. Both now live in the
    // grant, and the profile refuses them.
    it('a profile carrying who the member shares with is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(memberOf(db, ALICE), {
                ...someProfile('Alice'),
                sharedWith: { [BOB]: FOREVER },
            }),
        );
    });

    it('a profile naming the devices of the member is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
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

describe('grants', () => {
    it('a member writes their own grant', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(grantOf(db, ALICE), someGrant({ [BOB]: FOREVER })));
    });

    it('a member reads their own grant', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(grantOf(db, ALICE)));
    });

    // What a permission is worth beyond the positions themselves: the reader
    // learns which phones to subscribe to, because the collection of positions
    // cannot be listed.
    it('a reader with a permission reads the grant', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(grantOf(db, BOB)));
    });

    // The leak this collection closes: a member of the household who has been
    // given nothing learns nothing — not how many phones the other carries,
    // and not who else they share with.
    it('a member without a permission reads nothing of the grant', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(grantOf(context.firestore(), BOB), someGrant({}));
        });

        await assertFails(getDoc(grantOf(db, BOB)));
    });

    it('a permission that has run out reads nothing of the grant', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(grantOf(context.firestore(), BOB), someGrant({ [ALICE]: LONG_OVER }));
        });

        await assertFails(getDoc(grantOf(db, BOB)));
    });

    // Listing would hand every member the whole household's phones in one
    // read, which is exactly what the split was made to prevent.
    it('a member cannot list the grants', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(getDocs(collection(db, `groups/${GROUP}/grants`)));
    });

    it("a member cannot write another member's grant", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(grantOf(db, BOB), someGrant({ [ALICE]: FOREVER })));
    });

    it('a stranger reads no grant', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(grantOf(db, BOB)));
    });

    // A reader opens one subscription per device named here, and one per
    // member they share with is one more document to keep an eye on. A grant
    // naming thousands of either would put every screen that may read it out
    // of action, so the counts are held to what a household has.
    it('a grant naming more devices than a member carries is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(grantOf(db, ALICE), {
                sharedWith: {},
                devices: Array.from({ length: 11 }, (unused, index) => `phone-${index}`),
            }),
        );
    });

    it('a grant sharing with more readers than a household holds is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();
        const readers = Object.fromEntries(
            Array.from({ length: 51 }, (unused, index) => [`reader-${index}`, FOREVER]),
        );

        await assertFails(setDoc(grantOf(db, ALICE), someGrant(readers)));
    });

    it('a grant with a field of its own is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(grantOf(db, ALICE), { ...someGrant({}), note: 'anything' }),
        );
    });

    it('a member clears their own grant', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(deleteDoc(grantOf(db, ALICE)));
    });
});

// The phones a member carries, apart from whom they share with. The
// administrator sees the phones; only the people given a permission see them
// and the positions too.
describe('devices', () => {
    const devicesOf = (db, uid) => doc(db, `groups/${GROUP}/devices/${uid}`);
    const someDevices = {
        phone: { model: 'MAR-LX1', onlineAt: new Date(), mode: 'Standby', batteryPercent: 80 },
    };

    beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(devicesOf(context.firestore(), BOB), someDevices);
        });
    });

    it('a member writes and reads their own phones', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(devicesOf(db, ALICE), someDevices));
        await assertSucceeds(getDoc(devicesOf(db, ALICE)));
    });

    it('a reader with a permission reads the phones', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(devicesOf(db, BOB)));
    });

    it('a member without a permission reads nothing of the phones', async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(grantOf(context.firestore(), BOB), someGrant({ [ALICE]: LONG_OVER }));
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(getDoc(devicesOf(db, BOB)));
    });

    // What the administrator is shown in place of the grant: the phones and
    // whether they are in touch, not whom they are shared with.
    it('the administrator reads the phones without a permission', async () => {
        await plantAnAdministrator();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(grantOf(context.firestore(), BOB), someGrant({}));
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(devicesOf(db, BOB)));
        await assertFails(getDoc(grantOf(db, BOB)));
    });

    it('a member cannot list the phones', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(getDocs(collection(db, `groups/${GROUP}/devices`)));
    });

    it("a member cannot write another member's phones", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(devicesOf(db, BOB), someDevices));
    });

    it('more phones than a member carries are refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();
        const many = Object.fromEntries(
            Array.from({ length: 11 }, (unused, index) => [`phone-${index}`, { model: 'x' }]),
        );

        await assertFails(setDoc(devicesOf(db, ALICE), many));
    });

    it('a stranger reads no phones', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(devicesOf(db, BOB)));
    });
});

// What a member calls the phones and the people of the group, for themselves.
describe('labels', () => {
    const labelsOf = (db, uid) => doc(db, `groups/${GROUP}/labels/${uid}`);
    const someLabels = {
        devices: { [`${BOB}.phone`]: 'new phone' },
        members: { [BOB]: 'grandma' },
    };

    it('a member writes and reads their own names', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(labelsOf(db, ALICE), someLabels));
        await assertSucceeds(getDoc(labelsOf(db, ALICE)));
    });

    // Not even the person named, and not the administrator.
    it('nobody else reads them', async () => {
        await plantAnAdministrator();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(labelsOf(context.firestore(), BOB), someLabels);
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(getDoc(labelsOf(db, BOB)));
        await assertFails(setDoc(labelsOf(db, BOB), someLabels));
    });

    it('names with a field of their own are refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(labelsOf(db, ALICE), { ...someLabels, note: 'anything' }));
    });

    it('a stranger keeps no names in the group', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(setDoc(labelsOf(db, STRANGER), someLabels));
    });
});

// What an invitation looks like, and how long one lasts. Eight characters
// without the letters and digits that are read for each other, so that a code
// read aloud arrives as it was sent.
const CODE = 'ABCD2345';
const OTHER_CODE = 'KLMN6789';

const groupDoc = (db, id = GROUP) => doc(db, `groups/${id}`);
const invitationOf = (db, code) => doc(db, `groups/${GROUP}/invitations/${code}`);
const claimOf = (db, uid) => doc(db, `groups/${GROUP}/joins/${uid}`);

const inTenMinutes = () => new Date(Date.now() + 10 * 60 * 1000);

// The group as it stands once somebody runs it, which the household that
// started before invitations existed does not: its roster has no
// administrators at all and is still edited from the console.
const plantAnAdministrator = () =>
    testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(groupDoc(context.firestore()), { members: [ALICE, BOB], admins: [ALICE] });
    });

const plantAnInvitation = (code = CODE, expiresAt = inTenMinutes()) =>
    testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(invitationOf(context.firestore(), code), { createdBy: ALICE, expiresAt });
    });

describe('membership', () => {
    // The roster of the household that predates invitations: nobody runs it,
    // so nobody may edit it, and a phone that could would be a phone that can
    // add itself to the group.
    it('a member cannot add anybody to the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(groupDoc(db), { members: [ALICE, BOB, STRANGER] }));
    });

    // A group is named after the account that made it, which is the one name
    // its founder may write.
    it('somebody signed in makes a group with themselves alone in it', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(
            setDoc(groupDoc(db, STRANGER), { members: [STRANGER], admins: [STRANGER] }),
        );
    });

    // Otherwise one account could fill the database with groups nobody is in,
    // a document apiece and each of them free to make.
    it("a group cannot be given a name of its founder's choosing", async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, 'a-new-group'), { members: [STRANGER], admins: [STRANGER] }),
        );
    });

    it("a group cannot be made under somebody else's name", async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, ALICE), { members: [STRANGER], admins: [STRANGER] }),
        );
    });

    // Otherwise a group could be conjured around somebody who never asked to
    // be in one, and their phone would start writing into it.
    it('a group cannot be made with somebody else already in it', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, STRANGER), { members: [STRANGER, ALICE], admins: [STRANGER] }),
        );
    });

    it('a group cannot be made with somebody else running it', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, STRANGER), { members: [STRANGER], admins: [ALICE] }),
        );
    });

    it('nobody deletes a group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(deleteDoc(groupDoc(db)));
    });
});

describe('invitations', () => {
    it('the administrator makes an invitation', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(invitationOf(db, CODE), { createdBy: ALICE, expiresAt: inTenMinutes() }),
        );
    });

    it('a member who does not run the group makes none', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(
            setDoc(invitationOf(db, CODE), { createdBy: BOB, expiresAt: inTenMinutes() }),
        );
    });

    // The whole point of a code is that holding it is what lets somebody in.
    // A member who could read the collection would hold every code there is.
    it('a member cannot read an invitation', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(getDoc(invitationOf(db, CODE)));
    });

    it('an invitation cannot be given a later expiry once it is out', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(updateDoc(invitationOf(db, CODE), { expiresAt: inTenMinutes() }));
    });

    it('the administrator withdraws an invitation', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(deleteDoc(invitationOf(db, CODE)));
    });

    it('the code somebody holds is theirs alone to write and to read', async () => {
        const mine = testEnv.authenticatedContext(STRANGER).firestore();
        const other = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(setDoc(claimOf(mine, STRANGER), { code: CODE }));
        await assertSucceeds(getDoc(claimOf(mine, STRANGER)));
        await assertFails(getDoc(claimOf(other, STRANGER)));
        await assertFails(setDoc(claimOf(other, STRANGER), { code: CODE }));
    });

    // The rules build a document path out of what is written here, so what is
    // written here is held to the shape of a code.
    it('a claim that is not a code is refused', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(setDoc(claimOf(db, STRANGER), { code: 'not/a/code' }));
        await assertFails(setDoc(claimOf(db, STRANGER), { code: 'short' }));
        await assertFails(setDoc(claimOf(db, STRANGER), { code: CODE, note: 'anything' }));
    });
});

describe('joining a group', () => {
    const join = (db, roster) => updateDoc(groupDoc(db), { members: roster });

    it('somebody holding a live code adds themselves to the roster', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertSucceeds(join(db, [ALICE, BOB, STRANGER]));
    });

    // The one that decides how the application writes it: a person outside the
    // group cannot read the group document, so they cannot send the roster
    // they are adding themselves to. Asking the server to append instead
    // leaves the rule comparing the same two lists.
    it('somebody joining need not know who is already on the roster', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(groupDoc(db)));
        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertSucceeds(updateDoc(groupDoc(db), { members: arrayUnion(STRANGER) }));
    });

    it('somebody holding no code at all is refused', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(join(db, [ALICE, BOB, STRANGER]));
    });

    it('a code nobody ever made is refused', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: OTHER_CODE }));
        await assertFails(join(db, [ALICE, BOB, STRANGER]));
    });

    it('a code that has run out is refused', async () => {
        await plantAnAdministrator();
        await plantAnInvitation(CODE, new Date('2020-01-01T00:00:00Z'));

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db, [ALICE, BOB, STRANGER]));
    });

    // A code is handed to one person, and it adds that person. Otherwise
    // whoever it reached could put the whole of their address book in.
    it('the code adds the person holding it and nobody else', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db, [ALICE, BOB, 'somebody-else']));
        await assertFails(join(db, [ALICE, BOB, STRANGER, 'somebody-else']));
    });

    it('joining removes nobody', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db, [ALICE, STRANGER]));
    });

    // Joining is joining. Coming in as somebody who may then change the
    // roster is not what the code was handed over for.
    it('joining does not make somebody an administrator', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(
            setDoc(groupDoc(db), { members: [ALICE, BOB, STRANGER], admins: [ALICE, STRANGER] }),
        );
    });
});

describe('running a group', () => {
    it('the administrator takes somebody off the roster', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(groupDoc(db), { members: [ALICE], admins: [ALICE] }));
    });

    it('the administrator hands the group to another member', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(groupDoc(db), { members: [ALICE, BOB], admins: [BOB] }));
    });

    it('a member who does not run the group changes nothing', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(setDoc(groupDoc(db), { members: [BOB], admins: [BOB] }));
    });

    // A group whose administrators have all gone is one nobody can add to or
    // remove from, which is the console again.
    it('the last administrator cannot leave nobody in charge', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(groupDoc(db), { members: [ALICE, BOB], admins: [] }));
    });

    // Leaving the group has to take the power with it, or somebody outside
    // the group goes on running it.
    it('an administrator has to be on the roster', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(groupDoc(db), { members: [BOB], admins: [ALICE] }));
    });
});

// A group named after its founder, run by the founder and one more
// administrator, with one member who runs nothing. The founder owns the
// project and sees all of it anyway; the other administrator does not, and
// what they may do to the roster is narrower.
describe('running a group somebody founded', () => {
    const CAROL = 'carol-uid';
    const DAVE = 'dave-uid';
    const founded = (db) => doc(db, `groups/${ALICE}`);

    beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(founded(context.firestore()), {
                members: [ALICE, BOB, CAROL, DAVE],
                admins: [ALICE, BOB, CAROL],
            });
        });
    });

    it('the founder takes another administrator off the roster in one write', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(founded(db), { members: [ALICE, BOB, DAVE], admins: [ALICE, BOB] }),
        );
    });

    it('an administrator takes off a member who runs nothing', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(
            setDoc(founded(db), { members: [ALICE, BOB, CAROL], admins: [ALICE, BOB, CAROL] }),
        );
    });

    it('an administrator makes a member an administrator', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(updateDoc(founded(db), { admins: [ALICE, BOB, CAROL, DAVE] }));
    });

    // Two writes rather than one, so that a tap in the wrong row costs a role
    // and not a place in the group.
    it('an administrator takes another administrator off in two steps', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(
            setDoc(founded(db), { members: [ALICE, BOB, DAVE], admins: [ALICE, BOB] }),
        );
        await assertSucceeds(updateDoc(founded(db), { admins: [ALICE, BOB] }));
        await assertSucceeds(updateDoc(founded(db), { members: [ALICE, BOB, DAVE] }));
    });

    it('an administrator cannot take the founder off the roster', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(founded(db), { admins: [BOB, CAROL] }));
        await assertFails(
            setDoc(founded(db), { members: [BOB, CAROL, DAVE], admins: [BOB, CAROL] }),
        );
    });

    it('an administrator steps down on their own', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(updateDoc(founded(db), { admins: [ALICE, CAROL] }));
    });
});

// Where a false signal is known to put the phones of this group. The list is
// in the database rather than in the build, because which places those are is
// a fact about one household's surroundings.
describe('false places', () => {
    const falsePlaceOf = (db, id = 'a-false-place') =>
        doc(db, `groups/${GROUP}/falsePlaces/${id}`);

    const somePlace = {
        latitude: 61.43,
        longitude: 4.42,
        radiusMetres: 4000,
        named: 'a place a false signal uses',
    };

    const plantAPlace = () =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(falsePlaceOf(context.firestore()), somePlace);
        });

    it('a member reads the places', async () => {
        await plantAPlace();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(getDoc(falsePlaceOf(db)));
        await assertSucceeds(getDocs(collection(db, `groups/${GROUP}/falsePlaces`)));
    });

    it('somebody outside the group reads nothing', async () => {
        await plantAPlace();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(falsePlaceOf(db)));
    });

    it('an administrator names a place', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(falsePlaceOf(db), somePlace));
    });

    // A place added wrongly is ground the group's phones can no longer be
    // placed on, so naming one is an administrator's business.
    it('a member who does not run the group names none', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(setDoc(falsePlaceOf(db), somePlace));
    });

    it('a place outside the globe is refused', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(falsePlaceOf(db), { ...somePlace, latitude: 91 }));
        await assertFails(setDoc(falsePlaceOf(db), { ...somePlace, longitude: -181 }));
    });

    // A radius of nothing catches nothing, and one of half a country catches
    // the whole household.
    it('a radius has to be a radius', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(falsePlaceOf(db), { ...somePlace, radiusMetres: 0 }));
        await assertFails(setDoc(falsePlaceOf(db), { ...somePlace, radiusMetres: 50001 }));
    });

    it('nothing else goes in the document', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(falsePlaceOf(db), { ...somePlace, seenAt: new Date() }));
    });

    it('an administrator takes a place off the list, and a member does not', async () => {
        await plantAnAdministrator();
        await plantAPlace();

        const bob = testEnv.authenticatedContext(BOB).firestore();
        const alice = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(deleteDoc(falsePlaceOf(bob)));
        await assertSucceeds(deleteDoc(falsePlaceOf(alice)));
    });
});

// What the group has settled about how its phones judge an answer: today the
// limits of the rule that reads the shape of a run of answers.
describe('settings', () => {
    const settingOf = (db, key = 'circling') => doc(db, `groups/${GROUP}/settings/${key}`);

    const someLimits = {
        answers: 8,
        withinSeconds: 120,
        roundness: 0.02,
        widestMetres: 3000,
        tightestMetres: 500,
        steadiness: 0.08,
        turnedDegrees: 90,
        slowestMps: 10,
    };

    const plantTheLimits = () =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(settingOf(context.firestore()), someLimits);
        });

    it('every member reads the limits their phone weighs answers against', async () => {
        await plantTheLimits();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(getDoc(settingOf(db)));
    });

    it('somebody outside the group reads nothing', async () => {
        await plantTheLimits();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(settingOf(db)));
    });

    it('an administrator narrows the limits, and a member does not', async () => {
        await plantAnAdministrator();

        const alice = testEnv.authenticatedContext(ALICE).firestore();
        const bob = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(setDoc(settingOf(bob), someLimits));
        await assertSucceeds(setDoc(settingOf(alice), someLimits));
    });

    // Three points lie on a circle exactly, so a run of three has nothing left
    // over to disagree with one.
    it('a run too short to disagree with a circle is refused', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(settingOf(db), { ...someLimits, answers: 3 }));
    });

    it('a limit that is not a number is refused', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(settingOf(db), { ...someLimits, roundness: 'tight' }));
        await assertFails(setDoc(settingOf(db), { ...someLimits, widestMetres: 0 }));
    });

    it('nothing else goes in the document', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(settingOf(db), { ...someLimits, note: 'why' }));
    });

    // One document per subject, and the rules know the subjects: a setting
    // nobody reads is a setting nobody has reasoned about.
    it('a setting the phones do not read is refused', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(settingOf(db, 'whatever'), someLimits));
    });
});

// Everything this database holds that is not part of the household.
//
// A set of rules is the whole of what its database allows: what no `match`
// names is refused, and nothing under `groups` reaches outside it. That is
// worth checking rather than trusting, because the mistake it guards against
// is a quiet one — a `match` written one level further out, or carrying
// `{document=**}`, hands every other collection in the project to whoever is
// in the household, and every test above would still pass.
describe('the rest of the database', () => {
    const OUTSIDE = 'notes/kept-by-hand';
    const DEEPER_OUT = 'ledger/2026/entries/january';
    const NO_RULE_NAMES_IT = `groups/${GROUP}/secrets/one`;
    const ANOTHER_GROUP = 'neighbours';

    const plantWhatIsNotOurs = () =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(doc(db, OUTSIDE), { note: 'nothing to do with this application' });
            await setDoc(doc(db, DEEPER_OUT), { amount: 1 });
            await setDoc(doc(db, NO_RULE_NAMES_IT), { note: 'a collection nobody wrote a rule for' });
            await setDoc(doc(db, `groups/${ANOTHER_GROUP}`), { members: [STRANGER] });
        });

    it('a member of the household reads nothing else in the project', async () => {
        await plantWhatIsNotOurs();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(getDoc(doc(db, OUTSIDE)));
        await assertFails(getDoc(doc(db, DEEPER_OUT)));
        await assertFails(getDocs(collection(db, 'notes')));
    });

    it('a member of the household writes nothing else in the project', async () => {
        await plantWhatIsNotOurs();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(setDoc(doc(db, OUTSIDE), { note: 'mine now' }));
        await assertFails(setDoc(doc(db, 'anything/at-all'), { note: 'mine now' }));
        await assertFails(deleteDoc(doc(db, OUTSIDE)));
    });

    // Running the group is power over the group and nothing besides.
    it('an administrator reaches no further out than a member', async () => {
        await plantAnAdministrator();
        await plantWhatIsNotOurs();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(getDoc(doc(db, OUTSIDE)));
        await assertFails(setDoc(doc(db, OUTSIDE), { note: 'mine now' }));
    });

    // Permissions do not run downhill in Firestore: a subcollection is reached
    // only through a rule that names it. Everything the household keeps has
    // one, and anything else put under a group stays shut.
    it('a collection under the group that no rule names is shut', async () => {
        await plantWhatIsNotOurs();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(getDoc(doc(db, NO_RULE_NAMES_IT)));
        await assertFails(setDoc(doc(db, NO_RULE_NAMES_IT), { note: 'mine now' }));
    });

    it('a group somebody is not in is shut to them', async () => {
        await plantWhatIsNotOurs();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(getDoc(doc(db, `groups/${ANOTHER_GROUP}`)));
        await assertFails(getDocs(collection(db, 'groups')));
    });

    it('a signed out client reaches nothing at all', async () => {
        await plantWhatIsNotOurs();

        const db = testEnv.unauthenticatedContext().firestore();

        await assertFails(getDoc(doc(db, OUTSIDE)));
        await assertFails(getDoc(doc(db, `groups/${GROUP}`)));
        await assertFails(setDoc(doc(db, OUTSIDE), { note: 'mine now' }));
    });
});
