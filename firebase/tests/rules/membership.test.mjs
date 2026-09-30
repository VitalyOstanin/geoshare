// What the security rules let each side do: membership.

import { describe, it } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, getDoc, arrayUnion, setDoc, updateDoc } from 'firebase/firestore';

import { setUpRules, ALICE, BOB, STRANGER, STRANGER_EMAIL, testEnv, CODE, OTHER_CODE, groupDoc, invitationOf, claimOf, inTenMinutes, plantAnAdministrator, plantAnInvitation, plantAnAddressedInvitation, signedInAs } from './helpers.mjs';

setUpRules();

describe('membership', () => {
    // The roster of the household that predates invitations: nobody runs it,
    // so nobody may edit it, and a phone that could would be a phone that can
    // add itself to the group.
    it('a member cannot add anybody to the group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(groupDoc(db), { members: [ALICE, BOB, STRANGER] }));
    });

    // A group is named after the account that made it, which is the one name
    // its founder may write.
    it('somebody signed in makes a group with themselves alone in it', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(
            setDoc(groupDoc(db, STRANGER), { members: [STRANGER], admins: [STRANGER] }),
        );
    });

    // Otherwise one account could fill the database with groups nobody is in,
    // a document apiece and each of them free to make.
    it("a group cannot be given a name of its founder's choosing", async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, 'a-new-group'), { members: [STRANGER], admins: [STRANGER] }),
        );
    });

    it("a group cannot be made under somebody else's name", async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, ALICE), { members: [STRANGER], admins: [STRANGER] }),
        );
    });

    // Otherwise a group could be conjured around somebody who never asked to
    // be in one, and their phone would start writing into it.
    it('a group cannot be made with somebody else already in it', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, STRANGER), { members: [STRANGER, ALICE], admins: [STRANGER] }),
        );
    });

    it('a group cannot be made with somebody else running it', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(
            setDoc(groupDoc(db, STRANGER), { members: [STRANGER], admins: [ALICE] }),
        );
    });

    it('nobody deletes a group', async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(deleteDoc(groupDoc(db)));
    });
});

describe('invitations', () => {
    it('the administrator makes an invitation', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(
            setDoc(invitationOf(db, CODE), {
                createdBy: ALICE,
                expiresAt: inTenMinutes(),
                email: STRANGER_EMAIL,
            }),
        );
    });

    // ADR-0019: an invitation without an address would admit whoever it was
    // forwarded to, which is what the address is there to stop.
    it('an invitation without an address is refused', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            setDoc(invitationOf(db, CODE), { createdBy: ALICE, expiresAt: inTenMinutes() }),
        );
    });

    it('an invitation to something that is not an address is refused', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();
        const invitation = (email) => ({ createdBy: ALICE, expiresAt: inTenMinutes(), email });

        await assertFails(setDoc(invitationOf(db, CODE), invitation('stranger')));
        await assertFails(setDoc(invitationOf(db, CODE), invitation('a b@example.com')));
        await assertFails(setDoc(invitationOf(db, CODE), invitation('stranger@example')));
    });

    it('a member who does not run the group makes none', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(
            setDoc(invitationOf(db, CODE), { createdBy: BOB, expiresAt: inTenMinutes() }),
        );
    });

    // The whole point of a code is that holding it is what lets somebody in.
    // A member who could read the collection would hold every code there is.
    it('a member cannot read an invitation', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(getDoc(invitationOf(db, CODE)));
    });

    it('an invitation cannot be given a later expiry once it is out', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(updateDoc(invitationOf(db, CODE), { expiresAt: inTenMinutes() }));
    });

    it('the administrator withdraws an invitation', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(deleteDoc(invitationOf(db, CODE)));
    });

    it('the code somebody holds is theirs alone to write and to read', async () => {
        const mine = testEnv.authenticatedContext(STRANGER).firestore();
        const other = testEnv.authenticatedContext(BOB).firestore();

        await assertSucceeds(setDoc(claimOf(mine, STRANGER), { code: CODE }));
        await assertSucceeds(getDoc(claimOf(mine, STRANGER)));
        await assertFails(getDoc(claimOf(other, STRANGER)));
        await assertFails(setDoc(claimOf(other, STRANGER), { code: CODE }));
    });

    // The rules build a document path out of what is written here, so what is
    // written here is held to the shape of a code.
    it('a claim that is not a code is refused', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(setDoc(claimOf(db, STRANGER), { code: 'not/a/code' }));
        await assertFails(setDoc(claimOf(db, STRANGER), { code: 'short' }));
        await assertFails(setDoc(claimOf(db, STRANGER), { code: CODE, note: 'anything' }));
    });
});

