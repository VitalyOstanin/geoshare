// What the security rules let each side do: sirens.

import { describe, it, beforeEach } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, groupDoc } from './helpers.mjs';

setUpRules();

describe('sirens', () => {
    const CAROL = 'carol-uid';
    const BOBS_PHONE = `${BOB}.phone`;
    const CAROLS_TABLET = `${CAROL}.tablet`;
    const sirenOf = (db, id) => doc(db, `groups/${GROUP}/sirens/${id}`);
    const minutesAgo = (minutes) => new Date(Date.now() - minutes * 60 * 1000);

    // What a caller's screen writes when it rings a device now.
    const calledBy = (uid) => ({ calledBy: uid, calledAt: serverTimestamp() });

    // A call as it stands in the database, made some minutes ago.
    const plantACall = (id, by = ALICE) =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(sirenOf(context.firestore(), id), {
                calledBy: by,
                calledAt: minutesAgo(1),
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

    it('an administrator rings a phone of anybody in the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(sirenOf(db, BOBS_PHONE), calledBy(ALICE)));
        await assertSucceeds(getDoc(sirenOf(db, BOBS_PHONE)));
    });

    it('a member rings their own other phone without being an administrator', async () => {
        const db = testEnv.authenticatedContext(CAROL).firestore();

        await assertSucceeds(setDoc(sirenOf(db, CAROLS_TABLET), calledBy(CAROL)));
        await assertSucceeds(getDoc(sirenOf(db, CAROLS_TABLET)));
    });

    it('a member who runs nothing cannot ring somebody else, nor see that anybody did', async () => {
        await plantACall(BOBS_PHONE);

        const db = testEnv.authenticatedContext(CAROL).firestore();

        await assertFails(setDoc(sirenOf(db, BOBS_PHONE), calledBy(CAROL)));
        await assertFails(getDoc(sirenOf(db, BOBS_PHONE)));
    });

    it('nobody rings a phone of somebody outside the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(sirenOf(db, `${STRANGER}.phone`), calledBy(ALICE)));
    });

    it('somebody outside the group reaches no siren, their own id or not', async () => {
        await plantACall(BOBS_PHONE);

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(sirenOf(db, BOBS_PHONE)));
        await assertFails(setDoc(sirenOf(db, `${STRANGER}.phone`), calledBy(STRANGER)));
    });

    it('a call names its caller and the server time, and nothing else', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(sirenOf(db, BOBS_PHONE), calledBy(CAROL)));
        await assertFails(
            setDoc(sirenOf(db, BOBS_PHONE), { calledBy: ALICE, calledAt: new Date() }),
        );
        await assertFails(
            setDoc(sirenOf(db, BOBS_PHONE), { ...calledBy(ALICE), seconds: 600 }),
        );
    });

    it('the phone says it heard the call, and only the phone', async () => {
        await plantACall(BOBS_PHONE);

        const bob = testEnv.authenticatedContext(BOB).firestore();
        const alice = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(updateDoc(sirenOf(alice, BOBS_PHONE), { heardAt: serverTimestamp() }));
        await assertFails(updateDoc(sirenOf(bob, BOBS_PHONE), { heardAt: minutesAgo(5) }));
        await assertFails(
            updateDoc(sirenOf(bob, BOBS_PHONE), { heardAt: serverTimestamp(), calledBy: BOB }),
        );
        await assertSucceeds(updateDoc(sirenOf(bob, BOBS_PHONE), { heardAt: serverTimestamp() }));
    });

    it('the phone itself or an administrator stops it early, and nobody else', async () => {
        const stopBy = (uid) => ({ stoppedBy: uid, stoppedAt: serverTimestamp() });
        const carol = testEnv.authenticatedContext(CAROL).firestore();
        const bob = testEnv.authenticatedContext(BOB).firestore();
        const alice = testEnv.authenticatedContext(ALICE).firestore();

        await plantACall(BOBS_PHONE);
        await assertFails(updateDoc(sirenOf(carol, BOBS_PHONE), stopBy(CAROL)));
        await assertFails(updateDoc(sirenOf(bob, BOBS_PHONE), stopBy(ALICE)));
        await assertSucceeds(updateDoc(sirenOf(bob, BOBS_PHONE), stopBy(BOB)));

        await plantACall(BOBS_PHONE);
        await assertSucceeds(updateDoc(sirenOf(alice, BOBS_PHONE), stopBy(ALICE)));
    });

    it('a new call replaces what the last one left', async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(sirenOf(context.firestore(), BOBS_PHONE), {
                calledBy: ALICE,
                calledAt: minutesAgo(30),
                heardAt: minutesAgo(30),
                stoppedBy: BOB,
                stoppedAt: minutesAgo(29),
            });
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(sirenOf(db, BOBS_PHONE), calledBy(ALICE)));
    });

    it('a siren is not deleted, only called again', async () => {
        await plantACall(BOBS_PHONE);

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(deleteDoc(sirenOf(db, BOBS_PHONE)));
    });
});
