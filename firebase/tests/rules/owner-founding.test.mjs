// What the security rules let each side do: the first founding of a group is
// the project owner's alone (ADR-0038).
//
// The rules in the repository open founding to every account; a deployment
// binds it to the owner's address, and these tests run the rules bound the
// same way the setup page and tools/project.sh bind them.

import { readFileSync } from 'node:fs';
import { describe, it } from 'node:test';

import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';

import { rulesForOwner } from '../../../web/public/lib/owner-rules.js';
import { setUpRules, RULES_FILE, STRANGER, STRANGER_EMAIL, testEnv, foundGroup } from './helpers.mjs';

const OWNER = 'owner-uid';
const OWNER_EMAIL = 'owner@example.com';

setUpRules(rulesForOwner(readFileSync(RULES_FILE, 'utf8'), OWNER_EMAIL));

const found = (db, uid) => foundGroup(db, uid, { members: [uid], admins: [uid] });
const owner = (claims = {}) =>
    testEnv.authenticatedContext(OWNER, { email: OWNER_EMAIL, email_verified: true, ...claims })
        .firestore();

describe('founding in a project bound to its owner', () => {
    // Otherwise whoever learns the address of the site before the owner
    // founds the group founds it, and the owner is told the project has one.
    it('another account cannot found the group', async () => {
        const db = testEnv.authenticatedContext(
            STRANGER,
            { email: STRANGER_EMAIL, email_verified: true },
        ).firestore();

        await assertFails(found(db, STRANGER));
    });

    it('an account without an address in its token cannot', async () => {
        await assertFails(found(testEnv.authenticatedContext(STRANGER).firestore(), STRANGER));
    });

    it('the owner founds the group', async () => {
        await assertSucceeds(found(owner(), OWNER));
    });

    it('the owner writing the address in capitals is the same owner', async () => {
        await assertSucceeds(found(owner({ email: 'Owner@Example.COM' }), OWNER));
    });

    // An address the owner does not control must not stand for it.
    it('the owner address with the account not verified cannot found', async () => {
        await assertFails(found(owner({ email_verified: false }), OWNER));
    });
});
