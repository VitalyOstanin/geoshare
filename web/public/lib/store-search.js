// Searches, sirens, forgetting a phone and the private names a member gives
// the others. Split out of `store.js`, whose `store` and `currentGroup` this
// imports rather than keeping its own copies.

import {
    arrayRemove,
    collection,
    deleteDoc,
    deleteField,
    doc,
    onSnapshot,
    serverTimestamp,
    setDoc,
    updateDoc,
    writeBatch,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { labelsFrom } from './labels.js';
import { withinAnswerWait } from './link.js';
import { placesOf } from './false-places.js';
import { searchOf } from './searches.js';
import { sirenOf } from './sirens.js';
import { currentGroup, store } from './store.js';

/**
 * The places this group says a false signal puts phones at, and again
 * whenever an administrator names or moves one.
 *
 * A refusal is read as no places at all: the list only greys markers, and a
 * page that cannot read it draws them the way it did before any was named.
 */
export function watchFalsePlaces(onPlaces) {
    return onSnapshot(
        collection(store, 'groups', currentGroup(), 'falsePlaces'),
        (snapshot) => onPlaces(placesOf(snapshot.docs.map((document) => document.data()))),
        () => onPlaces([]),
    );
}

/**
 * The searches of this group, by phone, and again whenever an administrator
 * opens, renews or ends one, or the member whose phone it is says they were
 * found.
 *
 * The server's times come as its estimate while a write is on its way, so the
 * administrator who opened a search sees its end at once rather than after the
 * server has answered. A refusal is read as no searches: the page then draws
 * nobody as looked for, which is what it did before searches existed.
 */
export function watchSearches(onSearches) {
    return onSnapshot(
        collection(store, 'groups', currentGroup(), 'searches'),
        (snapshot) => {
            const searches = new Map();

            for (const document of snapshot.docs) {
                const data = document.data({ serverTimestamps: 'estimate' });
                const search = searchOf(document.id, {
                    ...data,
                    startedAt: data.startedAt?.toMillis?.(),
                    renewedAt: data.renewedAt?.toMillis?.(),
                });

                if (search !== null) {
                    searches.set(document.id, search);
                }
            }

            onSearches(searches);
        },
        () => onSearches(new Map()),
    );
}

/**
 * Opens a search for one phone: its place is open to the whole group for the
 * hours chosen. Written whole, so that one opened again after an earlier one
 * ran out starts from now and names whoever opened it this time. [phoneName]
 * is the phone's name as this administrator sees it, left out where there is
 * none, since the rules refuse anything but a string for a field they hold.
 */
export function openSearch(phoneId, adminId, hours, phoneName) {
    return withinAnswerWait(
        setDoc(doc(store, 'groups', currentGroup(), 'searches', phoneId), {
            openedBy: adminId,
            startedAt: serverTimestamp(),
            renewedBy: adminId,
            renewedAt: serverTimestamp(),
            hours,
            ...(phoneName ? { phoneName } : {}),
        }),
    );
}

/**
 * Moves the end of a standing search on to the hours chosen from now, keeping
 * who opened it and when, and refreshing the phone's name to what this
 * administrator sees now.
 */
export function renewSearch(phoneId, adminId, hours, phoneName) {
    return withinAnswerWait(
        updateDoc(doc(store, 'groups', currentGroup(), 'searches', phoneId), {
            renewedBy: adminId,
            renewedAt: serverTimestamp(),
            hours,
            ...(phoneName ? { phoneName } : {}),
        }),
    );
}

/** Ends a search: the phone was found, and its place closes to all but whom it is shared with. */
export function endSearch(phoneId) {
    return withinAnswerWait(
        deleteDoc(doc(store, 'groups', currentGroup(), 'searches', phoneId)),
    );
}

/**
 * A phone's siren, and again whenever it changes. There is one document per
 * phone, so a refusal — or a phone that was never called — is read as no
 * siren, not as an error to show.
 */
export function watchSiren(phoneId, onSiren) {
    return onSnapshot(
        doc(store, 'groups', currentGroup(), 'sirens', phoneId),
        (document) => {
            if (!document.exists()) {
                onSiren(null);

                return;
            }

            const data = document.data({ serverTimestamps: 'estimate' });

            onSiren(
                sirenOf(phoneId, {
                    ...data,
                    calledAt: data.calledAt?.toMillis?.(),
                    heardAt: data.heardAt?.toMillis?.(),
                    stoppedAt: data.stoppedAt?.toMillis?.(),
                }),
            );
        },
        () => onSiren(null),
    );
}

/**
 * Calls a phone's siren. Written whole rather than merged, since the rule
 * allows only these two fields on a call: a document left over from an
 * earlier one wipes its `heardAt` and `stopped*` along with it, so this call
 * starts from a clean one, as ringing again should.
 */
export function callSiren(phoneId, callerId) {
    return withinAnswerWait(
        setDoc(doc(store, 'groups', currentGroup(), 'sirens', phoneId), {
            calledBy: callerId,
            calledAt: serverTimestamp(),
        }),
    );
}

/** Stops a phone's siren, naming whoever stopped it. */
export function stopSiren(phoneId, stopperId) {
    return withinAnswerWait(
        updateDoc(doc(store, 'groups', currentGroup(), 'sirens', phoneId), {
            stoppedBy: stopperId,
            stoppedAt: serverTimestamp(),
        }),
    );
}

/**
 * Removes one of the member's phones from the group: its position, its claim
 * to be looking, its place among the phones the member gives out and its line
 * in the member's list, with the name the member gave it. The marker and the
 * line go for everybody. One batch, so the four go together or not at all;
 * the phone comes back if it is ever in touch under this account again.
 */
export function forgetPhone(phoneId) {
    const [memberId, deviceId] = phoneId.split('.');
    const group = ['groups', currentGroup()];
    const batch = writeBatch(store);

    batch.delete(doc(store, ...group, 'positions', phoneId));
    batch.delete(doc(store, ...group, 'watch', phoneId));
    batch.set(
        doc(store, ...group, 'grants', memberId),
        { devices: arrayRemove(deviceId) },
        { merge: true },
    );
    batch.set(
        doc(store, ...group, 'devices', memberId),
        { [deviceId]: deleteField() },
        { merge: true },
    );

    return batch.commit();
}

/**
 * What this member calls the others, and again whenever that changes — here,
 * on one of their phones or in another tab.
 *
 * A refusal means the browser is out of the group, and there is nobody left
 * for the names to name; a document not made yet is a member with no names.
 */
export function watchLabels(memberId, onLabels) {
    return onSnapshot(
        doc(store, 'groups', currentGroup(), 'labels', memberId),
        (snapshot) => onLabels(labelsFrom(snapshot.exists() ? snapshot.data() : null)),
        () => onLabels(labelsFrom(null)),
    );
}

/**
 * Gives a phone or a person a name for this member alone, or takes it away
 * when the name is null. `kind` is `devices` or `members`.
 *
 * Merged into the one map, so the other names stay as they were. The address
 * of a phone has a dot in it, which a merge takes as it is: only a field path
 * written as a string splits on dots.
 */
export async function writeLabel(memberId, kind, key, name) {
    await setDoc(
        doc(store, 'groups', currentGroup(), 'labels', memberId),
        { [kind]: { [key]: name ?? deleteField() } },
        { merge: true },
    );
}
