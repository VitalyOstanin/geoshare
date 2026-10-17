import { test } from 'node:test';
import assert from 'node:assert/strict';

import { releaseToken } from '../public/lib/setup-token.js';

function fixture(revoke) {
    const warnings = [];

    return {
        state: { token: 'token-1', api: {} },
        oauth2: { revoke },
        warn: (...args) => warnings.push(args),
        warnings,
    };
}

test('revokes the token and clears the token and the api', () => {
    const revoked = [];
    const { state, oauth2, warn } = fixture((token) => revoked.push(token));

    releaseToken(state, oauth2, warn);

    assert.deepEqual(revoked, ['token-1']);
    assert.equal(state.token, null);
    assert.equal(state.api, null);
});

test('does nothing when no token is held', () => {
    let calls = 0;
    const { state, oauth2, warn } = fixture(() => calls++);

    state.token = null;
    releaseToken(state, oauth2, warn);

    assert.equal(calls, 0);
});

test('a second call does not revoke again', () => {
    let calls = 0;
    const { state, oauth2, warn } = fixture(() => calls++);

    releaseToken(state, oauth2, warn);
    releaseToken(state, oauth2, warn);

    assert.equal(calls, 1);
});

test('a refusal is logged and the state is still cleared', () => {
    const { state, oauth2, warn, warnings } = fixture((token, done) =>
        done({ error: 'invalid_token' }));

    releaseToken(state, oauth2, warn);

    assert.equal(warnings.length, 1);
    assert.equal(state.token, null);
});

test('a throwing revoke is logged and the state is still cleared', () => {
    const { state, oauth2, warn, warnings } = fixture(() => {
        throw new Error('blocked');
    });

    releaseToken(state, oauth2, warn);

    assert.equal(warnings.length, 1);
    assert.equal(state.token, null);
    assert.equal(state.api, null);
});