describe('joining a group', () => {
    const join = (db, roster) => updateDoc(groupDoc(db), { members: roster });

    it('somebody holding a live code adds themselves to the roster', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertSucceeds(join(db, [ALICE, BOB, STRANGER]));
    });

    // The one that decides how the application writes it: a person outside the
    // group cannot read the group document, so they cannot send the roster
    // they are adding themselves to. Asking the server to append instead
    // leaves the rule comparing the same two lists.
    it('somebody joining need not know who is already on the roster', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(groupDoc(db)));
        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertSucceeds(updateDoc(groupDoc(db), { members: arrayUnion(STRANGER) }));
    });

    it('somebody holding no code at all is refused', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(join(db, [ALICE, BOB, STRANGER]));
    });

    it('a code nobody ever made is refused', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: OTHER_CODE }));
        await assertFails(join(db, [ALICE, BOB, STRANGER]));
    });

    it('a code that has run out is refused', async () => {
        await plantAnAdministrator();
        await plantAnInvitation(CODE, new Date('2020-01-01T00:00:00Z'));

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db, [ALICE, BOB, STRANGER]));
    });

    // A code is handed to one person, and it adds that person. Otherwise
    // whoever it reached could put the whole of their address book in.
    it('the code adds the person holding it and nobody else', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db, [ALICE, BOB, 'somebody-else']));
        await assertFails(join(db, [ALICE, BOB, STRANGER, 'somebody-else']));
    });

    it('joining removes nobody', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db, [ALICE, STRANGER]));
    });

    // Joining is joining. Coming in as somebody who may then change the
    // roster is not what the code was handed over for.
    it('joining does not make somebody an administrator', async () => {
        await plantAnAdministrator();
        await plantAnInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(
            setDoc(groupDoc(db), { members: [ALICE, BOB, STRANGER], admins: [ALICE, STRANGER] }),
        );
    });
});

// ADR-0015: the code admits the account it was made out to, and only once
// Google has confirmed that the address is that account's.
describe('joining on an invitation made out to an address', () => {
    const join = (db) => updateDoc(groupDoc(db), { members: arrayUnion(STRANGER) });

    it('the account the invitation names joins', async () => {
        await plantAnAdministrator();
        await plantAnAddressedInvitation();

        const db = signedInAs(STRANGER_EMAIL);

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertSucceeds(join(db));
    });

    // Google hands addresses back in whatever case they were typed in.
    it('the address is compared without regard to case', async () => {
        await plantAnAdministrator();
        await plantAnAddressedInvitation('Stranger@Example.com');

        const db = signedInAs(STRANGER_EMAIL);

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertSucceeds(join(db));
    });

    // The link was forwarded: the code is right, the person is not.
    it('another account holding the same code is refused', async () => {
        await plantAnAdministrator();
        await plantAnAddressedInvitation();

        const db = signedInAs('somebody-else@example.com');

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db));
    });

    it('an address Google has not confirmed is refused', async () => {
        await plantAnAdministrator();
        await plantAnAddressedInvitation();

        const db = signedInAs(STRANGER_EMAIL, false);

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db));
    });

    it('an account without an address at all is refused', async () => {
        await plantAnAdministrator();
        await plantAnAddressedInvitation();

        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(setDoc(claimOf(db, STRANGER), { code: CODE }));
        await assertFails(join(db));
    });
});

// ADR-0016: a caption people choose, beside the account-id name the document has.
describe('the name of a group', () => {
    it('a group is founded with a name', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertSucceeds(
            setDoc(groupDoc(db, STRANGER), {
                members: [STRANGER],
                admins: [STRANGER],
                name: 'Hikers',
            }),
        );
    });

    it('a name that is blank or longer than a link carries is refused', async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();
        const founded = (name) => ({ members: [STRANGER], admins: [STRANGER], name });

        await assertFails(setDoc(groupDoc(db, STRANGER), founded('   ')));
        await assertFails(setDoc(groupDoc(db, STRANGER), founded('x'.repeat(61))));
        await assertFails(setDoc(groupDoc(db, STRANGER), founded(42)));
    });

    it('the administrator renames the group', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(updateDoc(groupDoc(db), { name: 'Hikers' }));
        await assertSucceeds(updateDoc(groupDoc(db), { name: 'Hikers of the north' }));
    });

    it('a member who does not run the group cannot rename it', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(BOB).firestore();

        await assertFails(updateDoc(groupDoc(db), { name: 'Hikers' }));
    });

    // A rename is a rename: the roster checks live in their own write.
    it('renaming cannot carry a change to the roster with it', async () => {
        await plantAnAdministrator();

        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(
            updateDoc(groupDoc(db), { name: 'Hikers', members: [ALICE, BOB, STRANGER] }),
        );
    });
});
