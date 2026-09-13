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

    // The names of the five ways a phone collects its position. Names rather
    // than sentences describing them: what each one asks for and how often it
    // sends belongs in the help the card opens, where all five can be read
    // against each other. The document carries the entry of the enumeration,
    // which is the key of the lookup rather than anything shown, so a
    // translation names the modes in its own language.
    modeIdle: 'Idle',
    modeStandby: 'Standby',
    modeMoving: 'Moving',
    modeViewing: 'Viewing',
    modeNight: 'Night',

    // The help the mode on a card opens. All five are described in one place
    // rather than a sentence at a time on the cards: what a mode means is only
    // clear against the other four, and a member reads it once.
    modeHelpTitle: 'How a phone collects its position',
    modeHelpIntro:
        'The phone chooses one of these five by itself. Somebody with the map open outranks '
        + 'everything else; the rest of the time it spends as little as it can and still be found.',
    modeHelpWhen: 'When',
    modeHelpAsks: 'What the phone does',
    modeHelpClose: 'Close',
    modeWhenViewing: 'Somebody has the map open.',
    modeAsksViewing: 'Asks for the most exact position every 15 seconds, and writes it as soon as '
        + 'the marker would move.',
    modeWhenMoving: 'The phone noticed walking, running, cycling or riding.',
    modeAsksMoving: 'Asks every 5 minutes.',
    modeWhenStandby: 'Away from where it last wrote, with nobody looking.',
    modeAsksStandby: 'Asks every 15 minutes.',
    modeWhenNight: 'Between 23:00 and 07:00, or the charge is below 15 per cent.',
    modeAsksNight: 'Asks every 20 minutes, in the cheapest way it has.',
    modeWhenIdle: 'Settled within 150 metres of the position it last wrote.',
    modeAsksIdle: 'Asks for nothing at all, and repeats the last position every 25 minutes so the '
        + 'card does not go grey.',

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

    // Who else has the map open right now, named rather than counted, and said
    // once for the household rather than on a card: one open map puts every
    // phone of the group into the same way of collecting, so naming it per
    // member would repeat the same list under every face.
    mapOpen: (names) => `Map open: ${names.join(', ')}`,

    // Ages, which are the only words with a plural in them.
    justNow: 'just now',
    ago: (count, unit) => `${count} ${count === 1 ? unit.slice(0, -1) : unit} ago`,

    // What the sign-in screen says when it did not happen. The same four cases
    // the application tells apart.
    signInOffline: 'No connection. Sign in again once the network is back.',
    signInMisconfigured: 'This page is not set up for sign-in yet.',
    signInFailed: 'Sign-in did not go through. Try again.',
};
