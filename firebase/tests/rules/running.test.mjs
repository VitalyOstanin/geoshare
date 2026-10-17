// What the security rules let each side do: running.

import assert from 'node:assert/strict';
import { describe, it, beforeEach } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { arrayRemove, doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

import { setUpRules, ALICE, BOB, testEnv, groupDoc, plantAnAdministrator } from './helpers.mjs';

setUpRules();

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

// The household made in the console: its document carries a name, and the
// owner of the project is named in it rather than in the path.
describe('running a group whose owner is named in it', () => {
    const CAROL = 'carol-uid';
    const household = (db) => doc(db, 'groups/household');

    beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(household(context.firestore()), {
                name: 'household',
                members: [ALICE, BOB, CAROL],
                admins: [BOB],
                owner: ALICE,
            });
        });
    });

    it('the owner runs the group without being named among its administrators', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(updateDoc(household(db), { members: [ALICE, BOB] }));
    });

    it('an administrator writes the roster of a group that has a name', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(updateDoc(household(db), { members: [ALICE, BOB] }));
    });

    it('nobody writes the owner from a client', async () => {
        const owner = testEnv.authenticatedContext(ALICE).firestore();
        const administrator = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(household(owner), { owner: BOB }));
        await assertFails(updateDoc(household(administrator), { owner: BOB }));
    });

    // ADR-0016: the name is a caption the administrators keep, the console's
    // one included; the owner above is what decides anything.
    it('an administrator renames the household', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(updateDoc(household(db), { name: 'elsewhere' }));
    });

    it('an administrator cannot take the owner off the roster', async () => {
        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(household(db), { members: [BOB, CAROL] }));
    });

    // Nobody named, and the group is still run: by its owner.
    it('the owner leaves no administrator named and still runs the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(updateDoc(household(db), { admins: [] }));
        await assertSucceeds(updateDoc(household(db), { members: [ALICE, BOB] }));
    });
});

// Two administrators writing at the same moment: each write is checked
// against the roster as it stands when it lands, so the pair cannot leave the
// group with nobody to run it.
describe('two administrators writing at once', () => {
    const OWNER = 'owner-uid';
    const household = (db) => doc(db, 'groups/household');

    // The owner is off the roster, or the rules would let the group stand
    // with nobody named and the race would have nothing to show.
    beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(household(context.firestore()), {
                name: 'household',
                members: [ALICE, BOB],
                admins: [ALICE, BOB],
                owner: OWNER,
            });
        });
    });

    // The rules-free context hands back nothing of its own, so the list is
    // carried out of it.
    const adminsAfter = async () => {
        let admins;
        await testEnv.withSecurityRulesDisabled(async (context) => {
            admins = (await getDoc(household(context.firestore()))).data().admins;
        });
        return admins;
    };

    it('each taking the other off leaves one administrator', async () => {
        const alice = testEnv.authenticatedContext(ALICE).firestore();
        const bob = testEnv.authenticatedContext(BOB).firestore();

        const outcomes = await Promise.allSettled([
            updateDoc(household(alice), { admins: arrayRemove(BOB) }),
            updateDoc(household(bob), { admins: arrayRemove(ALICE) }),
        ]);

        assert.equal(outcomes.filter((o) => o.status === 'fulfilled').length, 1);
        assert.equal((await adminsAfter()).length, 1);
    });

    it('each resigning leaves the group with an administrator', async () => {
        const alice = testEnv.authenticatedContext(ALICE).firestore();
        const bob = testEnv.authenticatedContext(BOB).firestore();

        const outcomes = await Promise.allSettled([
            updateDoc(household(alice), { admins: arrayRemove(ALICE) }),
            updateDoc(household(bob), { admins: arrayRemove(BOB) }),
        ]);

        assert.equal(outcomes.filter((o) => o.status === 'fulfilled').length, 1);
        assert.ok((await adminsAfter()).length >= 1);
    });
});

// Where a false signal is known to put the devices of this group. The list is
// in the database rather than in the build, because which places those are is
// a fact about one household's surroundings.
