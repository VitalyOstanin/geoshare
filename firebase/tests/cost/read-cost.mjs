// What each thing the clients do costs in reads, counted against the emulator.
//
// Run through tools/read-cost.sh. The household is the shape of the family the
// production project serves: three members, one of them with three devices, the
// other two sharing with the first only. Every device runs the subscriptions of
// the service; a map adds those of the screen.
//
// Two kinds of read are counted for every event:
//   - delivered: documents a subscription was handed from the server, which is
//     what a listener is billed for;
//   - by the rules: evaluations of get() and exists() in the rules, by the
//     document they reach, taken from the emulator's coverage report.
import { readFileSync } from 'node:fs';

import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import {
    collection,
    deleteDoc,
    disableNetwork,
    doc,
    enableNetwork,
    onSnapshot,
    serverTimestamp,
    setDoc,
} from 'firebase/firestore';

const PROJECT = 'demo-geoshare';
const HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
const G = 'family';
const FOREVER = new Date('9999-12-31T00:00:00Z');
// How long one event is given to reach every listener before the next one. At
// 700 ms the services' first snapshots still trickled into the event after.
const SETTLE_MS = 2000;

const RULES = readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8');

// Where each get() and exists() of the rules starts, and which document it reaches.
const CALL_SITES = new Map();
for (const match of RULES.matchAll(/\b(get|exists)\(\/databases\/[^\n]*/g)) {
    const text = match[0];
    const target = /\/grants\//.test(text)
        ? 'grant'
        : /\/searches\//.test(text)
            ? 'search'
            : /\/invitations\//.test(text)
                ? 'invitation'
                : /groups\/\$\(groupId\)\)/.test(text)
                    ? 'group'
                    : 'other';
    CALL_SITES.set(match.index, { kind: match[1], target, line: RULES.slice(0, match.index).split('\n').length });
}

// Where the condition of each allow statement starts: how often it is evaluated
// is how many requests the rules weighed.
const ALLOWS = new Map();
for (const match of RULES.matchAll(/allow ([a-z, ]+): if\s+/g)) {
    const at = match.index + match[0].length;
    ALLOWS.set(at, { ops: match[1], line: RULES.slice(0, match.index).split('\n').length });
}

const env = await initializeTestEnvironment({ projectId: PROJECT, firestore: { rules: RULES } });

async function coverage() {
    const report = await (await fetch(`http://${HOST}/emulator/v1/projects/${PROJECT}:ruleCoverage`)).json();
    const counts = new Map();
    const walk = (node) => {
        const offset = node.sourcePosition?.currentOffset;
        if (CALL_SITES.has(offset) || ALLOWS.has(offset)) {
            const own = Object.entries(node)
                .filter(([key]) => key !== 'children' && key !== 'sourcePosition')
                .map(([, value]) => value);
            const total = JSON.stringify(own).match(/"count":"?(\d+)/g)?.reduce(
                (sum, hit) => sum + Number(hit.match(/(\d+)/)[1]),
                0,
            );
            counts.set(offset, (counts.get(offset) || 0) + (total || 0));
        }
        node.children?.forEach(walk);
    };
    report.report.forEach(walk);
    return counts;
}

// Every subscription made through here counts what it is handed.
const delivered = new Map();
const refusedCount = new Map();
function count(label, n) {
    delivered.set(label, (delivered.get(label) || 0) + n);
}
function follow(label, ref, isQuery) {
    return onSnapshot(
        ref,
        (snapshot) => {
            if (snapshot.metadata.fromCache) {
                return;
            }
            count(label, isQuery ? snapshot.docChanges().filter((c) => c.type !== 'removed').length : 1);
        },
        () => refusedCount.set(label, (refusedCount.get(label) || 0) + 1),
    );
}

const settle = () => new Promise((resolve) => setTimeout(resolve, SETTLE_MS));

async function measure(name, action) {
    await settle();
    const before = await coverage();
    const deliveredBefore = new Map(delivered);
    const refusedBefore = new Map(refusedCount);
    await action();
    await settle();
    const after = await coverage();

    const byTarget = {};
    for (const [offset, site] of CALL_SITES) {
        const n = (after.get(offset) || 0) - (before.get(offset) || 0);
        if (n > 0) {
            byTarget[`${site.kind} ${site.target} (line ${site.line})`] = n;
        }
    }
    let evaluated = 0;
    const allows = {};
    for (const [offset, site] of ALLOWS) {
        const n = (after.get(offset) || 0) - (before.get(offset) || 0);
        if (n > 0) {
            allows[`${site.ops} (line ${site.line})`] = n;
            evaluated += n;
        }
    }
    // One document read by the rules is paid once per request, however many
    // times the rules reach it.
    const perTarget = {};
    for (const [offset, site] of CALL_SITES) {
        const n = (after.get(offset) || 0) - (before.get(offset) || 0);
        perTarget[site.target] = (perTarget[site.target] || 0) + n;
    }
    const rulesBilled = Object.fromEntries(
        Object.entries(perTarget)
            .filter(([, n]) => n > 0)
            .map(([target, n]) => [target, Math.min(n, evaluated)]),
    );
    const got = {};
    for (const [label, n] of delivered) {
        const d = n - (deliveredBefore.get(label) || 0);
        if (d > 0) {
            got[label] = d;
        }
    }
    const refused = {};
    for (const [label, n] of refusedCount) {
        const d = n - (refusedBefore.get(label) || 0);
        if (d > 0) {
            refused[label] = d;
        }
    }
    const deliveredTotal = Object.values(got).reduce((a, b) => a + b, 0);
    console.log(
        'EVENT',
        JSON.stringify({
            name,
            deliveredTotal,
            rulesEvaluated: evaluated,
            rulesBilledAtMost: rulesBilled,
            delivered: got,
            refused,
            allows,
            rules: byTarget,
        }),
    );
}

