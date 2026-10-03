// What the security rules let each side do: access.

import { describe, it, beforeEach } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, serverTimestamp, setDoc } from 'firebase/firestore';

import { setUpRules, GROUP, ALICE, BOB, STRANGER, testEnv, watchOf, memberOf, grantOf, someProfile, FOREVER, LONG_OVER, someGrant, watchFrom, plantAnAdministrator } from './helpers.mjs';

setUpRules();

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
                photoUrl: `https://lh3.googleusercontent.com/${'a'.repeat(500)}`,
                updatedAt: new Date(),
            }),
        );
    });

    // Every member's phone fetches it: a picture on a server the member
    // chose would tell that server where each of them is online from.
    it('a picture kept anywhere but the account host is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        for (const photoUrl of [
            'https://example.com/me.png',
            'http://lh3.googleusercontent.com/a/example',
            'https://example.com/lh3.googleusercontent.com/a/example',
        ]) {
            await assertFails(
                setDoc(memberOf(db, ALICE), { displayName: 'Alice', photoUrl, updatedAt: new Date() }),
            );
        }
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

    // Nine phones written before, the tenth writing itself in: the write a
    // member with ten phones makes.
    it('the tenth phone, written as a phone writes itself, is taken', async () => {
        const nine = Object.fromEntries(
            Array.from({ length: 9 }, (unused, index) => [`phone-${index}`, { model: 'x' }]),
        );

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(devicesOf(context.firestore(), ALICE), nine);
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();
        const tenth = { ...someDevices.phone, name: 'kitchen', charging: true };

        await assertSucceeds(setDoc(devicesOf(db, ALICE), { 'phone-9': tenth }, { merge: true }));
    });

    // The last place of ten carries the wrong thing: every place is looked
    // at, not only the first.
    it('a phone carrying a field no phone writes is refused, wherever it stands', async () => {
        const nine = Object.fromEntries(
            Array.from({ length: 9 }, (unused, index) => [`phone-${index}`, { model: 'x' }]),
        );

        await testEnv.withSecurityRulesDisabled(async (context) => {
            await setDoc(devicesOf(context.firestore(), ALICE), nine);
        });

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(
                devicesOf(db, ALICE),
                { 'phone-9': { model: 'x', extra: 'y' } },
                { merge: true },
            ),
        );
    });

    it('a phone whose charge is not a number is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(devicesOf(db, ALICE), { phone: { batteryPercent: 'full' } }));
    });

    it('a phone named at greater length than a name is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(devicesOf(db, ALICE), { phone: { name: 'x'.repeat(101) } }));
    });

    it('an entry that is not a phone at all is refused', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(devicesOf(db, ALICE), { phone: 'x'.repeat(1000) }));
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
