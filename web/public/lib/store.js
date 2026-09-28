// Everything the page asks of Firebase: who is signed in, where the members
// are, and saying that somebody is looking.

import { initializeApp } from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js';
import {
    GoogleAuthProvider,
    getAuth,
    getRedirectResult,
    onAuthStateChanged,
    signInWithRedirect,
    signOut,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js';
import {
    arrayRemove,
    arrayUnion,
    collection,
    deleteDoc,
    deleteField,
    doc,
    getFirestore,
    onSnapshot,
    serverTimestamp,
    setDoc,
    updateDoc,
    writeBatch,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { firebaseConfig } from '../firebase-config.js';
import { positionMoments } from './age.js';
import { labelsFrom } from './labels.js';
import { placesOf } from './false-places.js';
import { combinedGrants, phonesOf } from './phones.js';
import { searchOf } from './searches.js';
import { sirenOf } from './sirens.js';
import {
    INVITATION_LASTS_MS,
    invitationText,
    newInvitationCode,
    rosterWithout,
} from './membership.js';

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
 * What a browser that has joined nothing assumes, because those phones and
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
function rememberGroup(group, storage = window.localStorage) {
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
 * claim carries the moment the server wrote it, and the phone reading it adds
 * the seven minutes both clients agree on. A viewer whose clock is wrong can
 * then neither hold the group longer nor fail to hold it at all.
 *
 * Every phone of the group reads each push, so in a group of twenty a push a
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
 * an event, and waiting five minutes to notice would keep the phones in their
 * fast mode that much longer; and a lost push waited for five minutes would
 * let the claim run out before the next one.
 */
const WATCH_CHECK_MS = 60 * 1000;

/**
 * How long a claim to be looking is worth, counted from the moment the server
 * wrote it down.
 *
 * The same seven minutes the phones add, and for the same reason: the claim
 * carries when it was made rather than when it ends, so whoever reads it
 * decides how long to believe it. Longer than the refresh above, so that one
 * write lost to a bad connection does not drop a viewer off the line.
 */
const WATCH_AHEAD_MS = 7 * 60 * 1000;

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
    // A trip to Google and back rather than a popup: phone browsers ask
    // before opening a window, open it as a tab, and block it outright when
    // it comes too long after the tap. The page is loaded afresh on the way
    // back, and nothing on the sign-in screen is lost with it.
    return signInWithRedirect(auth, new GoogleAuthProvider());
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
                doc(store, 'groups', currentGroup(), 'positions', id),
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
        // Whether the phone said its receiver had not settled since it was
        // switched on. Absent in what the older builds wrote and in every
        // position of a phone that has been up a while, which are one thing
        // to a reader: nothing to say beyond what the other fields say.
        settling: data.settling === true,
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

/**
 * Who is in the group, as the group document holds it.
 *
 * A subscription rather than a read: somebody joins on a code while the menu
 * is open, and an administrator taking a member off has to see them go.
 *
 * A refusal is read as no roster at all rather than reported. The group
 * document is closed to everybody who is not on it, so a refusal is this
 * account saying it is not in the group it is looking at — which is exactly
 * what the screen offering a way in has to know.
 */
export function watchRoster(onRoster) {
    return onSnapshot(
        doc(store, 'groups', currentGroup()),
        (snapshot) => onRoster(snapshot.exists() ? rosterOf(snapshot) : null),
        () => onRoster(null),
    );
}

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
 * The searches of this group, by member, and again whenever an administrator
 * opens, renews or ends one, or the member looked for says they were found.
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
 * Opens a search for a member: their place is open to the whole group for the
 * hours chosen. Written whole, so that one opened again after an earlier one
 * ran out starts from now and names whoever opened it this time.
 */
export function openSearch(memberId, adminId, hours) {
    return setDoc(doc(store, 'groups', currentGroup(), 'searches', memberId), {
        openedBy: adminId,
        startedAt: serverTimestamp(),
        renewedBy: adminId,
        renewedAt: serverTimestamp(),
        hours,
    });
}

/**
 * Moves the end of a standing search on to the hours chosen from now, keeping
 * who opened it and when.
 */
export function renewSearch(memberId, adminId, hours) {
    return updateDoc(doc(store, 'groups', currentGroup(), 'searches', memberId), {
        renewedBy: adminId,
        renewedAt: serverTimestamp(),
        hours,
    });
}

/** Ends a search: the member was found, and their place closes to all but whom they share with. */
export function endSearch(memberId) {
    return deleteDoc(doc(store, 'groups', currentGroup(), 'searches', memberId));
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
    return setDoc(doc(store, 'groups', currentGroup(), 'sirens', phoneId), {
        calledBy: callerId,
        calledAt: serverTimestamp(),
    });
}

/** Stops a phone's siren, naming whoever stopped it. */
export function stopSiren(phoneId, stopperId) {
    return updateDoc(doc(store, 'groups', currentGroup(), 'sirens', phoneId), {
        stoppedBy: stopperId,
        stoppedAt: serverTimestamp(),
    });
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

/**
 * A field that ought to be a list of account ids, read as one.
 *
 * Anything else is read as nothing rather than thrown: the menu then says
 * there is no group instead of failing, and what is actually in the document
 * is a question for the console.
 */
function rosterOf(snapshot) {
    const data = snapshot.data();

    return {
        members: idsOf(data.members),
        admins: idsOf(data.admins),
        // Whoever the document names as its owner, or else the account the
        // group is named after: the rules read the founder the same way.
        founder: typeof data.owner === 'string' ? data.owner : snapshot.id,
    };
}

function idsOf(field) {
    return Array.isArray(field) ? field.filter((id) => typeof id === 'string') : [];
}

/**
 * Makes a group with the founder alone in it, and stays in it.
 *
 * There is no name to choose: a group is named after the account that made it,
 * which is what keeps one account from making them without end. The browser is
 * moved into the new group only once the write has been allowed — a viewer
 * whose founding was refused would otherwise be left looking at a group that
 * does not exist.
 */
export async function foundGroup(memberId) {
    await setDoc(doc(store, 'groups', memberId), {
        members: [memberId],
        admins: [memberId],
    });

    rememberGroup(memberId);
}

/**
 * Makes an invitation and returns the whole of what is handed over: the code
 * with the name of this group in front of it.
 *
 * Nothing is read back. The code is the name of the document, and nobody may
 * read that collection at all — a reader could collect the codes of a group
 * and let themselves in.
 */
export async function makeInvitation(memberId) {
    const group = currentGroup();
    const code = newInvitationCode();

    await setDoc(doc(store, 'groups', group, 'invitations', code), {
        createdBy: memberId,
        expiresAt: new Date(Date.now() + INVITATION_LASTS_MS),
    });

    return invitationText(group, code);
}

/**
 * Joins a group on an invitation: the code first, the roster second.
 *
 * In that order because the rule weighing the roster reads the code from the
 * document written first. Which group is not this browser's to assume — the
 * invitation says so, and both writes are addressed by that name rather than
 * by the group being looked at, which until this succeeds is a different one.
 *
 * The roster is not read, and cannot be: the group document is closed to
 * everybody who is not on it. So the server is asked to append rather than
 * told the whole list.
 */
export async function joinGroup(memberId, invitation) {
    await setDoc(doc(store, 'groups', invitation.group, 'joins', memberId), {
        code: invitation.code,
    });

    await updateDoc(doc(store, 'groups', invitation.group), {
        members: arrayUnion(memberId),
    });

    rememberGroup(invitation.group);
}

/**
 * Takes a member off the roster, and off the administrators with it.
 *
 * Leaving them among the administrators would leave somebody outside the group
 * still able to say who is in it, and the rules refuse such a write whole
 * rather than perform half of it.
 */
export function removeMember(roster, memberId) {
    // The two lists alone: a whole document written back would drop the
    // founder and the name of the group, and the rules refuse a write that
    // touches either.
    return updateDoc(doc(store, 'groups', currentGroup()), rosterWithout(roster, memberId));
}

/**
 * Hands the running of the group to a member, or takes it back.
 *
 * One element added or taken rather than the list written whole, so that two
 * administrators changing different members at once do not undo each other.
 */
export function setRunning(memberId, runs) {
    return updateDoc(doc(store, 'groups', currentGroup()), {
        admins: runs ? arrayUnion(memberId) : arrayRemove(memberId),
    });
}

export function watchProfiles(onProfiles, onError) {
    return onSnapshot(
        collection(store, 'groups', currentGroup(), 'members'),
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
 * What each member gives out, as far as this viewer is allowed to know, and
 * what their phones last said about themselves.
 *
 * One subscription per member and document rather than one over a collection:
 * neither the grants nor the phones can be listed, because listing them would
 * hand the phones of the whole household to a member who was given nothing. A
 * member who shares with this viewer answers with their phones; one who does
 * not answers with a refusal, which is simply an absence here — the same member
 * is already missing from the map. The subscriptions are made again on a
 * timer, so a permission handed back arrives without a reload.
 *
 * The phones are followed apart from the positions: whether a phone is in
 * touch, how charged it is and whether it is on a cable are not a place, and
 * a phone whose answers are all being thrown away still says them. An
 * administrator reads them without being given a permission, which is why the
 * list of documents to follow still comes from the grant alone.
 */
export function watchGrants(memberIds, onGrants) {
    const named = new Map();
    const phones = new Map();

    const publish = () => onGrants(combinedGrants(named, phones));

    const stopGrants = followEach(memberIds, 'grants', (memberId, snapshot) => {
        if (snapshot === null) {
            named.delete(memberId);
        } else {
            named.set(memberId, snapshot.exists() ? devicesOf(snapshot) : []);
        }
        publish();
    });
    const stopPhones = followEach(memberIds, 'devices', (memberId, snapshot) => {
        if (snapshot === null || !snapshot.exists()) {
            phones.delete(memberId);
        } else {
            phones.set(memberId, phonesOf(snapshot.data()));
        }
        publish();
    });

    // Said once at the start, so that a viewer nobody shares with draws an
    // empty map rather than waiting for an answer that never comes.
    publish();

    return () => {
        stopGrants();
        stopPhones();
    };
}

/**
 * One document of each member of a collection, followed and asked for again a
 * minute after a refusal. The callback hears a snapshot, or null for a
 * refusal.
 */
function followEach(memberIds, collectionName, onDocument) {
    const stops = new Map();
    const waiting = new Set();
    let wanted = true;

    const follow = (memberId) => {
        stops.set(
            memberId,
            onSnapshot(
                doc(store, 'groups', currentGroup(), collectionName, memberId),
                (snapshot) => onDocument(memberId, snapshot),
                () => {
                    onDocument(memberId, null);
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
        doc(store, 'groups', currentGroup(), 'members', member.memberId),
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
 * on a desk for a quarter of an hour, nor a window left showing while its
 * viewer works in another one. The Android client says the same thing
 * by tying its claim to the screen being on and the map being in front.
 *
 * The claim carries the moment it was written by the server's clock rather
 * than the moment it runs out by this browser's: the phone that reads it adds
 * the same seven minutes, so a viewer whose clock is wrong cannot hold the
 * group longer, and cannot fail to hold it at all.
 */
export function keepWatching(memberId, onError, onClaim = () => {}) {
    // Per browser rather than per account, the way the phones claim per phone:
    // closing the page here must not withdraw the claim a phone of the same
    // account had just made.
    const marker = doc(store, 'groups', currentGroup(), 'watch', `${memberId}.${deviceName()}`);

    let timer = null;
    let touched = Date.now();
    // When a push last reached the server; a failed one leaves it where it
    // was, so the next check tries again rather than the next refresh.
    let pushed = 0;

    const push = () => setDoc(marker, { memberId, since: serverTimestamp() })
        .then(() => {
            pushed = Date.now();
        })
        .catch(onError);
    const pushWhenDue = () => {
        if (Date.now() - pushed >= WATCH_REFRESH_MS) {
            push();
        }
    };
    const withdraw = () => deleteDoc(marker).catch(onError);
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

    document.addEventListener('visibilitychange', review);
    window.addEventListener('focus', review);
    window.addEventListener('blur', review);
    ATTENTION.forEach((event) => window.addEventListener(event, noticed, { passive: true }));

    review();

    return () => {
        document.removeEventListener('visibilitychange', review);
        window.removeEventListener('focus', review);
        window.removeEventListener('blur', review);
        ATTENTION.forEach((event) => window.removeEventListener(event, noticed));

        if (timer) {
            clearInterval(timer);
            timer = null;
        }

        onClaim(false);

        return withdraw();
    };
}
