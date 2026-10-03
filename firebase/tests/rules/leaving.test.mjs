// What the security rules let each side do: leaving a group.

import { describe, it } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { arrayRemove, doc, setDoc, updateDoc, writeBatch } from 'firebase/firestore';

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
