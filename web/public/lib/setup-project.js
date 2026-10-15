// The steps that make a Firebase project ready for a group, run from the page
// with the account's own token.
//
// Every step first looks for what is already there and creates only what is
// missing, so a run that broke half way, or was started twice, can simply be
// run again. None of it touches the page: the caller hands in the function that
// reaches the network and gets small plain values back.

const FIREBASE = 'https://firebase.googleapis.com/v1beta1';
const SERVICE_USAGE = 'https://serviceusage.googleapis.com/v1';
const FIRESTORE = 'https://firestore.googleapis.com/v1';
const RULES = 'https://firebaserules.googleapis.com/v1';

/** How long an operation is polled before the step gives up, in polls. */
const OPERATION_POLLS = 90;
const OPERATION_POLL_MS = 3000;
// Polls of one operation lost in a row before the step gives up on it.
const LOST_POLLS = 3;

/** A fresh database refuses with 403 until the API enabled a moment ago spreads. */
const DATABASE_ATTEMPTS = 20;
const DATABASE_RETRY_MS = 15_000;

const realSleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * setup-config.json as tools/setup-site.sh writes it, or null when it is not:
 * a file cut short or written by hand would otherwise read as a config and
 * fail steps later, with a message about a step rather than about the file.
 * The fingerprints may be none; the console link may be left out.
 */
export function setupConfigOf(value) {
    const filled = (field) => typeof field === 'string' && field.trim().length > 0;

    if (
        value === null ||
        typeof value !== 'object' ||
        !filled(value.clientId) ||
        !filled(value.packageName) ||
        !filled(value.location) ||
        !Array.isArray(value.fingerprints) ||
        !value.fingerprints.every(filled) ||
        (value.consoleUrl !== undefined && !filled(value.consoleUrl))
    ) {
        return null;
    }

    return value;
}

/**
 * An Error tagged with what went wrong in terms the page can put into words:
 * 'network', 'denied', 'timeout', 'missing', 'transient' or 'other'. The text
 * of the Error stays technical, for the console; the page shows a phrase
 * picked by the kind.
 */
export function failure(message, kind, cause) {
    const error = new Error(message, { cause });

    error.kind = kind;

    return error;
}

function kindOfStatus(status) {
    if (status === 401 || status === 403) {
        return 'denied';
    }

    if (status === 404) {
        return 'missing';
    }

    // Too many requests, or a server that failed: the same request may pass
    // in a moment.
    return status === 429 || status >= 500 ? 'transient' : 'other';
}

/** Whether a call that failed this way may pass if it is made again. */
function passing(error) {
    return error?.kind === 'network' || error?.kind === 'transient';
}

/** The kind of a thrown value; anything not tagged is 'other'. */
export function failureKind(error) {
    return error?.kind || 'other';
}

/** The key in the words of the setup page for a kind of failure. */
export function failureWord(kind) {
    const keys = {
        network: 'errNetwork',
        denied: 'errDenied',
        timeout: 'errTimeout',
        transient: 'errTimeout',
        missing: 'errMissing',
    };

    return keys[kind] || 'errOther';
}

/**
 * The caller of the Google APIs for one token. An answer that is not a success
 * is thrown as an Error carrying `status` and the parsed body in `data`, since
 * the steps tell "already there" (409) and "not ready yet" (403) by the status.
 * With `raw` the body is sent as it is, for the uploads of hosting.
 */
export function makeApi(fetchFn, token) {
    return async function api(method, url, body, raw) {
        const headers = { Authorization: `Bearer ${token}` };

        if (body && !raw) {
            headers['Content-Type'] = 'application/json';
        }

        if (raw) {
            headers['Content-Type'] = 'application/octet-stream';
        }

        let response;

        try {
            response = await fetchFn(url, {
                method,
                headers,
                body: body ? (raw ? body : JSON.stringify(body)) : undefined,
            });
        } catch (cause) {
            throw failure(
                `${method} ${url.replace(/\?.*/, '')} failed: ${cause.message}`,
                'network',
                cause,
            );
        }

        const text = await response.text();
        let data = null;

        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text;
        }

        if (!response.ok) {
            const reason = `${data?.error?.status || ''} ${data?.error?.message || text}`;
            const error = new Error(
                `${method} ${url.replace(/\?.*/, '')} -> ${response.status} ${reason.slice(0, 300)}`,
            );

            error.status = response.status;
            error.data = data;
            error.kind = kindOfStatus(response.status);
            throw error;
        }

        return data;
    };
}

/**
 * Waits for a long-running operation of the API at `base` and returns what it
 * produced. An operation that is still going after OPERATION_POLLS polls, or
 * ended with an error, is thrown.
 *
 * The operation goes on at Google whatever happens to one poll of it, so a
 * poll lost to the network or to a busy server is made again; LOST_POLLS of
 * them in a row are thrown.
 */
