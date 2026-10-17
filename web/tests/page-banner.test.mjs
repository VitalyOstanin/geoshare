// The parts of the page that draw into the document, run against a document
// small enough to write here: the banner over a failure nothing caught and the
// window both helps open. Only what they put in the tree and what a click does
// is checked; how it looks is for a browser.

import assert from 'node:assert/strict';
import { test } from 'node:test';
import { setImmediate as nextTurn } from 'node:timers/promises';

import { noteInLog, pageLog } from '../public/lib/page-log.js';

class Node_ {
    constructor(tag) {
        this.tag = tag;
        this.children = [];
        this.listeners = {};
        this.attributes = {};
        this.className = '';
        this.textContent = '';
        this.parent = null;
        this.open = false;
    }

    append(...items) {
        for (const item of items) {
            if (typeof item === 'string') {
                const text = new Node_('#text');

                text.textContent = item;
                this.children.push(text);
            } else {
                item.parent = this;
                this.children.push(item);
            }
        }
    }

    remove() {
        this.parent?.children.splice(this.parent.children.indexOf(this), 1);
        this.parent = null;
    }

    setAttribute(name, value) {
        this.attributes[name] = value;
    }

    addEventListener(type, listener) {
        this.listeners[type] = listener;
    }

    click(event = {}) {
        this.listeners.click({ target: this, ...event });
    }

    showModal() {
        this.open = true;
    }

    close() {
        this.open = false;
    }

    getBoundingClientRect() {
        return { left: 10, right: 110, top: 10, bottom: 110 };
    }

    find(predicate) {
        if (predicate(this)) {
            return this;
        }
        for (const child of this.children) {
            const found = child.find(predicate);

            if (found) {
                return found;
            }
        }

        return null;
    }
}

const fakeDocument = {
    body: new Node_('body'),
    createElement: (tag) => new Node_(tag),
    getElementById(id) {
        return this.body.find((node) => node.id === id);
    },
};

// The banner script wires itself up on load, so the document and the target
// for its listeners have to be there first.
const heard = {};

globalThis.document = fakeDocument;
globalThis.addEventListener = (type, listener) => {
    heard[type] = listener;
};
await import('../public/lib/unexpected.js');

const { openHelpTable } = await import('../public/lib/help-table.js');
const { openModeHelp } = await import('../public/lib/mode-help.js');
const { openReasonHelp } = await import('../public/lib/reason-help.js');

const banner = () => fakeDocument.getElementById('unexpected');
const byClass = (root, name) => root.find((node) => node.className === name);

test('an unhandled rejection draws a banner with the details folded away', () => {
    heard.unhandledrejection({ reason: new Error('boom') });

    assert.equal(banner().attributes.role, 'alert');
    assert.equal(byClass(banner(), 'unexpected-details').hidden, true);
    assert.ok(byClass(banner(), 'unexpected-details').find((node) => node.tag === 'pre')
        .textContent.includes('boom'));
});

test('a second failure replaces the banner instead of stacking another', () => {
    heard.unhandledrejection({ reason: new Error('first') });
    heard.unhandledrejection({ reason: new Error('second') });

    assert.equal(fakeDocument.body.children.filter((node) => node.id === 'unexpected').length, 1);
    assert.ok(byClass(banner(), 'unexpected-details').find((node) => node.tag === 'pre')
        .textContent.includes('second'));
});

test('the details button opens and folds the details', () => {
    heard.unhandledrejection({ reason: new Error('boom') });

    const more = byClass(banner(), 'unexpected-more');

    more.click();
    assert.equal(byClass(banner(), 'unexpected-details').hidden, false);
    assert.equal(more.attributes['aria-expanded'], 'true');
    assert.ok(more.textContent.startsWith('▾'));
    more.click();
    assert.equal(byClass(banner(), 'unexpected-details').hidden, true);
    assert.ok(more.textContent.startsWith('▸'));
});

test('the cross takes the banner down and the reload button reloads', () => {
    let reloaded = false;

    globalThis.location = { reload: () => { reloaded = true; } };
    heard.unhandledrejection({ reason: new Error('boom') });
    byClass(banner(), 'unexpected-reload').click();
    assert.equal(reloaded, true);
    byClass(banner(), 'unexpected-close').click();
    assert.equal(banner(), null);
    delete globalThis.location;
});

