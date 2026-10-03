// What the security rules let each side do: leaving a group.

import { describe, it } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { arrayRemove, deleteDoc, doc, getDoc, setDoc, updateDoc, writeBatch } from 'firebase/firestore';

import {
    setUpRules,
    ALICE,
    BOB,
    STRANGER,
    FOREVER,
    testEnv,
    groupDoc,
    grantOf,
    memberOf,
    positionOf,
    positionFrom,
    someGrant,
    someProfile,
    watchOf,
    GROUP,
} from './helpers.mjs';

setUpRules();

// The group as each test needs it, written past the rules the way the console
// writes it.
const plant = (group) =>
    testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(groupDoc(context.firestore()), group);
    });

describe('leaving a group', () => {
    it('a member takes themselves off the roster', async () => {
        await plant({ members: [ALICE, BOB], admins: [ALICE] });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(updateDoc(groupDoc(db), { members: arrayRemove(BOB) }));
    });

    it('an administrator who is not the last leaves the roster and the administrators together', async () => {
        await plant({ members: [ALICE, BOB, STRANGER], admins: [ALICE, BOB], owner: ALICE });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(
            updateDoc(groupDoc(db), { members: arrayRemove(BOB), admins: arrayRemove(BOB) }),
        );
    });

    it('an administrator cannot stay among the administrators after leaving the roster', async () => {
        await plant({ members: [ALICE, BOB], admins: [ALICE, BOB], owner: ALICE });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(groupDoc(db), { members: arrayRemove(BOB) }));
    });

    it('the founder leaves while another administrator runs the group', async () => {
        await plant({ members: [ALICE, BOB], admins: [ALICE, BOB], owner: ALICE });
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            updateDoc(groupDoc(db), { members: arrayRemove(ALICE), admins: arrayRemove(ALICE) }),
        );
    });

    it('the only administrator cannot leave', async () => {
        await plant({ members: [ALICE, BOB], admins: [ALICE], owner: ALICE });
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            updateDoc(groupDoc(db), { members: arrayRemove(ALICE), admins: arrayRemove(ALICE) }),
        );
    });

    it('the last administrator cannot leave a group whose founder is gone', async () => {
        await plant({ members: [BOB, STRANGER], admins: [BOB], owner: ALICE });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(
            updateDoc(groupDoc(db), { members: arrayRemove(BOB), admins: arrayRemove(BOB) }),
        );
    });

    it('a member leaves a group nobody runs', async () => {
        await plant({ members: [ALICE, BOB] });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(updateDoc(groupDoc(db), { members: arrayRemove(BOB) }));
    });

    it('leaving takes nobody else along', async () => {
        await plant({ members: [ALICE, BOB, STRANGER], admins: [ALICE] });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(groupDoc(db), { members: [ALICE] }));
    });

    it('nobody leaves on somebody else\'s behalf', async () => {
        await plant({ members: [ALICE, BOB], admins: [ALICE] });
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(updateDoc(groupDoc(db), { members: arrayRemove(BOB) }));
    });

    it('leaving touches nothing but the two lists', async () => {
        await plant({ members: [ALICE, BOB], admins: [ALICE], name: 'Hike' });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(groupDoc(db), { members: arrayRemove(BOB), name: 'Mine' }));
    });

    it('a member takes their own documents and themselves out in one batch', async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(groupDoc(db), { members: [ALICE, BOB], admins: [ALICE] });
            await setDoc(memberOf(db, BOB), someProfile('Bob'));
            await setDoc(grantOf(db, BOB), someGrant({ [ALICE]: FOREVER }));
            await setDoc(positionOf(db, BOB), positionFrom(BOB));
        });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(
            writeBatch(db)
                .delete(positionOf(db, BOB))
                .delete(grantOf(db, BOB))
                .delete(memberOf(db, BOB))
                .update(groupDoc(db), { members: arrayRemove(BOB) })
                .commit(),
        );
    });

    // The batch the application sends: every document the member wrote into
    // the group, for each of their devices, some of which were never made.
    it('an administrator leaves with everything they wrote, in the batch the app sends', async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(groupDoc(db), { members: [ALICE, BOB], admins: [ALICE, BOB] });
            await setDoc(memberOf(db, BOB), someProfile('Bob'));
            await setDoc(grantOf(db, BOB), someGrant({ [ALICE]: FOREVER }));
            await setDoc(positionOf(db, BOB), positionFrom(BOB));
            await setDoc(watchOf(db, BOB), { memberId: BOB, since: new Date() });
            await setDoc(doc(db, `groups/${GROUP}/devices/${BOB}`), { phone: { model: 'X' } });
        });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(
            writeBatch(db)
                .delete(positionOf(db, BOB))
                .delete(watchOf(db, BOB))
                .delete(positionOf(db, BOB, 'tablet'))
                .delete(watchOf(db, BOB, 'tablet'))
                .delete(grantOf(db, BOB))
                .delete(doc(db, `groups/${GROUP}/devices/${BOB}`))
                .delete(memberOf(db, BOB))
                .delete(doc(db, `groups/${GROUP}/labels/${BOB}`))
                .update(groupDoc(db), { members: arrayRemove(BOB), admins: arrayRemove(BOB) })
                .commit(),
        );
    });
});