export async function waitOperation(api, base, op, sleep = realSleep) {
    let current = op;
    let lost = 0;

    for (let poll = 0; poll < OPERATION_POLLS && !current.done; poll++) {
        await sleep(OPERATION_POLL_MS);

        try {
            current = await api('GET', `${base}/${current.name}`);
            lost = 0;
        } catch (error) {
            lost++;

            if (!passing(error) || lost >= LOST_POLLS) {
                throw error;
            }
        }
    }

    if (!current.done) {
        throw failure('the operation did not finish in time', 'timeout');
    }

    if (current.error) {
        throw new Error(JSON.stringify(current.error));
    }

    return current.response;
}

/**
 * The Firebase projects of the account, as the API lists them, every page of
 * the list: an account with more than a hundred projects had the rest left
 * out of the choice.
 */
export async function listProjects(api) {
    const projects = [];
    let pageToken = '';

    do {
        const token = pageToken ? `&pageToken=${encodeURIComponent(pageToken)}` : '';
        const data = await api('GET', `${FIREBASE}/projects?pageSize=100${token}`);

        projects.push(...(data?.results || []));
        pageToken = data?.nextPageToken || '';
    } while (pageToken);

    return projects;
}

/** Turns Firestore on for the project; enabling a service already on is a no-op. */
export async function enableFirestoreApi(api, projectNumber, sleep = realSleep) {
    const service = 'firestore.googleapis.com';
    const url = `${SERVICE_USAGE}/projects/${projectNumber}/services/${service}:enable`;
    const op = await api('POST', url, {});

    if (!op.done) {
        await waitOperation(api, SERVICE_USAGE, op, sleep);
    }

    return 'enabled';
}

/**
 * Creates the default database. Returns 'created', or 'existing' when the
 * project already has one (409). A 403 right after the API was enabled only
 * means it is not ready, so it is retried before it is believed.
 */
export async function createDatabase(api, projectId, location, sleep = realSleep) {
    const url = `${FIRESTORE}/projects/${projectId}/databases?databaseId=(default)`;

    for (let attempt = 1; ; attempt++) {
        try {
            const op = await api('POST', url, { locationId: location, type: 'FIRESTORE_NATIVE' });

            await waitOperation(api, FIRESTORE, op, sleep);

            return 'created';
        } catch (error) {
            if (error.status === 409) {
                return 'existing';
            }

            if (error.status === 403 && attempt < DATABASE_ATTEMPTS) {
                await sleep(DATABASE_RETRY_MS);
                continue;
            }

            throw error;
        }
    }
}

/** The web app of the project, the first one if several, created when there is none. */
export async function ensureWebApp(api, projectId) {
    const list = await api('GET', `${FIREBASE}/projects/${projectId}/webApps`);
    const found = (list?.apps || [])[0];

    if (found) {
        return { name: found.name, appId: found.appId, created: false };
    }

    const op = await api('POST', `${FIREBASE}/projects/${projectId}/webApps`, {
        displayName: 'geoshare page',
    });
    const app = await waitOperation(api, FIREBASE, op);

    return { name: app.name, appId: app.appId, created: true };
}

/** The Android app of the package, created when the project has none. */
export async function ensureAndroidApp(api, projectId, packageName, sleep = realSleep) {
    const list = await api('GET', `${FIREBASE}/projects/${projectId}/androidApps`);
    const found = (list?.apps || []).find((app) => app.packageName === packageName);

    if (found) {
        return { name: found.name, appId: found.appId, created: false };
    }

    const op = await api('POST', `${FIREBASE}/projects/${projectId}/androidApps`, {
        packageName,
        displayName: 'geoshare',
    });
    const app = await waitOperation(api, FIREBASE, op, sleep);

    return { name: app.name, appId: app.appId, created: true };
}

/** A fingerprint as the API keeps it: lower-case hex, without the colons keytool prints. */
const bareHash = (hash) => hash.replaceAll(':', '').toLowerCase();

/**
 * Registers the signing fingerprints the app does not have yet, and returns
 * the ones it posted. They may come as keytool prints them, in colon-separated
 * pairs. A hash is told from its length: 40 digits are SHA-1, anything else is
 * taken for SHA-256.
 */
export async function ensureFingerprints(api, androidAppName, fingerprints) {
    const url = `${FIREBASE}/${androidAppName}/sha`;
    const list = await api('GET', url);
    const have = new Set((list?.certificates || []).map((c) => bareHash(c.shaHash)));
    const missing = fingerprints.map(bareHash).filter((hash) => !have.has(hash));

    for (const hash of missing) {
        await api('POST', url, { shaHash: hash, certType: hash.length === 40 ? 'SHA_1' : 'SHA_256' });
    }

    return missing;
}

/**
 * Puts the Firestore rules in force and returns the name of the ruleset. The
 * release is created, or, when one exists already (409), moved to the new
 * ruleset.
 */
export async function deployRules(api, projectId, source) {
    const base = `${RULES}/projects/${projectId}`;
    const ruleset = await api('POST', `${base}/rulesets`, {
        source: { files: [{ name: 'firestore.rules', content: source }] },
    });
    const release = {
        name: `projects/${projectId}/releases/cloud.firestore`,
        rulesetName: ruleset.name,
    };

    try {
        await api('POST', `${base}/releases`, release);
    } catch (error) {
        if (error.status !== 409) {
            throw error;
        }

        await api('PATCH', `${base}/releases/cloud.firestore`, { release });
    }

    return ruleset.name;
}
