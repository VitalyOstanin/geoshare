// The small readings and results of tools/project.sh, one task per run:
//
//   node tools/project/cli.mjs <task> [--option value]...
//
// Tasks that read an answer of the firebase command line or of a Google API
// take it on standard input; everything else comes as options. Each prints
// what the shell used to get from its own inline program, in the same form,
// so the script reads it the way it always has.
import { readFileSync, writeFileSync } from 'node:fs';
import { parseArgs } from 'node:util';

import {
    appIds,
    appSettings,
    certificateHashes,
    chosenSite,
    consoleLink,
    defaultAccount,
    googleSignInOn,
    listedShaHashes,
    storedToken,
} from './firebase.mjs';
import { createResult, deleteResult, showResult, updateResult } from './results.mjs';
import { rulesForOwner } from '../../web/public/lib/owner-rules.js';

const input = () => readFileSync(0, 'utf8');
const lines = (values) => values.forEach((value) => console.log(value));
const readStore = (file) => JSON.parse(readFileSync(file, 'utf8'));
const pretty = (value) => console.log(JSON.stringify(value, null, 2));

const tasks = {
    'sha-list': () => lines(listedShaHashes(input())),
    'app-ids': () => lines(appIds(input())),
    'certificates': () => lines(certificateHashes(input())),
    'google-on': () => console.log(googleSignInOn(input())),
    'site': () => console.log(chosenSite(input())),
    'app-settings': ({ site }) => pretty(appSettings(input(), site)),

    // Two lines: the token, then the seconds it has left.
    'access-token': ({ store, account }) => {
        const { token, secondsLeft } = storedToken(readStore(store), account, Date.now());

        lines([token, secondsLeft]);
    },
    // --account when it was given; the store is read only when it was not.
    'owner-email': ({ store, account }) =>
        console.log(account || defaultAccount(readStore(store))),
    'console-link': ({ url, who }) => console.log(consoleLink(url[0], who)),

    'create-result': ({ project, site, step, url }) => pretty(createResult({
        projectId: project,
        site,
        steps: step.map((what, i) => ({ what, url: url[i] })),
    })),
    'show-result': ({ project, site, google, step, url, usage }) => pretty(showResult({
        projectId: project,
        site,
        googleOn: google === '1',
        googleStep: { what: step[0], url: url[0] },
        usage: usage ? JSON.parse(usage) : undefined,
    })),
    'update-result': ({ project, site }) =>
        console.log(JSON.stringify(updateResult({ projectId: project, site }))),
    'delete-result': ({ project }) =>
        console.log(JSON.stringify(deleteResult({ projectId: project, now: Date.now() }))),

    // Binds the founding of the group to the project owner in a copy of the
    // rules, in place.
    'bind-rules': ({ file, owner }) =>
        writeFileSync(file, rulesForOwner(readFileSync(file, 'utf8'), owner)),
};

const { positionals, values } = parseArgs({
    allowPositionals: true,
    options: {
        account: { type: 'string', default: '' },
        file: { type: 'string' },
        google: { type: 'string' },
        owner: { type: 'string' },
        project: { type: 'string' },
        site: { type: 'string' },
        // --step and --url come in pairs, one pair per manual step.
        step: { type: 'string', multiple: true, default: [] },
        store: { type: 'string' },
        url: { type: 'string', multiple: true, default: [] },
        usage: { type: 'string', default: '' },
        who: { type: 'string', default: '' },
    },
});

const task = tasks[positionals[0]];

if (!task) {
    console.error(`tools/project/cli.mjs: no task "${positionals[0] ?? ''}"`);
    process.exit(2);
}

try {
    task(values);
} catch (error) {
    console.error(error.message);
    process.exit(1);
}
