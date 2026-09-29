// What the security rules let each side do: the rest.

import { describe, it } from 'node:test';

import { assertFails } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, plantAnAdministrator } from './helpers.mjs';

setUpRules();

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
