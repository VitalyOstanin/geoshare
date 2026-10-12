// The Firebase instances the rest of the store modules share, and what this
// page asks of them directly: who is signed in, which group a browser is
// addressed to, and saying that somebody is looking. The other pieces of the
// store — positions, roster and membership, searches and sirens — live in
// `store-positions.js`, `store-group.js` and `store-search.js`, all of which
// import `app`/`auth`/`store` from here rather than making their own.

import { initializeApp } from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js';
import {
    GoogleAuthProvider,
    connectAuthEmulator,
    getAuth,
    getRedirectResult,
    onAuthStateChanged,
    signInWithRedirect,
    signOut,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js';
import {
    collection,
    connectFirestoreEmulator,
    deleteDoc,
    disableNetwork,
    doc,
    enableNetwork,
    getFirestore,
    onSnapshot,
    serverTimestamp,
    setDoc,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { firebaseConfig } from '../firebase-config.js';
import { createRefusals } from './refusals.js';

/**
 * Where this browser keeps the group it looks at.
 *
 * It used to be a constant of the page, and could be while every visitor
 * belonged to one household. A group is now named after the account that made
 * it, and that is not known when the page is written, so the name is something
 * this browser remembers from the moment its viewer founded a group or joined
 * one.
 */
const GROUP_KEY = 'geoshare.group';

/**
 * The group of the household that started before groups had names of their own.
 *
 * What a browser that has joined nothing assumes, because those devices and
 * this page are already reading it: its document was made before the rule that
 * names a founder, and a change that moved the page elsewhere would take the
 * household off its own map.
 */
const INHERITED_GROUP = 'family';

/**
 * The group everything below is addressed to.
 *
 * Read on each use rather than once at the start: founding a group or joining
 * one changes it while the page is open, and every subscription made
 * afterwards has to reach the new one.
 *
 * A browser told to keep no site data throws on the attempt rather than
 * answering with nothing, and a page that cannot remember a group should still
 * show the one it started with.
 */
export function currentGroup(storage = window.localStorage) {
    try {
        return storage.getItem(GROUP_KEY) ?? INHERITED_GROUP;
    } catch {
        return INHERITED_GROUP;
    }
}

/** Remembers which group this browser is in, or carries on without remembering. */
export function rememberGroup(group, storage = window.localStorage) {
    try {
        storage.setItem(GROUP_KEY, group);
    } catch {
        // Nothing to say to the viewer: what they are looking at is the group
        // they just joined, it is only the next visit that starts elsewhere.
    }
}

/**
 * How often the claim to be looking is pushed forward.
 *
 * How long each one is worth is not said here and not said by this page: the
 * claim carries the moment the server wrote it, and the device reading it adds
 * the seven minutes both clients agree on. A viewer whose clock is wrong can
 * then neither hold the group longer nor fail to hold it at all.
 *
 * Every device of the group reads each push, so in a group of twenty a push a
 * minute is over a thousand reads an hour for one open page; leaving the page
 * properly withdraws the claim at once, so the length only matters for a
 * viewer who vanished.
 */
const WATCH_REFRESH_MS = 5 * 60 * 1000;

/**
 * How often the page asks itself whether it is still being looked at, and
 * tries again a push that failed.
 *
 * Apart from the refresh above: a tab left alone is noticed by this, not by
 * an event, and waiting five minutes to notice would keep the devices in their
 * fast mode that much longer; and a lost push waited for five minutes would
 * let the claim run out before the next one.
 */
const WATCH_CHECK_MS = 60 * 1000;

/**
 * How long a claim to be looking is worth, counted from the moment the server
 * wrote it down.
 *
 * The same seven minutes the devices add, and for the same reason: the claim
 * carries when it was made rather than when it ends, so whoever reads it
 * decides how long to believe it. Longer than the refresh above, so that one
 * write lost to a bad connection does not drop a viewer off the line.
 */
const WATCH_AHEAD_MS = 7 * 60 * 1000;

/**
 * How long a tab nobody touches goes on claiming to be looking.
 *
 * A tab left open on a laptop its owner walked away from holds every device of
 * the group in the thirty-second mode, which is the most expensive thing the
 * group does. Five minutes, the same as the page takes to stop listening
 * altogether (see `page/pause.js`): whoever watches somebody drive home
 * moves the mouse now and then, and a forgotten tab stops costing at once.
 */
export const IDLE_AFTER_MS = 5 * 60 * 1000;

/**
 * How long to wait before asking again for a document that was refused, when
 * nothing has told the page sooner.
 *
 * A refusal is usually the member saying they no longer share, and a permission
 * handed back touches their profile, which the page is already following and
 * retries on at once (see `refusals.js`). The timer only covers a permission
 * that expires or that arrives without such a touch, so it is slow: every ask
 * is a paid read.
 */
export const RETRY_AFTER_MS = 15 * 60 * 1000;

/** The refused subscriptions of the page, asked for again on a profile change or on the timer. */
export const refusals = createRefusals(RETRY_AFTER_MS);

/** Where this browser keeps the name it watches under, so a reload keeps one marker. */
const DEVICE_KEY = 'geoshare.device';

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const store = getFirestore(app);

// A trial stand and the page's snapshots talk to the Firebase emulators
// instead of a real project; the config says so with an optional `emulators`
// field, and its project id must start with `demo-` so nothing here can be
// mistaken for, or reach, a real one. Without the field nothing changes.
const emulators = firebaseConfig.emulators;
if (emulators) {
    connectAuthEmulator(auth, emulators.auth, { disableWarnings: true });
    connectFirestoreEmulator(store, emulators.firestore.host, emulators.firestore.port);
}

export function watchSignIn(onChange) {
    return onAuthStateChanged(auth, (user) =>
        onChange(
            user && {
                memberId: user.uid,
                name: user.displayName,
                picture: user.photoURL,
                email: user.email,
            },
        ),
    );
}

/**
 * Whatever became of a sign-in the page was left for, or nothing when it was
 * not.
 *
 * The account itself arrives through watchSignIn; this is only for the
 * failure, which would otherwise be lost with the page that asked.
 */
export function signInReturned() {
    return getRedirectResult(auth);
}

export function signIn() {
    // A trip to Google and back rather than a popup: device browsers ask
    // before opening a window, open it as a tab, and block it outright when
    // it comes too long after the tap. The page is loaded afresh on the way
    // back, and nothing on the sign-in screen is lost with it.
    return signInWithRedirect(auth, new GoogleAuthProvider());
}

export function leave() {
    return signOut(auth);
}

/**
 * Whether each snapshot of the group document came from the cache, and again
 * whenever only that changes.
 *
 * The group document is the one every signed-in page is on, so its snapshots
 * are what the link to the database is judged by (see `link.js`). A refusal
 * is the server answering, so it is read as a confirmation.
 */
export function watchLink(onSnapshotFrom) {
    return onSnapshot(
        doc(store, 'groups', currentGroup()),
        { includeMetadataChanges: true },
        (snapshot) => onSnapshotFrom(snapshot.metadata.fromCache),
        () => onSnapshotFrom(false),
    );
}

/**
 * What this browser calls itself when it claims to be looking.
 *
 * Made up once and kept, so that a reload does not leave a second marker
 * behind; a browser that clears its storage simply gets a new name, and the
 * marker it left expires on its own within minutes.
 */
function deviceName() {
    try {
        const kept = window.localStorage.getItem(DEVICE_KEY);

        if (kept) {
            return kept;
        }

        const made = crypto.randomUUID();

        window.localStorage.setItem(DEVICE_KEY, made);

        return made;
    } catch {
        // Storage can be refused outright — a private window, a browser set to
        // block it — and a claim under a fresh name every time still works.
        return crypto.randomUUID();
    }
}

/**
 * Who else has the map open, as they themselves claim.
 *
 * The other half of the collection this page writes into: the devices read it
 * to decide how hard to work, and the page reads it to say who is looking.
 * This browser's own marker is dropped — it is the one looking — while another
 * marker of the same member is kept, because a member watching from a device as
 * well is somebody to name.
 */
export function watchWatchers(memberId, onWatchers) {
    const own = `${memberId}.${deviceName()}`;

    return onSnapshot(collection(store, 'groups', currentGroup(), 'watch'), (snapshot) => {
        const looking = snapshot.docs
            .filter((document) => document.id !== own)
            .map((document) => {
                const data = document.data();
                const since = data.since?.toMillis?.();

                // A marker written a moment ago has no server time in it yet:
                // the field arrives with the write the server confirms.
                if (!data.memberId || !since) {
                    return null;
                }

                return { memberId: data.memberId, until: since + WATCH_AHEAD_MS };
            })
            .filter(Boolean);

        onWatchers(looking);
    });
}

/** What counts as somebody still being at the screen. */
export const ATTENTION = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'];

/**
 * Stops hearing from the database, and starts again: every listener stays in
 * place and is simply not fed, so a tab nobody looks at costs no reads.
 */
export function pauseDatabase() {
    return disableNetwork(store).catch(() => {});
}

export function resumeDatabase() {
    return enableNetwork(store).catch(() => {});
}

/**
 * Says that somebody is looking, for as long as somebody is.
 *
 * Returns the function that stops it. The claim is withdrawn on the way out,
 * but nothing depends on that arriving: it expires on its own, which is what
 * covers a tab closed by a crash or a laptop lid.
 *
 * Looking means a visible tab with somebody in front of it. A tab in the
 * background is not looking — the devices of the group would go on spending
 * their battery for a window nobody can see — and neither is a tab left open
 * on a desk for a quarter of an hour, nor a window left showing while its
 * viewer works in another one. The Android client says the same thing
 * by tying its claim to the screen being on and the map being in front.
 *
 * The claim carries the moment it was written by the server's clock rather
 * than the moment it runs out by this browser's: the device that reads it adds
 * the same seven minutes, so a viewer whose clock is wrong cannot hold the
 * group longer, and cannot fail to hold it at all.
 */
export function keepWatching(memberId, onError, onClaim = () => {}) {
    // Per browser rather than per account, the way the devices claim per
    // device: closing the page here must not withdraw the claim a device of the
    // same account had just made.
    const marker = doc(store, 'groups', currentGroup(), 'watch', `${memberId}.${deviceName()}`);

    let timer = null;
    let touched = Date.now();
    const { push, pushWhenDue, withdraw } = claimWriter(marker, memberId, onError);
    const looking = () =>
        document.visibilityState !== 'hidden'
        && document.hasFocus()
        && Date.now() - touched < IDLE_AFTER_MS;

    const claim = () => {
        if (timer) {
            return;
        }

        push();
        timer = setInterval(() => (looking() ? pushWhenDue() : release()), WATCH_CHECK_MS);
        onClaim(true);
    };

    const release = () => {
        if (!timer) {
            return;
        }

        clearInterval(timer);
        timer = null;
        withdraw();
        onClaim(false);
    };

    const review = () => (looking() ? claim() : release());

    const noticed = () => {
        touched = Date.now();

        // Only the first move after a pause has anything to do: while the
        // claim stands, this runs on every pointer event on the page.
        if (!timer) {
            review();
        }
    };

    const stopFollowing = followAttention(review, noticed);

    review();

    return () => {
        stopFollowing();

        if (timer) {
            clearInterval(timer);
            timer = null;
        }

        onClaim(false);

        return withdraw();
    };
}

/** Writes and withdraws the page's claim in [marker], keeping a write to one per refresh. */
function claimWriter(marker, memberId, onError) {
    // When a push last reached the server; a failed one leaves it where it
    // was, so the next check tries again rather than the next refresh.
    let pushed = 0;

    const push = () => setDoc(marker, { memberId, since: serverTimestamp() })
        .then(() => {
            pushed = Date.now();
        })
        .catch(onError);

    return {
        push,
        pushWhenDue: () => {
            if (Date.now() - pushed >= WATCH_REFRESH_MS) {
                push();
            }
        },
        withdraw: () => deleteDoc(marker).catch(onError),
    };
}

/**
 * Calls [review] whenever the page is shown, hidden, focused or left, and
 * [noticed] on every sign of somebody at it. Returns what stops both.
 */
function followAttention(review, noticed) {
    document.addEventListener('visibilitychange', review);
    window.addEventListener('focus', review);
    window.addEventListener('blur', review);
    ATTENTION.forEach((event) => window.addEventListener(event, noticed, { passive: true }));

    return () => {
        document.removeEventListener('visibilitychange', review);
        window.removeEventListener('focus', review);
        window.removeEventListener('blur', review);
        ATTENTION.forEach((event) => window.removeEventListener(event, noticed));
    };
}
