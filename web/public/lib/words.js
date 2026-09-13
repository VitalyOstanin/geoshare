// Everything the page says, in one place.
//
// The application keeps its wording in strings.xml, and the page kept its own
// scattered through the modules that happened to need it. One source is what
// makes the two halves possible to keep in step, and what makes translating
// the page later a matter of one file rather than a search across seven.
//
// The markup in index.html is not here: those words are read before any module
// runs, and moving them would mean a page that starts blank and fills in.
export const WORDS = {
    // The strip under the map, and what stands in for it when it is empty.
    you: 'You',
    signedIn: 'Signed in',
    nobodyYet: 'Nobody has shared a position yet.',
    looking: 'Looking for the group…',
    unreachable: 'No connection to the group. Trying again.',
    nobodySharing: 'Nobody is sharing a position with you right now.',
    oneMoreRefused: 'One more member is not sharing with you right now.',
    moreRefused: (count) => `${count} more members are not sharing with you right now.`,

    // The card a marker opens.
    lastSeen: 'Last seen',
    at: 'At',
    mode: 'Mode',
    battery: 'Battery',
    position: 'Position',
    unknown: 'unknown',
    backToList: '← Back to the list',
    oneMember: '1 member',
    members: (count) => `${count} members`,

    // Ages, which are the only words with a plural in them.
    justNow: 'just now',
    ago: (count, unit) => `${count} ${count === 1 ? unit.slice(0, -1) : unit} ago`,

    // What the sign-in screen says when it did not happen. The same four cases
    // the application tells apart.
    signInOffline: 'No connection. Sign in again once the network is back.',
    signInMisconfigured: 'This page is not set up for sign-in yet.',
    signInFailed: 'Sign-in did not go through. Try again.',
};
