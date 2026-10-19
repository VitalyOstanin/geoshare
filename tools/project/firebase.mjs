// What tools/project.sh reads out of the answers of the firebase command line
// and of the Google APIs it asks directly, and the few values it builds from
// them. Kept apart from the shell so that the parsing is linted and tested
// like the page's own code; every function here is pure, and cli.mjs is the
// one that reads standard input and prints.

// The JSON of a firebase command run with --json. The command line can put a
// line of its own ahead of it (an update notice, a warning), so whatever comes
// before the first brace is dropped.
export function afterBanner(raw) {
    return JSON.parse(raw.slice(raw.indexOf('{')));
}

// The fingerprints an existing project's Android app accepts, out of
// `firebase apps:android:sha:list --json`.
export function listedShaHashes(raw) {
    return afterBanner(raw).result.map((entry) => entry.shaHash);
}

// The app ids of one platform, out of the Management API's listing of the
// project's androidApps or webApps. A project with none has no field at all.
export function appIds(body) {
    return (JSON.parse(body).apps ?? []).map((app) => app.appId);
}

// The fingerprints the Android app already knows, out of the Management API's
// sha listing of the app.
export function certificateHashes(body) {
    return (JSON.parse(body).certificates ?? []).map((entry) => entry.shaHash);
}

// The access token of the account this runs as, out of the firebase command
// line's own store, and how many whole seconds it has left. An empty account
// means the command line's default one.
export function storedToken(store, account, nowMs) {
    const wanted = (account ?? '').toLowerCase();
    // The default account sits at the top of the file, every one added with
    // login:add in additionalAccounts beside it, each with tokens of its own.
    const accounts = [store, ...(store.additionalAccounts ?? [])];
    const match = accounts.filter((a) => !wanted || a.user.email.toLowerCase() === wanted);
    const tokens = match[0].tokens;

    return {
        token: tokens.access_token,
        // expires_at is in milliseconds since the epoch.
        secondsLeft: Math.trunc((tokens.expires_at ?? 0) / 1000 - nowMs / 1000),
    };
}

// The address of the firebase command line's default account.
export function defaultAccount(store) {
    return store.user.email;
}

// Whether the Google provider of Identity Toolkit is on. JSON leaves out a
// false boolean, so a provider that was switched off again comes back without
// the field.
export function googleSignInOn(body) {
    return JSON.parse(body).enabled === true;
}

// The Hosting site the page goes to, out of the Hosting API's listing: the
// project's default site where it has one, else its first, else none.
export function chosenSite(body) {
    const sites = JSON.parse(body).sites ?? [];
    const defaults = sites.filter((s) => s.type === 'DEFAULT_SITE');
    const chosen = (defaults.length ? defaults : sites)[0];

    return chosen ? chosen.name.split('/').pop() : '';
}

// geoshare.json, the settings the application reads from the group's site,
// out of `firebase apps:sdkconfig ANDROID --json`: the google-services.json
// the command line returns comes as a string inside its own JSON.
export function appSettings(raw, site) {
    const config = JSON.parse(afterBanner(raw).result.fileContents);
    const client = config.client[0];

    return {
        version: 1,
        projectId: config.project_info.project_id,
        applicationId: client.client_info.mobilesdk_app_id,
        apiKey: client.api_key[0].current_key,
        gcmSenderId: config.project_info.project_number,
        site,
    };
}

// A console link that opens in the given account rather than in the
// browser's first one; with no account, the link as it is.
export function consoleLink(url, who) {
    const link = new URL(url);

    if (who) {
        link.searchParams.set('authuser', who);
    }

    return link.toString();
}