// Somebody an administrator took off the roster: Bob, who had let Alice watch
// him for good, and who can no longer withdraw it.
describe('a member taken off the roster', () => {
    const devicesOf = (db, uid) => doc(db, `groups/${GROUP}/devices/${uid}`);

    // Alice an ordinary member, the group run by somebody else.
    const runByAnother = { members: [ALICE, STRANGER], admins: [STRANGER] };

    const plantBobTakenOff = () =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(groupDoc(db), { members: [ALICE], admins: [ALICE] });
            await setDoc(memberOf(db, BOB), someProfile('Bob'));
            await setDoc(grantOf(db, BOB), someGrant({ [ALICE]: FOREVER }));
            await setDoc(positionOf(db, BOB), positionFrom(BOB));
            await setDoc(devicesOf(db, BOB), { phone: { model: 'X' } });
        });

    it('is watched by nobody, whatever they once allowed', async () => {
        await plantBobTakenOff();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(groupDoc(context.firestore()), runByAnother);
        });
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(getDoc(positionOf(db, BOB)));
        await assertFails(getDoc(grantOf(db, BOB)));
    });

    it('has their phones shown to nobody but an administrator', async () => {
        await plantBobTakenOff();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(groupDoc(context.firestore()), runByAnother);
        });

        const alice = testEnv.authenticatedContext(ALICE).firestore();
        const administrator = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(devicesOf(alice, BOB)));
        await assertSucceeds(getDoc(devicesOf(administrator, BOB)));
    });

    it('takes away everything they wrote about themselves', async () => {
        await plantBobTakenOff();
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(
            writeBatch(db)
                .delete(positionOf(db, BOB))
                .delete(grantOf(db, BOB))
                .delete(devicesOf(db, BOB))
                .delete(memberOf(db, BOB))
                .commit(),
        );
    });

    it('takes away nothing of anybody else', async () => {
        await plantBobTakenOff();
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(positionOf(db, ALICE), positionFrom(ALICE));
            await setDoc(grantOf(db, ALICE), someGrant({}));
        });
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(deleteDoc(positionOf(db, ALICE)));
        await assertFails(deleteDoc(grantOf(db, ALICE)));
        await assertFails(deleteDoc(memberOf(db, ALICE)));
    });

    it('writes nothing more', async () => {
        await plantBobTakenOff();
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(setDoc(positionOf(db, BOB), positionFrom(BOB)));
        await assertFails(updateDoc(grantOf(db, BOB), { sharedWith: {} }));
    });
});
