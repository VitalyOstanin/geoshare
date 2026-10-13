// What a page does with a failure nothing in it caught: a banner
// over everything that says so, offers a reload, and holds the details for
// the person to pass on. Loaded by every page on its own, ahead of the page's
// own module, so the listeners are there before anything else runs.
//
// Its words live here rather than in a page's dictionary: the map page, the
// join page and the setup page each have their own, and this is the one part
// all three share.

import { browserOf, faultOf } from './fault.js';
import { spokenLanguage } from './language.js';
import { keepConsole, noteInLog } from './page-log.js';
import { PAGE_VERSION } from './version.js';

export const NEW_ISSUE = 'https://github.com/VitalyOstanin/geoshare/issues/new';

const WORDS = {
    en: {
        title: 'Unexpected error',
        reload: 'Reload',
        close: 'Close',
        details: 'Details',
        copy: 'Copy',
        copied: 'Copied',
        rejection: 'Unhandled rejection',
        exception: 'Exception',
        page: 'page',
        hintBefore: 'If it keeps happening, pass the details to whoever set up the group, or ',
        hintLink: 'open an issue',
        hintAfter: ' on GitHub. Addresses, names and account ids are left out of the details.',
        warning:
            'Issues on GitHub are public. Add nothing private to one: no addresses, names, places, ' +
            "invitation links or the group's site.",
    },
    ru: {
        title: 'Непредвиденная ошибка',
        reload: 'Обновить',
        close: 'Закрыть',
        details: 'Подробнее',
        copy: 'Скопировать',
        copied: 'Скопировано',
        rejection: 'Необработанный отказ',
        exception: 'Исключение',
        page: 'страница',
        hintBefore: 'Если ошибка повторяется, передайте подробности тому, кто завёл группу, или ',
        hintLink: 'создайте issue',
        hintAfter: ' на GitHub. Адреса, имена и id аккаунтов в подробности не попадают.',
        warning:
            'Issues на GitHub видны всем. Не добавляйте в него ничего личного: почту, имена, места, ' +
            'ссылки-приглашения и адрес сайта группы.',
    },
};

/** The details as one block of text, the way they are shown, copied and reported. */
export function detailsText(fault, words, { now = new Date(), agent = '', language = 'en' } = {}) {
    const [browser, system] = browserOf(agent);

    return [
        words[fault.kind],
        fault.message,
        ...(fault.place ? [fault.place] : []),
        `${now.toLocaleString(language)} · ${words.page} ${PAGE_VERSION}`,
        `${browser} · ${system} · ${language}`,
    ].join('\n');
}

/**
 * The address of a new issue with the details in its body, under the same
 * headings as the bug report template, and the warning first. The body is
 * always written in English: the issues are.
 */
export function issueAddress(details) {
    const body = [
        `> ${WORDS.en.warning}`,
        '',
        `**Version:** page ${PAGE_VERSION}`,
        '',
        '**What happened:**',
        '',
        '```',
        details,
        '```',
        '',
        '**What you expected:**',
        '',
        '**Steps to repeat it:**',
        '',
    ].join('\n');

    return `${NEW_ISSUE}?${new URLSearchParams({ title: 'Unexpected error on the page', body })}`;
}

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (text !== undefined) {
        node.textContent = text;
    }

    return node;
}

function button(className, text, onClick) {
    const node = element('button', className, text);
    node.type = 'button';
    node.addEventListener('click', onClick);

    return node;
}

/** The part under «Details»: the text, the copy button, what to do next. */
function detailsPart(words, details) {
    const part = element('div', 'unexpected-details');
    const copy = button('unexpected-copy', words.copy, async () => {
        try {
            await navigator.clipboard.writeText(details);
            copy.textContent = words.copied;
        } catch (error) {
            console.warn('copying the details:', error);
        }
    });
    const hint = element('p', 'unexpected-hint', words.hintBefore);
    const link = element('a', '', words.hintLink);
    link.href = issueAddress(details);
    link.target = '_blank';
    link.rel = 'noopener';
    hint.append(link, words.hintAfter);

    part.hidden = true;
    part.append(element('pre', '', details), copy, hint, element('p', 'unexpected-warning', words.warning));

    return part;
}

/** Draws the banner for [details], or refills the one already drawn. */
function show(words, details) {
    document.getElementById('unexpected')?.remove();

    const banner = element('div', 'unexpected');
    banner.id = 'unexpected';
    banner.setAttribute('role', 'alert');

    const head = element('div', 'unexpected-head');
    const close = button('unexpected-close', '×', () => banner.remove());
    close.title = words.close;
    close.setAttribute('aria-label', words.close);
    head.append(
        element('b', '', words.title),
        button('unexpected-reload', words.reload, () => globalThis.location.reload()),
        close,
    );

    const part = detailsPart(words, details);
    const more = button('unexpected-more', `▸ ${words.details}`, () => {
        part.hidden = !part.hidden;
        more.textContent = `${part.hidden ? '▸' : '▾'} ${words.details}`;
        more.setAttribute('aria-expanded', String(!part.hidden));
    });
    more.setAttribute('aria-expanded', 'false');

    banner.append(head, more, part);
    document.body.append(banner);
}

/** Listens for what nothing else caught, for as long as the page is open. */
export function catchUnexpected(target = globalThis) {
    const language = spokenLanguage();
    const words = WORDS[language] ?? WORDS.en;
    const report = (fault) => {
        noteInLog(fault.kind, [fault.message, ...(fault.place ? [fault.place] : [])]);
        show(words, detailsText(fault, words, { agent: navigator.userAgent, language }));
    };

    target.addEventListener('unhandledrejection', (event) => {
        report(faultOf({ kind: 'rejection', reason: event.reason }));
    });
    target.addEventListener('error', (event) => {
        // A script or a stylesheet that failed to load raises an error event
        // with no error in it; the page shows its own word for those.
        if (event.error === undefined && !event.message) {
            return;
        }
        const place = event.filename ? `${event.filename}:${event.lineno}` : null;
        report(faultOf({ kind: 'exception', reason: event.error ?? event.message, place }));
    });
}

if (globalThis.document) {
    keepConsole();
    catchUnexpected();
}