test('the copy button puts the details on the clipboard and says so', async () => {
    const written = [];
    const original = Object.getOwnPropertyDescriptor(globalThis, 'navigator');

    Object.defineProperty(globalThis, 'navigator', {
        configurable: true,
        value: { userAgent: '', clipboard: { writeText: async (text) => written.push(text) } },
    });
    try {
        heard.unhandledrejection({ reason: new Error('boom') });
        const copy = byClass(banner(), 'unexpected-copy');

        copy.click();
        await nextTurn();
        assert.equal(written.length, 1);
        assert.ok(written[0].includes('boom'));
        assert.equal(copy.textContent, 'Copied');
    } finally {
        Object.defineProperty(globalThis, 'navigator', original);
    }
});

test('a clipboard that refuses leaves the button as it was and warns', async () => {
    const original = Object.getOwnPropertyDescriptor(globalThis, 'navigator');
    const warnings = [];
    const warn = console.warn;

    Object.defineProperty(globalThis, 'navigator', {
        configurable: true,
        value: { userAgent: '', clipboard: { writeText: async () => { throw new Error('no'); } } },
    });
    console.warn = (...args) => warnings.push(args);
    try {
        heard.unhandledrejection({ reason: new Error('boom') });
        const copy = byClass(banner(), 'unexpected-copy');

        copy.click();
        await nextTurn();
        assert.equal(copy.textContent, 'Copy');
        assert.equal(warnings.length, 1);
    } finally {
        console.warn = warn;
        Object.defineProperty(globalThis, 'navigator', original);
    }
});

test('an error event draws a banner with where it happened', () => {
    heard.error({ error: new Error('bad'), filename: 'https://x.test/app.js', lineno: 7 });

    assert.ok(byClass(banner(), 'unexpected-details').find((node) => node.tag === 'pre')
        .textContent.includes('app.js:7'));
});

test('an error event with nothing in it draws nothing', () => {
    banner()?.remove();
    heard.error({});

    assert.equal(banner(), null);
});

test('an error event with only a message is reported without a place', () => {
    heard.error({ message: 'Script error.' });

    assert.ok(banner());
});

test('what a banner reports is kept in the page log', () => {
    heard.unhandledrejection({ reason: new Error('kept for the report') });

    assert.ok(pageLog('https://x.test').some((line) => line.includes('kept for the report')));
});

test('the log writes an error by its name and message, an object as JSON, a cycle as text', () => {
    const cycle = {};

    cycle.self = cycle;
    noteInLog('warn', [new TypeError('bad type'), { a: 1 }, cycle, null], new Date(0));

    const line = pageLog('https://x.test').at(-1);

    assert.ok(line.includes('TypeError: bad type {"a":1} [object Object] null'), line);
});

const help = {
    title: 'Title',
    intro: 'Intro',
    headings: ['One', 'Two'],
    rows: [{ name: 'Row', cells: ['a', 'b'] }, { name: 'Other', cells: ['c', 'd'] }],
};

test('a help window holds the title, the introduction and a row for each name', () => {
    const dialog = openHelpTable(help, fakeDocument);

    assert.equal(dialog.open, true);
    assert.equal(dialog.parent, fakeDocument.body);

    const table = dialog.find((node) => node.tag === 'table');
    const rows = table.children;

    assert.equal(rows.length, 3);
    assert.deepEqual(rows[0].children.map((cell) => cell.textContent), ['', 'One', 'Two']);
    assert.deepEqual(rows[2].children.map((cell) => cell.textContent), ['Other', 'c', 'd']);
    dialog.remove();
});

test('the cross, the escape key and a click on the dark each take the window down', () => {
    for (const close of [
        (dialog) => dialog.find((node) => node.tag === 'button').click(),
        (dialog) => dialog.listeners.cancel(),
        (dialog) => dialog.click({ clientX: 500, clientY: 500 }),
    ]) {
        const dialog = openHelpTable(help, fakeDocument);

        close(dialog);
        assert.equal(dialog.open, false);
        assert.equal(dialog.parent, null);
    }
});

test('a click on the window itself or on something in it leaves it open', () => {
    const dialog = openHelpTable(help, fakeDocument);

    dialog.click({ clientX: 50, clientY: 50 });
    dialog.click({ target: dialog.children[1], clientX: 500, clientY: 500 });
    assert.equal(dialog.open, true);
    dialog.remove();
});

test('the mode and reason helps open as help windows', () => {
    for (const open of [openModeHelp, openReasonHelp]) {
        const dialog = open(fakeDocument);

        assert.equal(dialog.className, 'mode-help');
        assert.ok(dialog.find((node) => node.tag === 'table').children.length > 1);
        dialog.remove();
    }
});
