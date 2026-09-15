// Everything the page asks of Firebase: who is signed in, where the members
// are, and saying that somebody is looking.

import { initializeApp } from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js';
import {
    GoogleAuthProvider,
    getAuth,
    onAuthStateChanged,
    signInWithPopup,
    signOut,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js';
import {
    collection,
    deleteDoc,
    doc,
    getFirestore,
    onSnapshot,
    serverTimestamp,
    setDoc,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { firebaseConfig } from '../firebase-config.js';
import { positionMoments } from './age.js';

/** The one group this build shares within; membership is edited outside the clients. */
const GROUP = 'family';

/**
 * How often the claim to be looking is pushed forward.
 *
 * How long each one is worth is not said here and not said by this page: the
 * claim carries the moment the server wrote it, and the phone reading it adds
 * the three minutes both clients agree on. A viewer whose clock is wrong can
 * then neither hold the group longer nor fail to hold it at all.
 */
const WATCH_REFRESH_MS = 60 * 1000;

/**
 * How long a claim to be looking is worth, counted from the moment the server
 * wrote it down.
 *
 * The same three minutes the phones add, and for the same reason: the claim
 * carries when it was made rather than when it ends, so whoever reads it
 * decides how long to believe it. Longer than the refresh above, so that one
 * write lost to a bad connection does not drop a viewer off the line.
 */
const WATCH_AHEAD_MS = 3 * 60 * 1000;

/**
 * How long a tab nobody touches goes on claiming to be looking.
 *
 * A tab left open on a laptop its owner walked away from holds every phone of
 * the group in the fifteen-second mode, which is the most expensive thing the
 * group does. Fifteen minutes is long enough to watch somebody drive home and
 * short enough that a forgotten tab stops costing within the hour.
 */
const IDLE_AFTER_MS = 15 * 60 * 1000;

/**
 * How long to wait before asking again for a document that was refused.
 *
 * A refusal is usually the member saying they no longer share, but it is also
 * what an expired permission and a permission not yet granted look like, and
 * neither of those changes the profiles the subscriptions are built from.
 */
const RETRY_AFTER_MS = 60 * 1000;

/** Where this browser keeps the name it watches under, so a reload keeps one marker. */
const DEVICE_KEY = 'geoshare.device';

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const store = getFirestore(app);

export function watchSignIn(onChange) {
    return onAuthStateChanged(auth, (user) =>
        onChange(
            user && {
                memberId: user.uid,
                name: user.displayName,
                picture: user.photoURL,
            },
        ),
    );
}

export function signIn() {
    // A popup rather than a redirect: the page keeps its state, and there is
    // no return trip to get wrong.
    return signInWithPopup(auth, new GoogleAuthProvider());
}

export function leave() {
    return signOut(auth);
}

/**
 * The positions, as they change.
 *
 * A subscription rather than a read on a timer: a member who moves writes a
 * new position, and the marker follows within a second without the page
 * asking for anything.
 *
 * One subscription per document, because that is the only shape the
 * permissions allow. A document this account may not read answers with a
 * refusal rather than with nothing, and that refusal is the member saying they
 * no longer share — so it drops the marker instead of being reported.
 */
export function watchPositions(documents, onPositions, onRefused) {
    const known = new Map();
    const refused = new Set();
    const publish = () => onPositions([...known.values()]);
    const stops = new Map();
    const waiting = new Set();
    let wanted = true;

    const sayRefused = () => onRefused?.(refused.size);

    const follow = (id) => {
        stops.set(
            id,
            onSnapshot(
                doc(store, 'groups', GROUP, 'positions', id),
                (snapshot) => {
                    const position = snapshot.exists() ? positionOf(snapshot) : null;

                    if (refused.delete(id)) {
                        sayRefused();
                    }

                    if (position) {
                        known.set(id, position);
                    } else if (!known.delete(id)) {
                        return;
                    }

                    publish();
                },
                () => {
                    if (known.delete(id)) {
                        publish();
                    }

                    refused.add(id);
                    sayRefused();
                    askAgainLater(id);
                },
            ),
        );
    };

    // A refused document stays refused until the member says otherwise, and
    // what they say does not reach this page: a permission lives in their
    // grant, which a refused reader cannot see either. So the
    // subscription is made again on a timer, and a permission handed back
    // brings the marker back with it instead of waiting for a reload.
    const askAgainLater = (id) => {
        stops.get(id)?.();
        stops.delete(id);

        if (!wanted) {
            return;
        }

        const timer = setTimeout(() => {
            waiting.delete(timer);

            if (wanted) {
                follow(id);
            }
        }, RETRY_AFTER_MS);

        waiting.add(timer);
    };

    documents.forEach(follow);

    // Said once at the start, so that a group with nothing to draw shows an
    // empty map rather than waiting for an answer that never comes.
    publish();

    return () => {
        wanted = false;
        waiting.forEach(clearTimeout);
        waiting.clear();
        stops.forEach((stop) => stop());
        stops.clear();
    };
}

/** The widest answer still drawn as a circle. The rules say the same number. */
const WIDEST_CIRCLE_METRES = 100000;

function positionOf(snapshot) {
    const data = snapshot.data();

    // A document without usable coordinates is not a position, and the map is
    // better off leaving the member out than drawing them off the coast of
    // Africa — or, for a number far outside the globe, than asking the map to
    // place a point that is not on it.
    if (!placeable(data.latitude, -90) || !placeable(data.longitude, -180) || !data.takenAt) {
        return null;
    }

    return {
        // One document per phone: the id is the account and the phone joined
        // by a dot, and the owner is repeated inside. A document from before
        // positions were kept per phone has no owner field, and there the
        // whole id is the account.
        positionId: snapshot.id,
        memberId: typeof data.memberId === 'string' ? data.memberId : snapshot.id.split('.')[0],
        deviceName: typeof data.deviceName === 'string' ? data.deviceName : null,
        latitude: data.latitude,
        longitude: data.longitude,
        takenAt: data.takenAt.toMillis(),
        // The server stamp dates the news and the fix dates the position; the
        // two part under a spoof, and which is which is worked out in one
        // place the tests can reach.
        ...positionMoments(data.takenAt.toMillis(), data.writtenAt?.toMillis() ?? null),
        mode: data.mode ?? null,
        batteryPercent: typeof data.batteryPercent === 'number' ? data.batteryPercent : null,
        // Absent in what the older builds wrote, which is a phone not known to
        // be charging rather than a phone known not to be.
        charging: data.charging === true,
        accuracyMetres: widthOf(data.accuracyMetres),
        // How the phone says it arrived at the answer, in the word it wrote.
        // Absent in what the older builds wrote, which is a source not known
        // rather than a position arrived at by no means at all.
        foundBy: typeof data.foundBy === 'string' ? data.foundBy : null,
        // How many answers this phone has thrown away since it wrote this
        // position, and why the last of them went. A phone under a
        // transmitter writes nothing at all, and without these its marker
        // simply gets older — which is what a phone in a drawer does too.
        leftOutCount: typeof data.leftOutCount === 'number' ? data.leftOutCount : null,
        leftOutReason: typeof data.leftOutReason === 'string' ? data.leftOutReason : null,
    };
}

/**
 * The width of the answer, where the document holds one worth drawing.
 *
 * A width is a radius, so nought and less are not widths at all; and one wider
 * than a hundred kilometres is a circle covering everything the map can show,
 * which says less about where the phone is than the marker alone does. The
 * rules refuse both, and this is what happens to a document written before
 * they did.
 */
function widthOf(metres) {
    if (typeof metres !== 'number' || !Number.isFinite(metres)) {
        return null;
    }

    return metres > 0 && metres <= WIDEST_CIRCLE_METRES ? metres : null;
}

/**
 * Whether a coordinate is a number the globe has a place for.
 *
 * Written by another member's phone, so it is worth as much as that phone: a
 * field of the wrong type, an infinity, or a degree count far outside the
 * globe all reach the clients, and the rules do not stop them.
 */
function placeable(degrees, limit) {
    return typeof degrees === 'number' && Number.isFinite(degrees) && degrees >= limit && degrees <= -limit;
}

/**
 * Who the members are, as they say so themselves.
 *
 * Names change about never, so this costs one read per member and then
 * nothing; keeping it a subscription rather than a single read is what lets a
 * member who renames their account stop being called by the old name without
 * anybody reloading the page.
 */
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

export function watchProfiles(onProfiles, onError) {
    return onSnapshot(
        collection(store, 'groups', GROUP, 'members'),
        (snapshot) => onProfiles(new Map(snapshot.docs.map(profileOf).filter(Boolean))),
        onError,
    );
}

function profileOf(document) {
    const data = document.data();

    // A profile without a name says nothing the id does not already say, and
    // both clients fall back to the id on their own.
    if (typeof data.displayName !== 'string' || data.displayName.trim() === '') {
        return null;
    }

    return [
        document.id,
        {
            memberId: document.id,
            displayName: data.displayName,
            photoUrl: typeof data.photoUrl === 'string' && data.photoUrl !== '' ? data.photoUrl : null,
        },
    ];
}

/**
 * What each member gives out, as far as this viewer is allowed to know.
 *
 * One subscription per member rather than one over the collection: the grants
 * cannot be listed, because listing them would hand the phones of the whole
 * household to a member who was given nothing. A member who shares with this
 * viewer answers with their phones; one who does not answers with a refusal,
 * which is simply an absence here — the same member is already missing from
 * the map. The subscription is made again on a timer, so a permission handed
 * back arrives without a reload.
 */
export function watchGrants(memberIds, onGrants) {
    const known = new Map();
    const stops = new Map();
    const waiting = new Set();
    let wanted = true;

    const publish = () => onGrants(new Map(known));

    const follow = (memberId) => {
        stops.set(
            memberId,
            onSnapshot(
                doc(store, 'groups', GROUP, 'grants', memberId),
                (snapshot) => {
                    const devices = snapshot.exists() ? devicesOf(snapshot) : [];

                    known.set(memberId, { memberId, devices });
                    publish();
                },
                () => {
                    known.delete(memberId);
                    publish();
                    askAgainLater(memberId);
                },
            ),
        );
    };

    const askAgainLater = (memberId) => {
        stops.get(memberId)?.();
        stops.delete(memberId);

        if (!wanted) {
            return;
        }

        const timer = setTimeout(() => {
            waiting.delete(timer);

            if (wanted) {
                follow(memberId);
            }
        }, RETRY_AFTER_MS);

        waiting.add(timer);
    };

    memberIds.forEach(follow);

    // Said once at the start, so that a viewer nobody shares with draws an
    // empty map rather than waiting for an answer that never comes.
    publish();

    return () => {
        wanted = false;
        waiting.forEach(clearTimeout);
        waiting.clear();
        stops.forEach((stop) => stop());
        stops.clear();
    };
}

/** The phones a grant names: the positions cannot be listed, so this is the list to read. */
function devicesOf(snapshot) {
    const data = snapshot.data();

    return Array.isArray(data.devices) ? data.devices.filter((one) => typeof one === 'string') : [];
}

/**
 * Says who this viewer is, when the account says something the group has not
 * been told.
 *
 * Skipped when the stored profile already matches: a name changes about never,
 * and the free tier counts every write.
 */
export function publishProfile(member, known, onError) {
    const displayName = member.name?.trim();

    if (!displayName) {
        return Promise.resolve();
    }

    const photoUrl = member.picture ?? null;

    if (known && known.displayName === displayName && known.photoUrl === photoUrl) {
        return Promise.resolve();
    }

    return setDoc(
        doc(store, 'groups', GROUP, 'members', member.memberId),
        {
            displayName,
            // Written as a field rather than left out, so that an account that
            // loses its picture loses it on the other screens too.
            photoUrl,
            updatedAt: serverTimestamp(),
        },
        // Merged rather than replacing the document, so that a field added
        // here later is not wiped by a client that predates it.
        { merge: true },
    ).catch(onError);
}

/**
 * Who else has the map open, as they themselves claim.
 *
 * The other half of the collection this page writes into: the phones read it
 * to decide how hard to work, and the page reads it to say who is looking.
 * This browser's own marker is dropped — it is the one looking — while another
 * marker of the same member is kept, because a member watching from a phone as
 * well is somebody to name.
 */
export function watchWatchers(memberId, onWatchers) {
    const own = `${memberId}.${deviceName()}`;

    return onSnapshot(collection(store, 'groups', GROUP, 'watch'), (snapshot) => {
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
const ATTENTION = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'];

/**
 * Says that somebody is looking, for as long as somebody is.
 *
 * Returns the function that stops it. The claim is withdrawn on the way out,
 * but nothing depends on that arriving: it expires on its own, which is what
 * covers a tab closed by a crash or a laptop lid.
 *
 * Looking means a visible tab with somebody in front of it. A tab in the
 * background is not looking — the phones of the group would go on spending
 * their battery for a window nobody can see — and neither is a tab left open
 * on a desk for a quarter of an hour. The Android client says the same thing
 * by tying its claim to the screen being on and the map being in front.
 *
 * The claim carries the moment it was written by the server's clock rather
 * than the moment it runs out by this browser's: the phone that reads it adds
 * the same three minutes, so a viewer whose clock is wrong cannot hold the
 * group longer, and cannot fail to hold it at all.
 */
export function keepWatching(memberId, onError) {
    // Per browser rather than per account, the way the phones claim per phone:
    // closing the page here must not withdraw the claim a phone of the same
    // account had just made.
    const marker = doc(store, 'groups', GROUP, 'watch', `${memberId}.${deviceName()}`);

    let timer = null;
    let touched = Date.now();

    const push = () => setDoc(marker, { memberId, since: serverTimestamp() }).catch(onError);
    const withdraw = () => deleteDoc(marker).catch(onError);
    const looking = () =>
        document.visibilityState !== 'hidden' && Date.now() - touched < IDLE_AFTER_MS;

    const claim = () => {
        if (timer) {
            return;
        }

        push();
        timer = setInterval(() => (looking() ? push() : release()), WATCH_REFRESH_MS);
    };

    const release = () => {
        if (!timer) {
            return;
        }

        clearInterval(timer);
        timer = null;
        withdraw();
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

    document.addEventListener('visibilitychange', review);
    ATTENTION.forEach((event) => window.addEventListener(event, noticed, { passive: true }));

    review();

    return () => {
        document.removeEventListener('visibilitychange', review);
        ATTENTION.forEach((event) => window.removeEventListener(event, noticed));

        if (timer) {
            clearInterval(timer);
            timer = null;
        }

        return withdraw();
    };
}
