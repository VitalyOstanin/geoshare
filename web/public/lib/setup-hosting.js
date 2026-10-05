// The hosting half of setting a project up from the page: the site the page is
// served from, the geoshare.json the app reads its project out of, and the
// deploy that puts both online.
//
// Every function takes `api(method, url, body, raw)` rather than calling fetch
// itself: it answers with the parsed JSON and throws an Error carrying the
// HTTP `.status` on anything but 2xx. The page owns the access token, and this
// module can be run under node:test with a fake.

const FIREBASE = 'https://firebase.googleapis.com/v1beta1';
const HOSTING = 'https://firebasehosting.googleapis.com/v1beta1';
const IDENTITY = 'https://identitytoolkit.googleapis.com/admin/v2';

/** The address a hosting site is served from. */
function siteUrl(siteId) {
    return `https://${siteId}.web.app`;
}

/**
 * The hosting site named exactly like the project: the default one the
 * project already has, or a new one.
 *
 * No other name is tried when this one is taken. The sign-in page has to be
 * among the project's authorized domains, and writing that list takes a scope
 * the narrow grant of this page does not have, so a site under another name
 * could be created but never signed in from. Taken names say so with
 * `.code = 'site-name-taken'` for the page to tell the owner.
 */
export async function ensureSite(api, projectId, webAppId) {
    const list = await api('GET', `${HOSTING}/projects/${projectId}/sites`);
    const existing = (list?.sites ?? []).find((site) => site.name.split('/').pop() === projectId);

    if (existing) {
        return existing;
    }

    try {
        return await api('POST', `${HOSTING}/projects/${projectId}/sites?siteId=${projectId}`, {
            appId: webAppId,
        });
    } catch (error) {
        if (error.status === 400 || error.status === 409) {
            const taken = new Error(`the site name ${projectId} is taken`);

            taken.code = 'site-name-taken';
            taken.cause = error;
            throw taken;
        }

        throw error;
    }
}

/**
 * The geoshare.json the app takes its project from, built out of the Android
 * app's own google-services config so the keys are the ones the app was
 * registered with.
 */
export async function appSettings(api, androidAppName, siteId) {
    const config = await api('GET', `${FIREBASE}/${androidAppName}/config`);
    const services = JSON.parse(atob(config.configFileContents));
    const client = services.client[0];

    return {
        version: 1,
        projectId: services.project_info.project_id,
        applicationId: client.client_info.mobilesdk_app_id,
        apiKey: client.api_key[0].current_key,
        gcmSenderId: services.project_info.project_number,
        site: siteId,
    };
}

/**
 * The firebase-config.js the group's page imports, as tools/web-config.sh
 * writes it: the web app's own config, with sign-in sent through the group's
 * site rather than firebaseapp.com, since browsers that keep each site's
 * storage apart lose a redirect sign-in passing through another domain.
 */
export async function webConfigFile(api, webAppName, siteId) {
    const firebaseConfig = await api('GET', `${FIREBASE}/${webAppName}/config`);

    firebaseConfig.authDomain = `${siteId}.web.app`;

    return [
        '// Written by the setup page out of the Firebase project itself.',
        `export const firebaseConfig = ${JSON.stringify(firebaseConfig, null, 4)};`,
        '',
    ].join('\n');
}

async function sha256Hex(bytes) {
    const digest = await crypto.subtle.digest('SHA-256', bytes);

    return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('');
}

/** Hosting names a file by the hash of its gzipped bytes, so that is what is sent. */
async function gzip(content) {
    const stream = new Blob([content]).stream().pipeThrough(new CompressionStream('gzip'));

    return new Uint8Array(await new Response(stream).arrayBuffer());
}

/**
 * Puts the files online as the site's live version and answers its address.
 *
 * `files` maps a path ("/index.html") to a string or bytes. Hosting is asked
 * which of them it does not hold yet and is sent only those; the version is
 * finalized before the release, since an unfinalized one cannot be released.
 * `config` is the version's rewrites and headers, as versionConfig() makes them.
 */
export async function deploySite(api, siteId, files, config = {}) {
    const version = await api('POST', `${HOSTING}/sites/${siteId}/versions`, { config });
    const gzipped = {};
    const hashes = {};

    for (const [path, content] of Object.entries(files)) {
        gzipped[path] = await gzip(content);
        hashes[path] = await sha256Hex(gzipped[path]);
    }

    const populated = await api('POST', `${HOSTING}/${version.name}:populateFiles`, {
        files: hashes,
    });

    for (const hash of populated.uploadRequiredHashes ?? []) {
        const path = Object.keys(hashes).find((candidate) => hashes[candidate] === hash);

        await api('POST', `${populated.uploadUrl}/${hash}`, gzipped[path], true);
    }

    await api('PATCH', `${HOSTING}/${version.name}?update_mask=status`, { status: 'FINALIZED' });
    await api('POST', `${HOSTING}/sites/${siteId}/releases?versionName=${version.name}`, {});

    return siteUrl(siteId);
}

/** The paths of the files the site serves now: those of the live channel's release. */
export async function releasedPaths(api, siteId) {
    const channel = await api('GET', `${HOSTING}/sites/${siteId}/channels/live`);
    const version = channel.release?.version?.name;
    const paths = new Set();

    if (!version) {
        return paths;
    }

    let pageToken = '';

    do {
        const page = await api(
            'GET',
            `${HOSTING}/${version}/files?status=ACTIVE${pageToken ? `&pageToken=${pageToken}` : ''}`,
        );

        for (const file of page.files ?? []) {
            paths.add(file.path);
        }

        pageToken = page.nextPageToken ?? '';
    } while (pageToken);

    return paths;
}

/**
 * The `hosting` section of firebase.json in the shape the Hosting API takes
 * for a version: `source` becomes `glob`, a rewrite's `destination` its
 * `path`. Without it the group's site would lose /join and its headers.
 */
export function versionConfig(hosting) {
    return {
        rewrites: (hosting.rewrites ?? []).map((rewrite) => ({
            glob: rewrite.source,
            path: rewrite.destination,
        })),
        headers: (hosting.headers ?? []).map((rule) => ({
            glob: rule.source,
            headers: Object.fromEntries(rule.headers.map((header) => [header.key, header.value])),
        })),
    };
}

/** Whether signing in with Google is switched on in the project; no such config means off. */
export async function googleSignInEnabled(api, projectId) {
    try {
        const idp = await api(
            'GET',
            `${IDENTITY}/projects/${projectId}/defaultSupportedIdpConfigs/google.com`,
        );

        return idp?.enabled === true;
    } catch (error) {
        if (error.status === 404) {
            return false;
        }

        throw error;
    }
}
