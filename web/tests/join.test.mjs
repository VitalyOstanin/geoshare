import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    APP_PACKAGE,
    RELEASES_URL,
    buildIntentUrl,
    invitationLink,
    invitationTyped,
    invitationUntilText,
    isAndroid,
    isForeignSite,
    isWrongAccount,
    joinCaption,
    parseInvitation,
    shortenedAddress,
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

test('an invitation link reads back as the invitation it was made from', () => {
    const link = invitationLink({
        site: 'imaginary-group',
        groupId: 'Kx9aQ2',
        code: '7FK3M9QA',
        by: 'Анна Петрова',
        name: 'Походы & горы',
        email: 'ivan.petrov@example.com',
    });

    assert.deepEqual(parseInvitation(new URL(link).hash), {
        groupId: 'Kx9aQ2',
        code: '7FK3M9QA',
        by: 'Анна Петрова',
        name: 'Походы & горы',
        to: 'ivan…@example.com',
    });
});

// The same string the application's ConnectLinkTest expects, so the two
// clients hand out one link for one invitation.
test('captions that are missing are left out of the link', () => {
    assert.equal(
        invitationLink({
            site: 'geoshare-example-abc123',
            groupId: 'g',
            code: '7FK3M9QA',
            by: null,
            name: ' ',
            email: 'ivan.petrov@example.com',
        }),
        'https://geoshare-example-abc123.web.app/join#g=g&c=7FK3M9QA&to=ivan%E2%80%A6%40example.com',
    );
});

test('a short name keeps only its first character', () => {
    assert.equal(shortenedAddress('ivan@example.com'), 'i…@example.com');
    assert.equal(shortenedAddress('ivan.petrov@example.com'), 'ivan…@example.com');
});

test('when an invitation stops working is said as a day and a time', () => {
    const millis = Date.parse('2026-10-01T15:40:00');

    assert.equal(invitationUntilText(millis, 'ru'), '1 октября, 15:40');
    assert.equal(invitationUntilText(millis, 'en'), 'October 1, 15:40');
});

// invitationTyped reads back whatever was pasted into the join field: a
// link, the older bare form, or neither.
test('a pasted link reads back as an invitation with its site', () => {
    const pasted = 'https://imaginary-group.web.app/join#g=g&c=ABCDEFGH&by=Alice&to=al…@x.com';

    assert.deepEqual(invitationTyped(pasted), {
        site: 'imaginary-group',
        groupId: 'g',
        code: 'ABCDEFGH',
        by: 'Alice',
        name: '',
        to: 'al…@x.com',
    });
});

test('a pasted link is found among a messenger\'s own text around it', () => {
    const pasted = 'Заходи в группу:\n'
        + 'https://imaginary-group.web.app/join#g=g&c=ABCDEFGH.\n'
        + '(переслано из чата "Семья")';

    assert.deepEqual(invitationTyped(pasted), {
        site: 'imaginary-group',
        groupId: 'g',
        code: 'ABCDEFGH',
        by: '',
        name: '',
        to: '',
    });
});

// A link cut off right after its "#" still has the invitation in the query.
test('an empty fragment does not hide the invitation in the query', () => {
    assert.deepEqual(invitationTyped('https://imaginary-group.web.app/join?g=g&c=ABCDEFGH#'), {
        site: 'imaginary-group',
        groupId: 'g',
        code: 'ABCDEFGH',
        by: '',
        name: '',
        to: '',
    });
});

// The application reads both Hosting hosts and a slash after "join"; so
// does the page.
test('a pasted firebaseapp.com link with a slash after join is read too', () => {
    assert.deepEqual(invitationTyped('https://imaginary-group.firebaseapp.com/join/#g=g&c=ABCDEFGH'), {
        site: 'imaginary-group',
        groupId: 'g',
        code: 'ABCDEFGH',
        by: '',
        name: '',
        to: '',
    });
});

test('a pasted link with neither a group nor a code carries no invitation', () => {
    assert.equal(invitationTyped('https://imaginary-group.web.app/join?utm_source=chat'), null);
});

test('a pasted "group.code" reads back as an invitation with no site', () => {
    assert.deepEqual(invitationTyped('imaginary-group.ABCD2345'), {
        site: null,
        groupId: 'imaginary-group',
        code: 'ABCD2345',
        by: '',
        name: '',
        to: '',
    });
});

test('text that is neither a link nor "group.code" carries no invitation', () => {
    assert.equal(invitationTyped('thanks!'), null);
});

test('a link naming another site is foreign, unless the host is not one itself', () => {
    const invitation = invitationTyped('https://other-group.web.app/join#g=g&c=ABCDEFGH');

    assert.equal(isForeignSite(invitation, 'imaginary-group.web.app'), true);
    assert.equal(isForeignSite(invitation, 'other-group.web.app'), false);
    assert.equal(isForeignSite(invitation, '127.0.0.1:8080'), false);
});

test('the older "group.code" form names no site, so it is never foreign', () => {
    const invitation = invitationTyped('imaginary-group.ABCD2345');

    assert.equal(isForeignSite(invitation, 'other-group.web.app'), false);
});

test('an invitation for another address is a mismatch, case aside', () => {
    const invitation = invitationTyped(
        'https://imaginary-group.web.app/join#g=g&c=ABCDEFGH&to=ivan…@example.com',
    );

    assert.equal(isWrongAccount(invitation, 'alice.wonderland@example.com'), true);
    assert.equal(isWrongAccount(invitation, 'IVAN.PETROV@EXAMPLE.COM'), false);
});

test('nothing to compare either way is no mismatch', () => {
    const bare = invitationTyped('imaginary-group.ABCD2345');
    const named = invitationTyped(
        'https://imaginary-group.web.app/join#g=g&c=ABCDEFGH&to=ivan…@example.com',
    );

    assert.equal(isWrongAccount(bare, 'ivan.petrov@example.com'), false);
    assert.equal(isWrongAccount(named, undefined), false);
});
