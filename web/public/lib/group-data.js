// What the documents of a group hold, read into what the page works with:
// the roster, the members' profiles and the devices a grant names. Kept apart
// from `store-group.js`, which subscribes to them, so that the tests reach
// it without Firebase.

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

/**
 * A field that ought to be a list of account ids, read as one.
 *
 * Anything else is read as nothing rather than thrown: the menu then says
 * there is no group instead of failing, and what is actually in the document
 * is a question for the console.
 */
export function idsOf(field) {
    return Array.isArray(field) ? field.filter((id) => typeof id === 'string') : [];
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

/** The devices a grant names: the positions cannot be listed, so this is the list to read. */
export function devicesOf(snapshot) {
    const data = snapshot.data();

    return Array.isArray(data.devices) ? data.devices.filter((one) => typeof one === 'string') : [];
}
