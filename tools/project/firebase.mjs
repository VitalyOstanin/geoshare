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

// The shortest password Identity Toolkit takes.
const MIN_PASSWORD_LENGTH = 6;

// Why a password file's permissions are refused, or '' when they are fine:
// the password is a secret kept in a file, and a file other users can read
// is not one. `mode` is the file's st_mode; only the permission bits count.
export function permissionsProblem(mode, file) {
    const bits = mode & 0o777;

    if (bits === 0o600 || bits === 0o400) {
        return '';
    }

    return `${file} holds a password and has permissions ${bits.toString(8)}; `
        + `it must be 600 or 400 (chmod 600 ${file})`;
}

// The password out of the file's text: its first line, without the line
// break. Throws with the reason when it is too short to be accepted, so the
// refusal comes before any request is made.
export function passwordFrom(text) {
    const password = text.split(/\r?\n/)[0];

    if (password.length < MIN_PASSWORD_LENGTH) {
        throw new Error(`the password is too short: it has ${password.length} characters, `
            + `at least ${MIN_PASSWORD_LENGTH} are needed (first line of the file, `
            + 'without the line break)');
    }

    return password;
}

// Whether sign-in with an email address and a password is on, out of the
// Identity Toolkit admin API's configuration of the project. JSON leaves out
// a false boolean, so a provider that is off comes back without the field.
export function emailSignInOn(body) {
    return JSON.parse(body).signIn?.email?.enabled === true;
}

// The code Identity Toolkit gives for a refusal (EMAIL_EXISTS, WEAK_PASSWORD,
// INVALID_EMAIL, ...), out of error.message, which is the code optionally
// followed by " : " and a hint. Empty when the answer is no error body at all.
export function errorCode(body) {
    let message;

    try {
        message = JSON.parse(body).error?.message;
    } catch {
        return '';
    }

    return typeof message === 'string' ? message.split(' : ')[0].trim() : '';
}

// The uid of the account with the address looked up, or '' when there is none:
// a lookup of an address nobody has answers without a users list at all.
export function lookupUid(body) {
    return JSON.parse(body).users?.[0]?.localId ?? '';
}

// The uid of the account that was just made, out of accounts:create's answer.
export function createdUid(body) {
    const uid = JSON.parse(body).localId;

    if (!uid) {
        throw new Error('the answer names no uid');
    }

    return uid;
}

// What to tell whoever ran a command that Identity Toolkit refused: the
// code's own plain words, with the way out where there is one.
export function refusalText({ code, status, project, email }) {
    switch (code) {
        case 'EMAIL_EXISTS':
            return `${email} already has an account in ${project}; to change its password run `
                + `\`tools/project.sh set-password ${project} --email ${email} `
                + '--password-file <file>`';
        case 'WEAK_PASSWORD':
            return 'Firebase refuses the password as too weak; use a longer one';
        case 'INVALID_EMAIL':
            return `${email} is not an address Firebase accepts`;
        default:
            return `Identity Toolkit refused the request: ${code || `HTTP ${status}`}`;
    }
}
