import assert from 'node:assert/strict';
import { test } from 'node:test';

import { configProblem } from '../public/lib/config-check.js';

const CONFIG = {
    projectId: 'family',
    appId: '1:2:web:3',
    apiKey: 'key',
    authDomain: 'family.firebaseapp.com',
};
const EMULATORS = { auth: 'http://127.0.0.1:9099', firestore: { host: '127.0.0.1', port: 8080 } };

test('a whole configuration starts the page', () => {
    assert.equal(configProblem(CONFIG), null);
    assert.equal(configProblem({ ...CONFIG, projectId: 'demo-family', emulators: EMULATORS }), null);
});

test('a missing field is named, with the script that writes the file', () => {
    assert.equal(
        configProblem({ projectId: CONFIG.projectId, appId: CONFIG.appId }),
        'firebase-config.js has no apiKey, authDomain; tools/web-config.sh writes it',
    );
    assert.match(configProblem({ ...CONFIG, projectId: '' }), /no projectId/u);
    assert.match(configProblem(undefined), /no projectId, appId, apiKey, authDomain/u);
});

test('emulators beside a real project are refused', () => {
    assert.equal(
        configProblem({ ...CONFIG, emulators: EMULATORS }),
        'emulators are only for a demo- project, not family',
    );
});
