// What the security rules let each side do: asking a phone to forget its place.

import { describe, it, beforeEach } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, groupDoc } from './helpers.mjs';

setUpRules();

describe('resets', () => {
    const CAROL = 'carol-uid';
    const BOBS_PHONE = `${BOB}.phone`;
    const CAROLS_TABLET = `${CAROL}.tablet`;
    const resetOf = (db, id) => doc(db, `groups/${GROUP}/resets/${id}`);
    const minutesAgo = (minutes) => new Date(Date.now() - minutes * 60 * 1000);

    // What an asking screen writes when it asks a phone now.
    const askedBy = (uid) => ({ askedBy: uid, askedAt: serverTimestamp() });

    // A request as it stands in the database, made some minutes ago.
    const plantARequest = (id, by = ALICE) =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(resetOf(context.firestore(), id), {
                askedBy: by,
                askedAt: minutesAgo(1),
            });
        });

    beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(groupDoc(context.firestore()), {
                members: [ALICE, BOB, CAROL],
                admins: [ALICE],
            });
        });
    });

    it('an administrator asks a phone of anybody in the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(resetOf(db, BOBS_PHONE), askedBy(ALICE)));
        await assertSucceeds(getDoc(resetOf(db, BOBS_PHONE)));
    });

    it('a member asks their own other phone without being an administrator', async () => {
        const db = testEnv.authenticatedContext(CAROL).firestore();

        await assertSucceeds(setDoc(resetOf(db, CAROLS_TABLET), askedBy(CAROL)));
        await assertSucceeds(getDoc(resetOf(db, CAROLS_TABLET)));
    });

    it('a member who runs nothing cannot ask somebody else, nor see that anybody did', async () => {
        await plantARequest(BOBS_PHONE);

        const db = testEnv.authenticatedContext(CAROL).firestore();

        await assertFails(setDoc(resetOf(db, BOBS_PHONE), askedBy(CAROL)));
        await assertFails(getDoc(resetOf(db, BOBS_PHONE)));
    });

    it('nobody asks a phone of somebody outside the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(resetOf(db, `${STRANGER}.phone`), askedBy(ALICE)));
    });

    it('somebody outside the group reaches no request, their own id or not', async () => {
        await plantARequest(BOBS_PHONE);

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(resetOf(db, BOBS_PHONE)));
        await assertFails(setDoc(resetOf(db, `${STRANGER}.phone`), askedBy(STRANGER)));
    });

    it('a request names who asked and the server time, and nothing else', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(resetOf(db, BOBS_PHONE), askedBy(CAROL)));
        await assertFails(
            setDoc(resetOf(db, BOBS_PHONE), { askedBy: ALICE, askedAt: new Date() }),
        );
        await assertFails(setDoc(resetOf(db, BOBS_PHONE), { ...askedBy(ALICE), place: 'home' }));
    });

    it('the phone says it has forgotten, and only the phone', async () => {
        await plantARequest(BOBS_PHONE);

        const bob = testEnv.authenticatedContext(BOB).firestore();
        const alice = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(updateDoc(resetOf(alice, BOBS_PHONE), { doneAt: serverTimestamp() }));
        await assertFails(updateDoc(resetOf(bob, BOBS_PHONE), { doneAt: minutesAgo(5) }));
        await assertFails(
            updateDoc(resetOf(bob, BOBS_PHONE), { doneAt: serverTimestamp(), askedBy: BOB }),
        );
        await assertSucceeds(updateDoc(resetOf(bob, BOBS_PHONE), { doneAt: serverTimestamp() }));
    });

    it('a new request replaces what the last one left', async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(resetOf(context.firestore(), BOBS_PHONE), {
                askedBy: ALICE,
                askedAt: minutesAgo(30),
                doneAt: minutesAgo(29),
            });
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(resetOf(db, BOBS_PHONE), askedBy(ALICE)));
    });

    it('a request is not deleted, only made again', async () => {
        await plantARequest(BOBS_PHONE);

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(deleteDoc(resetOf(db, BOBS_PHONE)));
    });
});