// The household.
const M1 = 'm1';
const M2 = 'm2';
const M3 = 'm3';
const DEVICES = { [M1]: ['a1'], [M2]: ['b1', 'b2', 'b3'], [M3]: ['c1'] };
const GRANTS = { [M1]: [M2, M3], [M2]: [M1], [M3]: [M1] };

const position = (uid, lat) => ({
    memberId: uid,
    latitude: lat,
    longitude: 30,
    takenAt: new Date(),
    writtenAt: serverTimestamp(),
    mode: 'Standby',
    batteryPercent: 80,
    charging: false,
});

await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, `groups/${G}`), { members: [M1, M2, M3], admins: [M1] });
    for (const uid of [M1, M2, M3]) {
        await setDoc(doc(db, `groups/${G}/members/${uid}`), {
            displayName: uid,
            photoUrl: 'https://lh3.googleusercontent.com/a/x',
            updatedAt: new Date(),
        });
        await setDoc(doc(db, `groups/${G}/grants/${uid}`), {
            sharedWith: Object.fromEntries(GRANTS[uid].map((r) => [r, FOREVER])),
            devices: DEVICES[uid],
        });
        await setDoc(
            doc(db, `groups/${G}/devices/${uid}`),
            Object.fromEntries(DEVICES[uid].map((p) => [p, { model: 'x', mode: 'Standby' }])),
        );
        for (const device of DEVICES[uid]) {
            await setDoc(doc(db, `groups/${G}/positions/${uid}.${device}`), position(uid, 60));
        }
    }
});

// One client per device, as on the devices.
const clients = {};
for (const [uid, devices] of Object.entries(DEVICES)) {
    for (const device of devices) {
        clients[device] = { uid, db: env.authenticatedContext(uid).firestore() };
    }
}

// What the service of every device keeps open (service/*Source.kt).
function serviceOf(device) {
    const { uid, db } = clients[device];
    const p = `svc ${device}`;
    follow(`${p} group`, doc(db, `groups/${G}`));
    follow(`${p} watch*`, collection(db, `groups/${G}/watch`), true);
    follow(`${p} searches*`, collection(db, `groups/${G}/searches`), true);
    follow(`${p} falsePlaces*`, collection(db, `groups/${G}/falsePlaces`), true);
    follow(`${p} own search`, doc(db, `groups/${G}/searches/${uid}.${device}`));
    follow(`${p} own siren`, doc(db, `groups/${G}/sirens/${uid}.${device}`));
    follow(`${p} own reset`, doc(db, `groups/${G}/resets/${uid}.${device}`));
    // The own grant also tells the service that this device was taken out of
    // "My devices" (service/OwnLine.kt); the member's devices document, which
    // every device of theirs rewrites with its status, is not listened to.
    follow(`${p} own grant`, doc(db, `groups/${G}/grants/${uid}`));
    follow(`${p} circling`, doc(db, `groups/${G}/settings/circling`));
}

// What an open map adds (map/*Source.kt, web/public/lib/store-*.js): the
// profiles, the grant and the devices of every other member, and a position
// for every device a readable grant names.
const mapStops = [];
function mapOf(device) {
    const { uid, db } = clients[device];
    const p = `map ${device}`;
    mapStops.push(follow(`${p} members*`, collection(db, `groups/${G}/members`), true));
    mapStops.push(follow(`${p} labels`, doc(db, `groups/${G}/labels/${uid}`)));
    for (const other of [M1, M2, M3]) {
        if (other === uid) {
            continue;
        }
        mapStops.push(follow(`${p} grant ${other}`, doc(db, `groups/${G}/grants/${other}`)));
        mapStops.push(follow(`${p} devices ${other}`, doc(db, `groups/${G}/devices/${other}`)));
        if (GRANTS[other].includes(uid)) {
            for (const id of DEVICES[other]) {
                const position = doc(db, `groups/${G}/positions/${other}.${id}`);

                mapStops.push(follow(`${p} pos ${other}.${id}`, position));
            }
        }
    }
    for (const id of DEVICES[uid]) {
        mapStops.push(follow(`${p} pos own ${id}`, doc(db, `groups/${G}/positions/${uid}.${id}`)));
    }
}

