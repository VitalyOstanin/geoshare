import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import { rulesForOwner } from '../public/lib/owner-rules.js';

const RULES = readFileSync(new URL('../../firebase/firestore.rules', import.meta.url), 'utf8');

test('the owner replaces the marker with a check on the verified address', () => {
    const bound = rulesForOwner(RULES, 'Owner@Example.com');

    assert.ok(!bound.includes('@project-owner'));
    assert.ok(bound.includes("request.auth.token.email.lower() == 'owner@example.com'"));
    assert.ok(bound.includes('request.auth.token.email_verified == true'));
    assert.equal(bound.split('\n').length, RULES.split('\n').length + 1);
});

test('only the marker line changes', () => {
    const bound = rulesForOwner(RULES, 'owner@example.com');
    const before = RULES.split('\n').filter((line) => !line.includes('@project-owner'));
    const after = bound.split('\n');

    for (const line of before) {
        assert.ok(after.includes(line), line);
    }
});

test('an address that could break out of the rules is refused', () => {
    const bad = ["a'b@example.com", 'a b@example.com', 'a\\@example.com', '', null, undefined, 'plain'];

    for (const address of bad) {
        assert.throws(() => rulesForOwner(RULES, address), /usable e-mail/, String(address));
    }
});

test('rules without the marker are refused, not deployed open', () => {
    assert.throws(() => rulesForOwner('rules_version = "2";', 'owner@example.com'), /marker/);
});
