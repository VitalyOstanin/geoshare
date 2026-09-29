// What the security rules let each side do: positions.

import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, getDoc, getDocs, query, setDoc, updateDoc, where } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, positionOf, grantOf, LONG_OVER, someGrant, somePosition, positionFrom } from './helpers.mjs';

setUpRules();

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