const write = (device, path, data, options) =>
    setDoc(doc(clients[device].db, `groups/${G}/${path}`), data, options ?? {});

console.log('CALL_SITES', JSON.stringify([...CALL_SITES.values()]));

await measure('services start, 5 devices', async () => {
    Object.keys(clients).forEach(serviceOf);
    return 5 * 10;
});
await measure('background: position write by b1 (no map)', () => write('b1', `positions/${M2}.b1`, position(M2, 60.001)));
await measure('background: status write by b1 (no map)', () =>
    write('b1', `devices/${M2}`, { b1: { model: 'x', mode: 'Standby', batteryPercent: 70 } }, { merge: true }),
);

for (const viewer of ['a1', 'c1']) {
    const uid = clients[viewer].uid;
    await measure(`map ${viewer} (${uid}) opens`, async () => {
        mapOf(viewer);
    });
    await measure(`map ${viewer}: watch mark written`, () =>
        write(viewer, `watch/${uid}.${viewer}`, { memberId: uid, since: serverTimestamp() }),
    );
    await measure(`map ${viewer}: position write by b1`, () => write('b1', `positions/${M2}.b1`, position(M2, 60.002)));
    await measure(`map ${viewer}: position write by a1`, () => write('a1', `positions/${M1}.a1`, position(M1, 60.002)));
    await measure(`map ${viewer}: position write by c1`, () => write('c1', `positions/${M3}.c1`, position(M3, 60.002)));
    await measure(`map ${viewer}: status write by b1`, () =>
        write('b1', `devices/${M2}`, { b1: { model: 'x', mode: 'Viewing', batteryPercent: 69 } }, { merge: true }),
    );
    // A refused document is asked for again a minute later (RETRY_AFTER).
    await measure(`map ${viewer}: one round of retries of refused documents`, async () => {
        const { db } = clients[viewer];
        let n = 0;
        for (const other of [M1, M2, M3]) {
            if (other === uid || GRANTS[other].includes(uid)) {
                continue;
            }
            for (const what of ['grants', 'devices']) {
                const refused = doc(db, `groups/${G}/${what}/${other}`);
                const stop = follow(`retry ${viewer} ${what} ${other}`, refused);
                n++;
                await settle();
                stop();
            }
        }
        return n;
    });
    await measure(`map ${viewer}: pause and resume (disableNetwork/enableNetwork)`, async () => {
        await disableNetwork(clients[viewer].db);
        await settle();
        await enableNetwork(clients[viewer].db);
    });
    await measure(`map ${viewer}: watch mark removed`, () =>
        deleteDoc(doc(clients[viewer].db, `groups/${G}/watch/${uid}.${viewer}`)),
    );
    await measure(`map ${viewer} closes`, async () => {
        mapStops.splice(0).forEach((stop) => stop());
    });
}

// The page in a browser, signed in as m1: a client of its own, apart from any
// device (web/public/lib/store*.js).
{
    const db = env.authenticatedContext(M1).firestore();
    clients.web = { uid: M1, db };
    await measure('page m1 opens', async () => {
        follow('web group', doc(db, `groups/${G}`));
        follow('web members*', collection(db, `groups/${G}/members`), true);
        follow('web watch*', collection(db, `groups/${G}/watch`), true);
        follow('web searches*', collection(db, `groups/${G}/searches`), true);
        follow('web falsePlaces*', collection(db, `groups/${G}/falsePlaces`), true);
        follow('web labels', doc(db, `groups/${G}/labels/${M1}`));
        for (const other of [M2, M3]) {
            follow(`web grant ${other}`, doc(db, `groups/${G}/grants/${other}`));
            follow(`web devices ${other}`, doc(db, `groups/${G}/devices/${other}`));
            for (const id of DEVICES[other]) {
                follow(`web pos ${other}.${id}`, doc(db, `groups/${G}/positions/${other}.${id}`));
            }
        }
    });
    await measure('page m1: watch mark written', () =>
        setDoc(doc(db, `groups/${G}/watch/${M1}.web`), { memberId: M1, since: serverTimestamp() }),
    );
    await measure('page m1: position write by b1', () => write('b1', `positions/${M2}.b1`, position(M2, 60.003)));
    await measure('page m1: status write by b1', () =>
        write('b1', `devices/${M2}`, { b1: { model: 'x', mode: 'Viewing', batteryPercent: 68 } }, { merge: true }),
    );
    await measure('page m1: pause and resume, nothing changed meanwhile', async () => {
        await disableNetwork(db);
        await settle();
        await enableNetwork(db);
    });
    await measure('page m1: pause, b1 writes twice, resume', async () => {
        await disableNetwork(db);
        await write('b1', `positions/${M2}.b1`, position(M2, 60.004));
        await write('b1', `positions/${M2}.b1`, position(M2, 60.005));
        await settle();
        await enableNetwork(db);
    });
}

await env.cleanup();
console.log('COST DONE');
process.exit(0);
