import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    APP_PACKAGE,
    RELEASES_URL,
    buildIntentUrl,
    isAndroid,
    joinCaption,
    parseInvitation,
    siteFromHost,
} from '../public/lib/join.js';
import { JOIN_EN } from '../public/lib/words/join-en.js';

const HOST = 'imaginary-group.web.app';

test('a host on web.app shows its site without the domain', () => {
    assert.equal(siteFromHost(HOST), 'imaginary-group');
});

// The emulator and a custom domain are not guessed at: shown as they stand
// rather than mangled into something that looks like a site but is not.
test('a host that is not a web.app address is shown as it stands', () => {
    assert.equal(siteFromHost('127.0.0.1:8080'), '127.0.0.1:8080');
});

test('a link with no fragment carries no invitation', () => {
    assert.equal(parseInvitation(''), null);
    assert.equal(parseInvitation('#'), null);
});

test('a fragment with neither a group nor a code is the project owner\'s link', () => {
    assert.equal(parseInvitation('#utm_source=chat'), null);
});

test('an invitation is read out of the fragment, hash included or not', () => {
    const withHash = parseInvitation('#g=imaginary-group&c=ABCDE&by=Alice&n=Household&to=al…@x.com');
    const withoutHash = parseInvitation('g=imaginary-group&c=ABCDE&by=Alice&n=Household&to=al…@x.com');

    assert.deepEqual(withHash, withoutHash);
    assert.deepEqual(withHash, {
        groupId: 'imaginary-group',
        code: 'ABCDE',
        by: 'Alice',
        name: 'Household',
        to: 'al…@x.com',
    });
});

// The names are captions from a link nobody signed, so they are trimmed and
// cut short rather than trusted whole.
test('a caption longer than 60 characters is cut to 60', () => {
    const long = 'x'.repeat(90);
    const invitation = parseInvitation(`#g=g&c=c&by=${long}`);

    assert.equal(invitation.by.length, 60);
});

test('a caption is trimmed of the whitespace around it', () => {
    const invitation = parseInvitation('#g=g&c=c&by=%20Alice%20');

    assert.equal(invitation.by, 'Alice');
});

test('a plain link asks the viewer to connect, nothing more', () => {
    assert.deepEqual(joinCaption(JOIN_EN, null), { title: JOIN_EN.joinTitle, invitedFor: null });
});

test('an invitation with both names is captioned with both', () => {
    const invitation = { groupId: 'g', code: 'c', by: 'Alice', name: 'Household', to: '' };

    assert.deepEqual(joinCaption(JOIN_EN, invitation), {
        title: JOIN_EN.joinInvited('Alice', 'Household'),
        invitedFor: null,
    });
});

test('an invitation missing the inviter still names the group', () => {
    const invitation = { groupId: 'g', code: 'c', by: '', name: 'Household', to: '' };

    assert.equal(joinCaption(JOIN_EN, invitation).title, JOIN_EN.joinInvitedNoBy('Household'));
});

test('an invitation missing the group still names the inviter', () => {
    const invitation = { groupId: 'g', code: 'c', by: 'Alice', name: '', to: '' };

    assert.equal(joinCaption(JOIN_EN, invitation).title, JOIN_EN.joinInvitedNoGroup('Alice'));
});

test('an invitation with neither name falls back to a bare title', () => {
    const invitation = { groupId: 'g', code: 'c', by: '', name: '', to: '' };

    assert.equal(joinCaption(JOIN_EN, invitation).title, JOIN_EN.joinInvitedBare);
});

test('the invited address is a second line, only where the link carries one', () => {
    const invitation = { groupId: 'g', code: 'c', by: '', name: '', to: 'al…@x.com' };

    assert.equal(joinCaption(JOIN_EN, invitation).invitedFor, JOIN_EN.joinInvitedFor('al…@x.com'));
});

test('the intent link carries the fields of the invitation as a query', () => {
    const invitation = { groupId: 'imaginary-group', code: 'ABCDE', by: 'Alice', name: '', to: '' };
    const url = buildIntentUrl(HOST, invitation, RELEASES_URL);

    assert.equal(
        url,
        `intent://${HOST}/join?g=imaginary-group&c=ABCDE&by=Alice#Intent;scheme=https;`
            + `package=${APP_PACKAGE};`
            + `S.browser_fallback_url=${encodeURIComponent(RELEASES_URL)};end`,
    );
});

test('a plain link with no invitation carries no query at all', () => {
    const url = buildIntentUrl(HOST, null, RELEASES_URL);

    assert.equal(
        url,
        `intent://${HOST}/join#Intent;scheme=https;package=${APP_PACKAGE};`
            + `S.browser_fallback_url=${encodeURIComponent(RELEASES_URL)};end`,
    );
});

test('only Chrome on Android is offered the intent link', () => {
    assert.equal(isAndroid('Mozilla/5.0 (Linux; Android 14) Chrome/128 Mobile'), true);
    assert.equal(isAndroid('Mozilla/5.0 (Macintosh) Chrome/128'), false);
    assert.equal(isAndroid(undefined), false);
});
