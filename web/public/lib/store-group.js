// The roster of the group and the membership of it: who is on it, inviting
// somebody in, taking somebody off, running it, naming it, and what each
// member gives out to the others. Split out of `store.js`, whose `store`,
// `currentGroup`, `rememberGroup` and `RETRY_AFTER_MS` this imports rather
// than keeping its own copies.

import {
    arrayRemove,
    arrayUnion,
    collection,
    doc,
    onSnapshot,
    serverTimestamp,
    setDoc,
    updateDoc,
    writeBatch,
} from 'https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js';
import { pictureAddress, profileName } from './members.js';
import { combinedGrants, phonesOf } from './phones.js';
import {
    INVITATION_LASTS_MS,
    newInvitationCode,
    rosterWithout,
} from './membership.js';
import { currentGroup, refusals, rememberGroup, store } from './store.js';

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
 * A field that ought to be a list of account ids, read as one.
 *
 * Anything else is read as nothing rather than thrown: the menu then says
 * there is no group instead of failing, and what is actually in the document
 * is a question for the console.
 */
export function rosterOf(snapshot) {
    const data = snapshot.data();

    return {
        members: idsOf(data.members),
        admins: idsOf(data.admins),
        // Whoever the document names as its owner, or else the account the
        // group is named after: the rules read the founder the same way.
        founder: typeof data.owner === 'string' ? data.owner : snapshot.id,
        // What the group is called, or null where nobody has named it yet
        // (see `lib/group-name.js`, ADR-0016).
        name: typeof data.name === 'string' ? data.name : null,
    };
}

export function idsOf(field) {
    return Array.isArray(field) ? field.filter((id) => typeof id === 'string') : [];
}

/**
 * Makes a group with the founder alone in it, and stays in it.
 *
 * The document is named after the account that made it regardless of `name`:
 * that is what keeps one account from making groups without end. `name` is
 * only the group's own name, optional and chosen once here or given later by
 * an administrator (ADR-0016) — left out of the write entirely rather than
 * sent empty, since the rules refuse a name that is nothing but spaces. The
 * browser is moved into the new group only once the write has been allowed —
 * a viewer whose founding was refused would otherwise be left looking at a
 * group that does not exist.
 */
export async function foundGroup(memberId, name = '') {
    const trimmed = name.trim();

    await setDoc(doc(store, 'groups', memberId), {
        members: [memberId],
        admins: [memberId],
        ...(trimmed === '' ? {} : { name: trimmed }),
    });

    rememberGroup(memberId);
}

/**
 * Makes an invitation and returns the whole of what is handed over: the
 * group and code the link is built from, when it stops working, and the
 * address it was made out to (ADR-0015, ADR-0019) — everything the admin
 * panel needs to build the link and say when it runs out, without reading
 * anything back.
 *
 * Nothing is read back. The code is the name of the document, and nobody may
 * read that collection at all — a reader could collect the codes of a group
 * and let themselves in.
 */
export async function makeInvitation(memberId, email) {
    const group = currentGroup();
    const code = newInvitationCode();
    const trimmedEmail = email.trim();
    const expiresAt = new Date(Date.now() + INVITATION_LASTS_MS);

    await setDoc(doc(store, 'groups', group, 'invitations', code), {
        createdBy: memberId,
        expiresAt,
        email: trimmedEmail,
    });

    return { groupId: group, code, expiresAt, email: trimmedEmail };
}

/**
 * Joins a group on an invitation: the code, the roster and the spending of the
 * code, in one batch.
 *
 * One batch because the rules weigh the three together: the roster grows only
 * if the code the claim names is gone afterwards, and the code goes only if
 * the roster grows — which is what makes a code admit one person once.
 *
 * Which group is not this browser's to assume — the invitation says so, and
 * every write is addressed by that name rather than by the group being looked
 * at, which until this succeeds is a different one.
 *
 * The roster is not read, and cannot be: the group document is closed to
 * everybody who is not on it. So the server is asked to append rather than
 * told the whole list.
 */
export async function joinGroup(memberId, invitation) {
    const group = doc(store, 'groups', invitation.group);

    await writeBatch(store)
        .set(doc(group, 'joins', memberId), { code: invitation.code })
        .update(group, { members: arrayUnion(memberId) })
        .delete(doc(group, 'invitations', invitation.code))
        .commit();

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

/**
 * Gives the group a name, or another one.
 *
 * The name alone, as the rules require of this write (`renaming()`): the
 * roster is an administrator's to change too, but by a different write, and
 * one that touches both at once would be refused for reaching past what
 * either rule allows on its own.
 */
export function renameGroup(name) {
    return updateDoc(doc(store, 'groups', currentGroup()), { name: name.trim() });
}

/**
 * Who the members are, as they say so themselves.
 *
 * Names change about never, so this costs one read per member and then
 * nothing; keeping it a subscription rather than a single read is what lets a
 * member who renames their account stop being called by the old name without
 * anybody reloading the page.
 */
export function watchProfiles(onProfiles, onError) {
    return onSnapshot(
        collection(store, 'groups', currentGroup(), 'members'),
        (snapshot) => onProfiles(new Map(snapshot.docs.map(profileOf).filter(Boolean))),
        onError,
    );
}

export function profileOf(document) {
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
            // When the member last touched the profile; what a permission
            // handed back also does, so a change here is the cue to ask again.
            updatedAt: typeof data.updatedAt?.toMillis === 'function' ? data.updatedAt.toMillis() : null,
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
 * timer (and at once when a profile changes), so a permission
 * handed back arrives without a reload.
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
 * while after a refusal: at once when a profile changes, otherwise on the
 * slow timer of `RETRY_AFTER_MS`. The callback hears a snapshot, or null for a
 * refusal.
 */
export function followEach(memberIds, collectionName, onDocument) {
    const stops = new Map();
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

        refusals.retryLater(`${collectionName}/${memberId}`, () => {
            if (wanted) {
                follow(memberId);
            }
        });
    };

    memberIds.forEach(follow);

    return () => {
        wanted = false;
        memberIds.forEach((memberId) => refusals.forget(`${collectionName}/${memberId}`));
        stops.forEach((stop) => stop());
        stops.clear();
    };
}

/** The phones a grant names: the positions cannot be listed, so this is the list to read. */
export function devicesOf(snapshot) {
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
    const displayName = profileName(member.name, member.email);

    if (!displayName) {
        return Promise.resolve();
    }

    const photoUrl = pictureAddress(member.picture);

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
