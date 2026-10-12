// What the security rules let each side do: places and settings.

import { describe, it } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, plantAnAdministrator } from './helpers.mjs';

setUpRules();

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

    // A place added wrongly is ground the group's devices can no longer be
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

// What the group has settled about how its devices judge an answer: today the
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
// The group looking for one device of a member: an administrator opens a
// search, and until it is called off or runs out, everybody in the group sees
// where that device is, whatever the member's own permissions say. Alice runs
// the group, Bob's device is looked for, and Carol is in the group without a
// permission from Bob.
