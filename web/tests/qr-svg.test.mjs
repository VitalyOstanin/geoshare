import assert from 'node:assert/strict';
import { test } from 'node:test';

import { qrPath, qrSvg } from '../public/lib/qr-svg.js';

/** Just enough of a document for qrSvg: elements that keep their attributes and children. */
const fakeDocument = {
    createElementNS: (namespace, name) => ({
        namespace,
        name,
        attributes: {},
        children: [],
        setAttribute(key, value) {
            this.attributes[key] = String(value);
        },
        append(...nodes) {
            this.children.push(...nodes);
        },
    }),
};

test('each run of dark modules in a row is one rectangle', () => {
    const matrix = [
        [true, true, false, true],
        [false, false, false, false],
        [false, true, true, true],
    ];

    assert.equal(qrPath(matrix), 'M0 0h2v1h-2zM3 0h1v1h-1zM1 2h3v1h-3z');
    assert.equal(qrPath(matrix, 4), 'M4 4h2v1h-2zM7 4h1v1h-1zM5 6h3v1h-3z');
});

test('the code sits in a quiet zone of four modules, finder pattern in the corner', () => {
    const svg = qrSvg(fakeDocument, 'https://example-try-a2.web.app/join');
    const [background, modules] = svg.children;
    const side = Number(svg.attributes.viewBox.split(' ')[2]);
    const d = modules.attributes.d;

    // Version 3 holds this link at level M: 29 modules a side.
    assert.equal(side, 29 + 8);
    assert.equal(background.attributes.width, String(side));
    // The finder pattern's top edge, 7 modules long, starts right after the
    // quiet zone, in the top-left and the top-right corner.
    assert.ok(d.startsWith('M4 4h7v1h-7z'), d.slice(0, 40));
    assert.ok(d.includes(`M${side - 4 - 7} 4h7v1h-7z`));
    // Its bottom edge is the seventh row; the eighth row is the separator.
    assert.ok(d.includes('M4 10h7v1h-7z'));
    assert.ok(!d.includes('M4 11h'));
});
