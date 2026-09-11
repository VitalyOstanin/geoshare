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

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const store = getFirestore(app);

export function watchSignIn(onChange) {
    return onAuthStateChanged(auth, (user) => onChange(user && { memberId: user.uid, name: user.displayName }));
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
 */
export function watchPositions(onPositions, onError) {
    return onSnapshot(
        collection(store, 'groups', GROUP, 'positions'),
        (snapshot) => onPositions(snapshot.docs.map(positionOf).filter(Boolean)),
        onError,
    );
}

function positionOf(document) {
    const data = document.data();

    // A document without coordinates is not a position, and the map is better
    // off leaving the member out than drawing them off the coast of Africa.
    if (typeof data.latitude !== 'number' || typeof data.longitude !== 'number' || !data.takenAt) {
        return null;
    }

    return {
        memberId: document.id,
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
 * Says that somebody is looking, for as long as this page is.
 *
 * Returns the function that stops it. The claim is withdrawn on the way out,
 * but nothing depends on that arriving: it expires on its own, which is what
 * covers a tab closed by a crash or a laptop lid.
 */
export function keepWatching(memberId, onError) {
    const marker = doc(store, 'groups', GROUP, 'watch', memberId);

    const push = () =>
        setDoc(marker, { until: Timestamp.fromMillis(Date.now() + WATCH_AHEAD_MS) }).catch(onError);

    push();

    const timer = setInterval(push, WATCH_REFRESH_MS);

    return () => {
        clearInterval(timer);

        return deleteDoc(marker).catch(onError);
    };
}
