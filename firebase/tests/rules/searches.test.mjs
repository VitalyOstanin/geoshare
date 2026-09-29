// What the security rules let each side do: searches.

import { describe, it, beforeEach } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, positionOf, grantOf, groupDoc } from './helpers.mjs';

setUpRules();

describe('searches', () => {
    const CAROL = 'carol-uid';
    const BOBS_PHONE = `${BOB}.phone`;
    const searchOf = (db, id) => doc(db, `groups/${GROUP}/searches/${id}`);
    const devicesOf = (db, uid) => doc(db, `groups/${GROUP}/devices/${uid}`);
    const hoursAgo = (hours) => new Date(Date.now() - hours * 60 * 60 * 1000);

    // What an administrator's screen writes when they open a search now.
    const openedBy = (uid, hours = 24) => ({
        openedBy: uid,
        startedAt: serverTimestamp(),
        renewedBy: uid,
        renewedAt: serverTimestamp(),
        hours,
    });

    // A search as it stands in the database, opened some hours ago.
    const plantASearch = (hours, openedHoursAgo) =>
        testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(searchOf(context.firestore(), BOBS_PHONE), {
                openedBy: ALICE,
                startedAt: hoursAgo(openedHoursAgo),
                renewedBy: ALICE,
                renewedAt: hoursAgo(openedHoursAgo),
                hours,
            });
        });

    beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (context) => {
            const db = context.firestore();

            await setDoc(groupDoc(db), { members: [ALICE, BOB, CAROL], admins: [ALICE] });
            await setDoc(devicesOf(db, BOB), { phone: { model: 'MAR-LX1' } });
        });
    });

    // The search opens the one phone's position and nothing else of Bob's:
    // not his other phones, whom he shares with, or what phones he has.
    const carolReadsBob = async (expect) => {
        const db = testEnv.authenticatedContext(CAROL).firestore();

        await expect(getDoc(positionOf(db, BOB)));
        await assertFails(getDoc(positionOf(db, BOB, 'tablet')));
        await assertFails(getDoc(grantOf(db, BOB)));
        await assertFails(getDoc(devicesOf(db, BOB)));
    };

    it('without a search a member without a permission reads nothing of the place', async () => {
        await carolReadsBob(assertFails);
    });

    it('an administrator opens a search for a phone', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE, 6)));
        await assertSucceeds(setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE, 72)));
    });

    it('several phones are looked for at once, of one member or of several', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE)));
        await assertSucceeds(setDoc(searchOf(db, `${BOB}.tablet`), openedBy(ALICE)));
        await assertSucceeds(setDoc(searchOf(db, `${CAROL}.phone`), openedBy(ALICE)));
    });

    it('a search carries the name of the phone, and a short one', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(searchOf(db, BOBS_PHONE), { ...openedBy(ALICE), phoneName: 'New phone' }),
        );
        await assertFails(
            setDoc(searchOf(db, BOBS_PHONE), { ...openedBy(ALICE), phoneName: 'x'.repeat(101) }),
        );
        await assertFails(setDoc(searchOf(db, BOBS_PHONE), { ...openedBy(ALICE), phoneName: 7 }));
    });

    it('while the search stands everybody in the group reads the place', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE));

        await carolReadsBob(assertSucceeds);
    });

    it('everybody in the group sees who is looked for, and nobody outside', async () => {
        await plantASearch(24, 1);

        const carol = testEnv.authenticatedContext(CAROL).firestore();
        const stranger = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(getDoc(searchOf(carol, BOBS_PHONE)));
        await assertSucceeds(getDocs(collection(carol, `groups/${GROUP}/searches`)));
        await assertFails(getDoc(searchOf(stranger, BOBS_PHONE)));
    });

    it('a search that has run out opens nothing', async () => {
        await plantASearch(24, 25);

        await carolReadsBob(assertFails);
    });

    it('a member who does not run the group opens no search', async () => {
        const db = testEnv.authenticatedContext(CAROL).firestore();

        await assertFails(setDoc(searchOf(db, BOBS_PHONE), openedBy(CAROL)));
    });

    it('nobody is searched for who is not in the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(searchOf(db, `${STRANGER}.phone`), openedBy(ALICE)));
    });

    // A member taken off the roster is no longer the group's to look for,
    // even while a search opened before stands.
    it('a search stops opening the place once the member leaves', async () => {
        await plantASearch(24, 1);
        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(groupDoc(context.firestore()), {
                members: [ALICE, CAROL],
                admins: [ALICE],
            });
        });

        await carolReadsBob(assertFails);
    });

    it('a search lasts three days at most and at least an hour', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE, 73)));
        await assertFails(setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE, 0)));
        await assertFails(setDoc(searchOf(db, BOBS_PHONE), openedBy(ALICE, 1.5)));
    });

    // The end is counted from the server's clock: a renewal dated by the
    // administrator's phone would be worth whatever that phone's clock says.
    it("the moments come from the server's clock", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(searchOf(db, BOBS_PHONE), { ...openedBy(ALICE), renewedAt: new Date('2100-01-01') }),
        );
        await assertFails(
            setDoc(searchOf(db, BOBS_PHONE), { ...openedBy(ALICE), startedAt: hoursAgo(1) }),
        );
    });

    it('a search names the administrator who writes it and nothing else', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        const search = searchOf(db, BOBS_PHONE);

        await assertFails(setDoc(search, { ...openedBy(ALICE), openedBy: CAROL }));
        await assertFails(setDoc(search, { ...openedBy(ALICE), renewedBy: CAROL }));
        await assertFails(setDoc(search, { ...openedBy(ALICE), note: 'extra' }));
    });

    it('a renewal keeps who opened the search and when', async () => {
        await plantASearch(6, 5);

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            updateDoc(searchOf(db, BOBS_PHONE), {
                renewedBy: ALICE,
                renewedAt: serverTimestamp(),
                hours: 24,
            }),
        );
        await assertFails(
            updateDoc(searchOf(db, BOBS_PHONE), {
                startedAt: hoursAgo(100),
                renewedBy: ALICE,
                renewedAt: serverTimestamp(),
            }),
        );
        await assertFails(
            updateDoc(searchOf(db, BOBS_PHONE), {
                openedBy: BOB,
                renewedBy: ALICE,
                renewedAt: serverTimestamp(),
            }),
        );
    });

    it('a renewal moves the end on from now', async () => {
        await plantASearch(24, 25);
        await carolReadsBob(assertFails);

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await updateDoc(searchOf(db, BOBS_PHONE), {
            renewedBy: ALICE,
            renewedAt: serverTimestamp(),
            hours: 24,
        });

        await carolReadsBob(assertSucceeds);
    });

    it('a member who does not run the group renews nothing', async () => {
        await plantASearch(6, 5);

        const db = testEnv.authenticatedContext(CAROL).firestore();

        await assertFails(
            updateDoc(searchOf(db, BOBS_PHONE), {
                renewedBy: CAROL,
                renewedAt: serverTimestamp(),
                hours: 24,
            }),
        );
    });

    it('calling the search off closes the place at once', async () => {
        await plantASearch(24, 1);
        await carolReadsBob(assertSucceeds);

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(deleteDoc(searchOf(db, BOBS_PHONE)));
        await carolReadsBob(assertFails);
    });

    it('the member whose phone it is calls it off; another member does not', async () => {
        await plantASearch(24, 1);

        const carol = testEnv.authenticatedContext(CAROL).firestore();
        const bob = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(deleteDoc(searchOf(carol, BOBS_PHONE)));
        await assertSucceeds(deleteDoc(searchOf(bob, BOBS_PHONE)));
    });

    it('the member whose phone it is does not renew it', async () => {
        await plantASearch(6, 5);

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(
            updateDoc(searchOf(db, BOBS_PHONE), {
                renewedBy: BOB,
                renewedAt: serverTimestamp(),
                hours: 72,
            }),
        );
    });
});
