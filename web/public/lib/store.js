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
    Timestamp,
    collection,
    deleteDoc,
    doc,
    getFirestore,
    onSnapshot,
    serverTimestamp,
    setDoc,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { firebaseConfig } from '../firebase-config.js';

/** The one group this build shares within; membership is edited outside the clients. */
const GROUP = 'family';

/**
 * How far ahead a viewer claims to be looking, and how often the claim is
 * pushed forward. The same pair the Android client uses: a lost connection
 * must not drop the phones out of their fast mode, and a browser tab that
 * disappears must stop being a viewer within minutes.
 */
const WATCH_AHEAD_MS = 3 * 60 * 1000;
const WATCH_REFRESH_MS = 60 * 1000;

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
export function watchPositions(documents, onPositions) {
    const known = new Map();
    const publish = () => onPositions([...known.values()]);

    const stops = documents.map((id) =>
        onSnapshot(
            doc(store, 'groups', GROUP, 'positions', id),
            (snapshot) => {
                const position = snapshot.exists() ? positionOf(snapshot) : null;

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
            },
        ),
    );

    // Said once at the start, so that a group with nothing to draw shows an
    // empty map rather than waiting for an answer that never comes.
    publish();

    return () => stops.forEach((stop) => stop());
}

function positionOf(document) {
    const data = document.data();

    // A document without coordinates is not a position, and the map is better
    // off leaving the member out than drawing them off the coast of Africa.
    if (typeof data.latitude !== 'number' || typeof data.longitude !== 'number' || !data.takenAt) {
        return null;
    }

    return {
        // One document per phone: the id is the account and the phone joined
        // by a dot, and the owner is repeated inside. A document from before
        // positions were kept per phone has no owner field, and there the
        // whole id is the account.
        positionId: document.id,
        memberId: typeof data.memberId === 'string' ? data.memberId : document.id.split('.')[0],
        deviceName: typeof data.deviceName === 'string' ? data.deviceName : null,
        latitude: data.latitude,
        longitude: data.longitude,
        takenAt: data.takenAt.toMillis(),
        // The server stamp is the one clock every member shares; the phone
        // that took the fix may be minutes off.
        knownAt: (data.writtenAt ?? data.takenAt).toMillis(),
        mode: data.mode ?? null,
        batteryPercent: typeof data.batteryPercent === 'number' ? data.batteryPercent : null,
    };
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
            // The phones this member carries: the positions cannot be listed,
            // so this is how the page learns which documents to read.
            devices: Array.isArray(data.devices) ? data.devices.filter((one) => typeof one === 'string') : [],
        },
    ];
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
        // Merged rather than replacing the document: the permissions and the
        // phones of this member live there too, and neither is this write's
        // business.
        { merge: true },
    ).catch(onError);
}

/**
 * Says that somebody is looking, for as long as this page is.
 *
 * Returns the function that stops it. The claim is withdrawn on the way out,
 * but nothing depends on that arriving: it expires on its own, which is what
 * covers a tab closed by a crash or a laptop lid.
 */
export function keepWatching(memberId, onError) {
    // Per browser rather than per account, the way the phones claim per phone:
    // closing the page here must not withdraw the claim a phone of the same
    // account had just made.
    const marker = doc(store, 'groups', GROUP, 'watch', `${memberId}.${deviceName()}`);

    const push = () =>
        setDoc(marker, {
            memberId,
            until: Timestamp.fromMillis(Date.now() + WATCH_AHEAD_MS),
        }).catch(onError);

    push();

    const timer = setInterval(push, WATCH_REFRESH_MS);

    return () => {
        clearInterval(timer);

        return deleteDoc(marker).catch(onError);
    };
}
